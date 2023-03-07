package net.azisaba.playersync.network

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.azisaba.playersync.config.Config
import net.azisaba.playersync.network.Packet.Companion.readString
import net.azisaba.playersync.network.Packet.Companion.writeString
import net.azisaba.playersync.network.Protocol.getByName
import net.azisaba.playersync.util.toByteArray
import org.jetbrains.annotations.Contract
import org.slf4j.Logger
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException
import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class PubSubHandler(
    private val logger: Logger,
    private val jedisPool: JedisPool,
    private val packetListener: PacketListener
) : Closeable {
    private val handlers: MutableMap<String, MutableList<(ByteBuf) -> Unit>> = ConcurrentHashMap()
    private val pingPongQueue = ArrayDeque<(ByteArray) -> Unit>()
    private val listener = PubSubListener()
    private val pingThread = Executors.newSingleThreadScheduledExecutor { r: Runnable? ->
        val t = Thread(r, "PlayerSync PubSub Ping Thread")
        t.isDaemon = true
        t
    }
    private val subscriberThread = Executors.newFixedThreadPool(1) { r: Runnable? ->
        val t = Thread(r, "PlayerSync PubSub Subscriber Thread")
        t.isDaemon = true
        t
    }
    private var isProcessing = true

    init {
        register()
    }

    private fun loop() {
        try {
            try {
                jedisPool.resource.use { jedis -> jedis.subscribe(listener, CHANNEL) }
            } catch (e: JedisConnectionException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            logger.warn("Failed to get Jedis resource", e)
        } finally {
            subscriberThread.submit {
                try {
                    Thread.sleep(3000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                loop() // recursion
            }
        }
    }

    private fun register() {
        jedisPool.resource.close() // check connection
        subscriberThread.submit { loop() }
        pingThread.scheduleAtFixedRate({
            try {
                val latency = ping()
                if (latency < 0) {
                    logger.warn("Got disconnected from Redis server, attempting to reconnect... (code: {})", latency)
                    listener.unsubscribe()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 5, 5, TimeUnit.SECONDS)
    }

    fun getHandlerList(key: String): List<(ByteBuf) -> Unit> =
        handlers.getOrDefault(key, emptyList())

    private fun getOrCreateHandlerList(key: String): MutableList<(ByteBuf) -> Unit> =
        handlers.computeIfAbsent(key) { ArrayList() }

    fun subscribe(key: String, handler: (ByteBuf) -> Unit) {
        require(getByName(key) == null) { "Cannot subscribe to a defined packet" }
        getOrCreateHandlerList(key).add(handler)
    }

    fun unsubscribe(key: String, handler: (ByteBuf) -> Unit) {
        getOrCreateHandlerList(key).remove(handler)
    }

    fun unsubscribeAll(key: String) {
        handlers.remove(key)
    }

    fun publish(key: String, data: ByteBuf) {
        val buf = Unpooled.buffer()
        buf.writeString(key)
        buf.writeBytes(data)
        jedisPool.resource.use { jedis ->
            jedis.publish(CHANNEL, buf.toByteArray())
        }
    }

    fun publish(key: String, packet: Packet<*>) {
        val buf = Unpooled.buffer()
        buf.writeString(key)
        packet.encode(buf)
        jedisPool.resource.use { jedis ->
            jedis.publish(CHANNEL, buf.toByteArray())
        }
    }

    private fun processRawMessage(message: ByteArray) {
        val buf = Unpooled.wrappedBuffer(message)
        val key = buf.readString()
        try {
            val namedPacket = getByName(key)
            if (namedPacket == null) {
                processUnknown(key, buf.slice())
            } else {
                handlePacket(namedPacket, buf.slice())
            }
        } catch (e: Exception) {
            logger.error("Error handling packet", e)
        } finally {
            if (buf.refCnt() > 0) {
                buf.release()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <P : PacketListener> handlePacket(namedPacket: NamedPacket<P, *>, buf: ByteBuf) {
        val packet: Packet<P> = namedPacket.create(buf) as Packet<P>
        packet.handle(packetListener as P)
    }

    private fun processUnknown(key: String, data: ByteBuf) {
        getOrCreateHandlerList(key).forEach { it(data) }
    }

    private fun ping(): Long {
        if (!listener.isSubscribed) {
            return -2
        }
        val thread = Thread {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        // TODO: maybe we should start too; i don't think it works without this
        //thread.start();
        val start = System.currentTimeMillis()
        pingPongQueue.add { thread.interrupt() }
        try {
            listener.ping()
        } catch (e: JedisConnectionException) {
            return -1
        }
        try {
            thread.join(3000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return System.currentTimeMillis() - start
    }

    override fun close() {
        isProcessing = false
        subscriberThread.shutdownNow()
        pingThread.shutdownNow()
    }

    private inner class PubSubListener : BinaryJedisPubSub() {
        override fun onMessage(channel: ByteArray, message: ByteArray) {
            if (isProcessing && CHANNEL.contentEquals(channel)) {
                processRawMessage(message)
            }
        }

        override fun onPong(pattern: ByteArray) {
            val action = pingPongQueue.poll()
            if (action != null) {
                try {
                    action(pattern)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        val CHANNEL = RedisKey().group(Config.config.group).path("pubsub").toString().toByteArray(StandardCharsets.UTF_8)
    }
}