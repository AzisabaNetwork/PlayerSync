package net.azisaba.playersync.network.packet

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.network.Packet
import net.azisaba.playersync.network.Packet.Companion.readCodec
import net.azisaba.playersync.network.Packet.Companion.readString
import net.azisaba.playersync.network.Packet.Companion.writeCodec
import net.azisaba.playersync.network.Packet.Companion.writeString
import net.azisaba.playersync.network.PacketListener
import xyz.acrylicstyle.util.serialization.codec.Codec
import java.util.UUID

data class PacketPlayerUpdateTabName(val uuid: UUID, val tabListName: String) : Packet<PacketListener> {
    constructor(buf: ByteBuf) : this(buf.readCodec(Codec.UUID), buf.readString())

    override fun encode(buf: ByteBuf) {
        buf.writeCodec(Codec.UUID, uuid)
        buf.writeString(tabListName)
    }

    override fun handle(packetListener: PacketListener) {
        packetListener.handlePlayerUpdateTabName(this)
    }
}
