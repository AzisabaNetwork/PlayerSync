package net.azisaba.playersync.network.packet

import com.mojang.authlib.GameProfile
import io.netty.buffer.ByteBuf
import net.azisaba.playersync.codec.ExtraCodecs
import net.azisaba.playersync.data.PlayerPos
import net.azisaba.playersync.network.Packet
import net.azisaba.playersync.network.Packet.Companion.readCodec
import net.azisaba.playersync.network.Packet.Companion.readString
import net.azisaba.playersync.network.Packet.Companion.writeCodec
import net.azisaba.playersync.network.Packet.Companion.writeString
import net.azisaba.playersync.network.PacketListener

/**
 * プレイヤーの参加もしくはワールドを変更したときに送信されるパケット
 */
class PacketSpawnPlayer(
    val worldName: String,
    val gameProfile: GameProfile,
    val pos: PlayerPos,
    val tabListName: String,
    val vanished: Boolean,
) : Packet<PacketListener> {
    constructor(buf: ByteBuf) : this(
        buf.readString(),
        buf.readCodec(ExtraCodecs.gameProfile),
        buf.readCodec(PlayerPos.CODEC),
        buf.readString(),
        buf.readBoolean(),
    )

    override fun encode(buf: ByteBuf) {
        buf.writeString(worldName)
        buf.writeCodec(ExtraCodecs.gameProfile, gameProfile)
        buf.writeCodec(PlayerPos.CODEC, pos)
        buf.writeString(tabListName)
        buf.writeBoolean(vanished)
    }

    override fun handle(packetListener: PacketListener) {
        packetListener.handleSpawnPlayer(this)
    }
}
