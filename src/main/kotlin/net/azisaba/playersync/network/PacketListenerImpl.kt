package net.azisaba.playersync.network

import net.azisaba.playersync.PlayerSyncPlugin
import net.azisaba.playersync.entity.EntityPlayerSynced
import net.azisaba.playersync.network.packet.PacketDestroyPlayer
import net.azisaba.playersync.network.packet.PacketPlayerSwingHand
import net.azisaba.playersync.network.packet.PacketPlayerTick
import net.azisaba.playersync.network.packet.PacketSpawnPlayer
import net.azisaba.playersync.network.packet.PacketUpdateInventory
import net.azisaba.playersync.util.PlayerUtil
import net.minecraft.server.v1_15_R1.EnumGamemode
import net.minecraft.server.v1_15_R1.EnumHand
import net.minecraft.server.v1_15_R1.PacketPlayOutAnimation
import net.minecraft.server.v1_15_R1.PlayerInteractManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld
import java.util.UUID

object PacketListenerImpl : PacketListener {
    private val plugin by lazy { PlayerSyncPlugin.getInstance() }

    private val wasVanished = mutableSetOf<UUID>()

    override fun handleSpawnPlayer(packet: PacketSpawnPlayer) {
        // このサーバーにplayerがいる場合は無視
        if (Bukkit.getPlayer(packet.gameProfile.id) != null) return
        // ワールドがない場合は無視
        val world = Bukkit.getWorld(packet.worldName) ?: run {
            // 別ワールドにいるプレイヤーを削除
            EntityPlayerSynced.players[packet.gameProfile.id]?.unregister()
            return
        }
        // エンティティをスポーン
        val worldServer = (world as CraftWorld).handle
        val player = EntityPlayerSynced(
            worldServer,
            packet.gameProfile,
            PlayerInteractManager(worldServer),
            packet.pos,
            packet.inventory,
            packet.tabListName,
        )
        player.vanished = packet.vanished
        player.spawn()
    }

    override fun handlePlayerTick(packet: PacketPlayerTick) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        // 座標を更新
        player.currentPos = packet.pos
        plugin.sync {
            player.changeGameMode(packet.gameMode.toEnumGamemode()) // ゲームモード変更
            player.isSneaking = packet.sneaking
            player.glowing = packet.glowing
            player.isSwimming = packet.swimming
            if (packet.gliding) {
                if (!player.isGliding) {
                    player.startGliding()
                }
            } else {
                if (player.isGliding) {
                    player.stopGliding()
                }
            }
            player.dataWatcher.set(EntityPlayerSynced.DATA_PLAYER_MODE_CUSTOMIZATION, packet.playerSkinCustomization)
            player.dataWatcher.set(EntityPlayerSynced.DATA_PLAYER_MAIN_HAND, packet.playerMainHand)
            player.updatePlayerPose()
            player.a(EnumHand.MAIN_HAND)
            player.setHandRaised(packet.handRaised, packet.raisedHand)
            player.vanished = packet.vanished
            if (packet.vanished) {
                if (!wasVanished.contains(packet.uuid)) {
                    // start vanish
                    wasVanished.add(packet.uuid)
                    val packetBuilder = player.getDespawnPacketBuilder()
                    Bukkit.getOnlinePlayers().forEach { player ->
                        if (player.hasPermission("playersync.see-vanished")) return@forEach
                        packetBuilder.sendTo(player)
                    }
                }
            } else {
                if (wasVanished.contains(packet.uuid)) {
                    // end vanish
                    wasVanished.remove(packet.uuid)
                    player.spawn()
                }
            }
        }
        player.lastActive = System.currentTimeMillis()
    }

    override fun handleDestroyPlayer(packet: PacketDestroyPlayer) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        player.unregister()
    }

    override fun handleUpdateInventory(packet: PacketUpdateInventory) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        // インベントリを更新
        player.partialInventory = packet.inventory
        player.getInventoryUpdatePacketBuilder().broadcast(player)
    }

    override fun handlePlayerSwingHand(packet: PacketPlayerSwingHand) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        PlayerUtil.packetBuilder {
            addPacket(PacketPlayOutAnimation(player, if (packet.hand == EnumHand.MAIN_HAND) 0 else 3))
        }.broadcast(player)
    }

    private fun GameMode.toEnumGamemode() =
        when (this) {
            GameMode.ADVENTURE -> EnumGamemode.ADVENTURE
            GameMode.CREATIVE -> EnumGamemode.CREATIVE
            GameMode.SPECTATOR -> EnumGamemode.SPECTATOR
            GameMode.SURVIVAL -> EnumGamemode.SURVIVAL
            else -> EnumGamemode.NOT_SET
        }
}
