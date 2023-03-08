package net.azisaba.playersync.network

import net.azisaba.playersync.PlayerSyncPlugin
import net.azisaba.playersync.entity.EntityPlayerSynced
import net.azisaba.playersync.listener.PlayerListener
import net.azisaba.playersync.network.packet.PacketDestroyPlayer
import net.azisaba.playersync.network.packet.PacketPlayerSwingHand
import net.azisaba.playersync.network.packet.PacketPlayerTick
import net.azisaba.playersync.network.packet.PacketPlayerUpdateTabName
import net.azisaba.playersync.network.packet.PacketRefreshPlayers
import net.azisaba.playersync.network.packet.PacketSpawnPlayer
import net.azisaba.playersync.network.packet.PacketUpdateInventory
import net.azisaba.playersync.util.PlayerUtil
import net.minecraft.server.v1_15_R1.EnumGamemode
import net.minecraft.server.v1_15_R1.EnumHand
import net.minecraft.server.v1_15_R1.MobEffects
import net.minecraft.server.v1_15_R1.PacketPlayOutAnimation
import net.minecraft.server.v1_15_R1.PlayerInteractManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

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
            tabListName = packet.tabListName,
        )
        player.vanished = packet.vanished
        player.spawn()
    }

    override fun handlePlayerTick(packet: PacketPlayerTick) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        // 座標を更新
        player.currentPos = packet.pos
        plugin.sync {
            // mode
            player.changeGameMode(packet.gameMode.toEnumGamemode()) // ゲームモード変更
            // sneaking
            player.isSneaking = packet.sneaking
            // glowing
            player.glowing = packet.glowing
            val flags = player.dataWatcher.get(EntityPlayerSynced.SHARED_FLAGS_ID)
            if (packet.glowing) {
                player.dataWatcher.set(EntityPlayerSynced.SHARED_FLAGS_ID, flags or (1 shl 6).toByte())
            } else {
                player.dataWatcher.set(EntityPlayerSynced.SHARED_FLAGS_ID, flags and (1 shl 6).toByte().inv())
            }
            // swimming
            player.isSwimming = packet.swimming
            // gliding
            if (packet.gliding) {
                if (!player.isGliding) {
                    player.startGliding()
                }
            } else {
                if (player.isGliding) {
                    player.stopGliding()
                }
            }
            // skin customizations
            player.dataWatcher.set(EntityPlayerSynced.DATA_PLAYER_MODE_CUSTOMIZATION, packet.playerSkinCustomization)
            player.dataWatcher.set(EntityPlayerSynced.DATA_PLAYER_MAIN_HAND, packet.playerMainHand)
            // update pose
            player.updatePlayerPose()
            // set "using item" animation if needed
            player.setHandRaised(packet.handRaised, packet.raisedHand)
            // vanish
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
        player.updateInventory()
    }

    override fun handlePlayerSwingHand(packet: PacketPlayerSwingHand) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        PlayerUtil.packetBuilder {
            addPacket(PacketPlayOutAnimation(player, if (packet.hand == EnumHand.MAIN_HAND) 0 else 3))
        }.broadcast(player)
    }

    override fun handlePlayerUpdateTabName(packet: PacketPlayerUpdateTabName) {
        val player = EntityPlayerSynced.players[packet.uuid] ?: return
        player.tabListName = packet.tabListName
        player.updateTabName()
    }

    override fun handleRefreshPlayers(packet: PacketRefreshPlayers) {
        Bukkit.getOnlinePlayers().forEach { PlayerListener.sendSpawnAndRespawnNPCs(it) }
    }

    private fun GameMode.toEnumGamemode() =
        when (this) {
            GameMode.ADVENTURE -> EnumGamemode.ADVENTURE
            GameMode.CREATIVE -> EnumGamemode.CREATIVE
            GameMode.SPECTATOR -> EnumGamemode.SPECTATOR
            GameMode.SURVIVAL -> EnumGamemode.SURVIVAL
            else -> EnumGamemode.NOT_SET
        }

    private fun PotionEffectType.toMobEffectList() =
        when (this) {
            PotionEffectType.SPEED -> MobEffects.FASTER_MOVEMENT
            PotionEffectType.SLOW -> MobEffects.SLOWER_MOVEMENT
            PotionEffectType.FAST_DIGGING -> MobEffects.FASTER_DIG
            PotionEffectType.SLOW_DIGGING -> MobEffects.SLOWER_DIG
            PotionEffectType.INCREASE_DAMAGE -> MobEffects.INCREASE_DAMAGE
            PotionEffectType.HEAL -> MobEffects.HEAL
            PotionEffectType.HARM -> MobEffects.HARM
            PotionEffectType.JUMP -> MobEffects.JUMP
            PotionEffectType.CONFUSION -> MobEffects.CONFUSION
            PotionEffectType.REGENERATION -> MobEffects.REGENERATION
            PotionEffectType.DAMAGE_RESISTANCE -> MobEffects.RESISTANCE
            PotionEffectType.FIRE_RESISTANCE -> MobEffects.FIRE_RESISTANCE
            PotionEffectType.WATER_BREATHING -> MobEffects.WATER_BREATHING
            PotionEffectType.INVISIBILITY -> MobEffects.INVISIBILITY
            PotionEffectType.BLINDNESS -> MobEffects.BLINDNESS
            PotionEffectType.NIGHT_VISION -> MobEffects.NIGHT_VISION
            PotionEffectType.HUNGER -> MobEffects.HUNGER
            PotionEffectType.WEAKNESS -> MobEffects.WEAKNESS
            PotionEffectType.POISON -> MobEffects.POISON
            PotionEffectType.WITHER -> MobEffects.WITHER
            PotionEffectType.HEALTH_BOOST -> MobEffects.HEALTH_BOOST
            PotionEffectType.ABSORPTION -> MobEffects.ABSORBTION
            PotionEffectType.SATURATION -> MobEffects.SATURATION
            PotionEffectType.GLOWING -> MobEffects.GLOWING
            PotionEffectType.LEVITATION -> MobEffects.LEVITATION
            PotionEffectType.LUCK -> MobEffects.LUCK
            PotionEffectType.UNLUCK -> MobEffects.UNLUCK
            PotionEffectType.SLOW_FALLING -> MobEffects.SLOW_FALLING
            PotionEffectType.CONDUIT_POWER -> MobEffects.CONDUIT_POWER
            PotionEffectType.DOLPHINS_GRACE -> MobEffects.DOLPHINS_GRACE
            PotionEffectType.BAD_OMEN -> MobEffects.BAD_OMEN
            PotionEffectType.HERO_OF_THE_VILLAGE -> MobEffects.HERO_OF_THE_VILLAGE
            else -> error("Unknown potion effect type: $name")
        }
}
