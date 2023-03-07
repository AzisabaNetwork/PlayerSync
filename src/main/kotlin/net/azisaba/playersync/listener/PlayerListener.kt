package net.azisaba.playersync.listener

import net.azisaba.playersync.PlayerSyncPlugin
import net.azisaba.playersync.config.Config
import net.azisaba.playersync.data.PartialInventory
import net.azisaba.playersync.data.PlayerPos
import net.azisaba.playersync.entity.EntityPlayerSynced
import net.azisaba.playersync.network.Protocol
import net.azisaba.playersync.network.packet.PacketDestroyPlayer
import net.azisaba.playersync.network.packet.PacketPlayerSwingHand
import net.azisaba.playersync.network.packet.PacketSpawnPlayer
import net.minecraft.server.v1_15_R1.EnumHand
import org.bukkit.ChatColor
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object PlayerListener : Listener {
    private val plugin by lazy { PlayerSyncPlugin.getInstance() }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (e.player.hasPermission("playersync.auto-vanish")) {
            plugin.vanish(e.player, true)
            e.player.sendMessage("${ChatColor.GOLD}PlayerSyncのvanish状態は${ChatColor.GREEN}オン${ChatColor.GOLD}になっています。")
            e.player.sendMessage("${ChatColor.AQUA}/playersync vanish${ChatColor.GOLD}でオフにできます。")
        }
        sendSpawnAndRespawnNPCs(e.player)
    }

    @EventHandler
    fun onPlayerWorldChange(e: PlayerChangedWorldEvent) {
        sendSpawnAndRespawnNPCs(e.player)
    }

    private fun sendSpawnAndRespawnNPCs(player: Player) {
        plugin.async {
            val gameProfile = (player as CraftPlayer).handle.profile
            val packet =
                PacketSpawnPlayer(
                    player.world.name,
                    gameProfile,
                    PlayerPos(player.location),
                    PartialInventory.fromBukkitInventory(player.inventory),
                    ChatColor.translateAlternateColorCodes('&', Config.config.getFormattedTabListName(player)),
                    plugin.isVanished(player),
                )
            Protocol.SPAWN_PLAYER.send(plugin.jedisBox.pubSubHandler, packet)
            EntityPlayerSynced.players.values.forEach { it.spawnFor(player) }
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        plugin.async {
            val packet = PacketDestroyPlayer(e.player.uniqueId)
            Protocol.DESTROY_PLAYER.send(plugin.jedisBox.pubSubHandler, packet)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInteract(e: PlayerAnimationEvent) {
        plugin.async {
            if (e.animationType == PlayerAnimationType.ARM_SWING) {
                Protocol.PLAYER_SWING_HAND.send(
                    plugin.jedisBox.pubSubHandler,
                    PacketPlayerSwingHand(e.player.uniqueId, EnumHand.MAIN_HAND)
                )
            }
        }
    }
}
