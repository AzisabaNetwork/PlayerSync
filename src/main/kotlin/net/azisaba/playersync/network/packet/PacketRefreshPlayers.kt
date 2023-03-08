package net.azisaba.playersync.network.packet

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.network.Packet
import net.azisaba.playersync.network.PacketListener

class PacketRefreshPlayers() : Packet<PacketListener> {
    @Suppress("UNUSED_PARAMETER")
    constructor(buf: ByteBuf) : this()

    override fun encode(buf: ByteBuf) {}

    override fun handle(packetListener: PacketListener) {
        packetListener.handleRefreshPlayers(this)
    }
}
