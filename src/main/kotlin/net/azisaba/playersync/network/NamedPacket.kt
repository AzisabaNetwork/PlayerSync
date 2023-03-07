package net.azisaba.playersync.network

import io.netty.buffer.ByteBuf
import org.jetbrains.annotations.Contract
import java.util.Objects
import java.util.function.Function

class NamedPacket<P : PacketListener, T : Packet<P>>(
    val name: String,
    val clazz: Class<T>,
    private val packetConstructor: Function<ByteBuf, T>,
) {

    fun create(buf: ByteBuf): T = packetConstructor.apply(buf)

    fun send(pubSubHandler: PubSubHandler, packet: T) {
        pubSubHandler.publish(name, packet)
    }
}