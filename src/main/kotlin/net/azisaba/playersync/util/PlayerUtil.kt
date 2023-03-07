package net.azisaba.playersync.util

import net.azisaba.playersync.entity.EntityPlayerSynced
import net.minecraft.server.v1_15_R1.Packet
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer
import org.bukkit.entity.Player

object PlayerUtil {
    /**
     * パケットビルダーを作成
     */
    fun packetBuilder(builder: PacketBuilder.() -> Unit) =
        PacketBuilder().apply(builder)
}

class PacketBuilder {
    private val list = mutableListOf<Packet<*>>()

    /**
     * 送信するパケットを追加
     */
    fun addPacket(packet: Packet<*>) {
        list.add(packet)
    }

    /**
     * サーバーにいる全プレイヤーにパケットを送信する。[originPlayer]が指定されている場合、[originPlayer]がvanishしている場合かつ他の
     * プレイヤーが`playersync.see-vanished`権限を所持していない場合は送信しない。[Player.canSee]がfalseの場合も送信しない。
     * @param originPlayer 送信元とするプレイヤー
     */
    fun broadcast(originPlayer: EntityPlayerSynced? = null) {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (originPlayer != null) {
                if (originPlayer.vanished && !player.hasPermission("playersync.see-vanished")) return@forEach
                if (!player.canSee(originPlayer.bukkitEntity)) return@forEach
            }
            val pc = (player as CraftPlayer).handle.playerConnection
            list.forEach { pc.sendPacket(it) }
        }
    }

    fun sendTo(player: Player) {
        val pc = (player as CraftPlayer).handle.playerConnection
        list.forEach { pc.sendPacket(it) }
    }
}
