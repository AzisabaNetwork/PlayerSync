package net.azisaba.playersync.network.packet

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.data.PlayerPos
import net.azisaba.playersync.network.Packet
import net.azisaba.playersync.network.Packet.Companion.readCodec
import net.azisaba.playersync.network.Packet.Companion.writeCodec
import net.azisaba.playersync.network.PacketListener
import net.minecraft.server.v1_15_R1.EnumHand
import org.bukkit.GameMode
import xyz.acrylicstyle.util.serialization.codec.Codec
import java.util.UUID

class PacketPlayerTick(
    val uuid: UUID,
    val pos: PlayerPos,
    val gameMode: GameMode,
    val sneaking: Boolean,
    val glowing: Boolean,
    val swimming: Boolean,
    val gliding: Boolean,
    val playerSkinCustomization: Byte,
    val playerMainHand: Byte,
    val handRaised: Boolean,
    val raisedHand: EnumHand,
    val vanished: Boolean,
) : Packet<PacketListener> {
    constructor(buf: ByteBuf) : this(
        buf.readCodec(Codec.UUID),
        buf.readCodec(PlayerPos.CODEC),
        GameMode.values()[buf.readInt()],
        buf.readBoolean(),
        buf.readBoolean(),
        buf.readBoolean(),
        buf.readBoolean(),
        buf.readByte(),
        buf.readByte(),
        buf.readBoolean(),
        EnumHand.values()[buf.readInt()],
        buf.readBoolean(),
    )

    override fun encode(buf: ByteBuf) {
        buf.writeCodec(Codec.UUID, uuid)
        buf.writeCodec(PlayerPos.CODEC, pos)
        buf.writeInt(gameMode.ordinal)
        buf.writeBoolean(sneaking)
        buf.writeBoolean(glowing)
        buf.writeBoolean(swimming)
        buf.writeBoolean(gliding)
        buf.writeByte(playerSkinCustomization.toInt())
        buf.writeByte(playerMainHand.toInt())
        buf.writeBoolean(handRaised)
        buf.writeInt(raisedHand.ordinal)
        buf.writeBoolean(vanished)
    }

    override fun handle(packetListener: PacketListener) {
        packetListener.handlePlayerTick(this)
    }
}
