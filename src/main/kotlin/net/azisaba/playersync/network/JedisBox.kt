package net.azisaba.playersync.network

import net.azisaba.playersync.config.RedisConfig
import org.slf4j.Logger
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.io.Closeable

/**
 * JedisBoxとJedisPoolを作成
 */
class JedisBox(logger: Logger, config: RedisConfig) : Closeable {
    private val jedisPool: JedisPool = createPool(config.host, config.port, config.username, config.password)
    val pubSubHandler: PubSubHandler = PubSubHandler(logger, jedisPool, PacketListenerImpl)

    /**
     * Redisの接続を閉じる
     */
    override fun close() {
        pubSubHandler.close()
        jedisPool.close()
    }

    companion object {
        /**
         * 指定された情報からJedisPoolを作成する
         */
        fun createPool(hostname: String, port: Int, username: String?, password: String?): JedisPool {
            return if (username != null && password != null) {
                JedisPool(hostname, port, username, password)
            } else if (password != null) {
                JedisPool(JedisPoolConfig(), hostname, port, 3000, password)
            } else if (username != null) {
                error("password must not be null when username is provided")
            } else {
                JedisPool(JedisPoolConfig(), hostname, port)
            }
        }
    }
}
