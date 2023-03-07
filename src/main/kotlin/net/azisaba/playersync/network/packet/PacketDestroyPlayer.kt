package net.azisaba.playersync.network.packet

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.network.Packet
import net.azisaba.playersync.network.Packet.Companion.readCodec
import net.azisaba.playersync.network.Packet.Companion.writeCodec
import net.azisaba.playersync.network.PacketListener
import xyz.acrylicstyle.util.serialization.codec.Codec
import java.util.UUID

class PacketDestroyPlayer(val uuid: UUID) : Packet<PacketListener> {
    constructor(buf: ByteBuf) : this(buf.readCodec(Codec.UUID))

    override fun encode(buf: ByteBuf) {
        buf.writeCodec(Codec.UUID, uuid)
    }

    override fun handle(packetListener: PacketListener) {
        packetListener.handleDestroyPlayer(this)
    }
}