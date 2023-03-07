package net.azisaba.playersync.network.packet

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.network.Packet
import net.azisaba.playersync.network.Packet.Companion.readCodec
import net.azisaba.playersync.network.Packet.Companion.writeCodec
import net.azisaba.playersync.network.PacketListener
import net.minecraft.server.v1_15_R1.EnumHand
import xyz.acrylicstyle.util.serialization.codec.Codec
import java.util.UUID

class PacketPlayerSwingHand(val uuid: UUID, val hand: EnumHand) : Packet<PacketListener> {
    constructor(buf: ByteBuf) : this(buf.readCodec(Codec.UUID), EnumHand.values()[buf.readInt()])

    override fun encode(buf: ByteBuf) {
        buf.writeCodec(Codec.UUID, uuid)
        buf.writeInt(hand.ordinal)
    }

    override fun handle(packetListener: PacketListener) {
        packetListener.handlePlayerSwingHand(this)
    }
}