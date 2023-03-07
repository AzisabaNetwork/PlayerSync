@file:JvmName("PlayerSyncPlugin")
package net.azisaba.playersync

import net.azisaba.playersync.command.PlayerSyncCommand
import net.azisaba.playersync.config.Config
import net.azisaba.playersync.data.PartialInventory
import net.azisaba.playersync.data.PlayerPos
import net.azisaba.playersync.entity.EntityPlayerSynced
import net.azisaba.playersync.listener.PlayerListener
import net.azisaba.playersync.network.JedisBox
import net.azisaba.playersync.network.Protocol
import net.azisaba.playersync.network.packet.PacketPlayerTick
import net.azisaba.playersync.network.packet.PacketUpdateInventory
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class PlayerSyncPlugin : JavaPlugin(), AbstractPlayerSync {
    companion object {
        fun getInstance() = getPlugin(PlayerSyncPlugin::class.java)
    }

    lateinit var jedisBox: JedisBox

    private val trackingPartialInventories = mutableMapOf<UUID, PartialInventory>()
    private val vanishedPlayers = mutableSetOf<UUID>()

    override fun onEnable() {
        // 設定を読み込む
        Config
        jedisBox = JedisBox(slF4JLogger, Config.config.redis)

        // Listenerを登録
        Bukkit.getPluginManager().registerEvents(PlayerListener, this)

        // コマンドを登録
        Bukkit.getPluginCommand("playersync")?.setExecutor(PlayerSyncCommand)

        // 毎tickのタスク
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            // PacketPlayerTickを送信
            Bukkit.getOnlinePlayers().forEach { player ->
                val entityPlayer = (player as CraftPlayer).handle
                val packet =
                    PacketPlayerTick(
                        player.uniqueId,
                        PlayerPos(player.location),
                        player.gameMode,
                        player.isSneaking,
                        player.isGlowing || player.hasPotionEffect(PotionEffectType.GLOWING),
                        player.isSwimming,
                        player.isGliding,
                        entityPlayer.dataWatcher.get(EntityPlayerSynced.DATA_PLAYER_MODE_CUSTOMIZATION),
                        entityPlayer.dataWatcher.get(EntityPlayerSynced.DATA_PLAYER_MAIN_HAND),
                        entityPlayer.isHandRaised,
                        entityPlayer.raisedHand,
                        vanishedPlayers.contains(player.uniqueId),
                    )
                Protocol.PLAYER_TICK.send(jedisBox.pubSubHandler, packet)

                // インベントリ更新
                val currentInventory = PartialInventory.fromBukkitInventory(player.inventory)
                if (currentInventory != trackingPartialInventories[player.uniqueId]) {
                    trackingPartialInventories[player.uniqueId] = currentInventory
                    val inventoryPacket = PacketUpdateInventory(player.uniqueId, currentInventory)
                    Protocol.UPDATE_INVENTORY.send(jedisBox.pubSubHandler, inventoryPacket)
                }
            }
            // EntityPlayerSyncedのtickAndRefreshを実行
            EntityPlayerSynced.players.values.forEach { it.tickAndRefresh() }
        }, 1, 1)
    }

    override fun onDisable() {
        jedisBox.close()
        EntityPlayerSynced.players.values.forEach { it.unregister() }
    }

    override fun vanish(player: Player, vanish: Boolean) {
        if (vanish) {
            vanishedPlayers.add(player.uniqueId)
        } else {
            vanishedPlayers.remove(player.uniqueId)
        }
    }

    override fun isVanished(player: Player): Boolean = vanishedPlayers.contains(player.uniqueId)

    fun async(fn: () -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(this, fn)
    }

    fun sync(fn: () -> Unit) {
        Bukkit.getScheduler().runTask(this, fn)
    }
}
