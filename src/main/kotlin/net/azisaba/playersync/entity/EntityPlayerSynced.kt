package net.azisaba.playersync.entity

import com.mojang.authlib.GameProfile
import net.azisaba.playersync.data.PartialInventory
import net.azisaba.playersync.data.PlayerPos
import net.azisaba.playersync.util.PacketBuilder
import net.azisaba.playersync.util.PlayerUtil
import net.minecraft.server.v1_15_R1.ChatComponentText
import net.minecraft.server.v1_15_R1.EntityPlayer
import net.minecraft.server.v1_15_R1.EnumGamemode
import net.minecraft.server.v1_15_R1.EnumHand
import net.minecraft.server.v1_15_R1.EnumItemSlot
import net.minecraft.server.v1_15_R1.IChatBaseComponent
import net.minecraft.server.v1_15_R1.ItemStack
import net.minecraft.server.v1_15_R1.PacketPlayOutEntity
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityEquipment
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityHeadRotation
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityMetadata
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityTeleport
import net.minecraft.server.v1_15_R1.PacketPlayOutNamedEntitySpawn
import net.minecraft.server.v1_15_R1.PacketPlayOutPlayerInfo
import net.minecraft.server.v1_15_R1.PlayerInteractManager
import net.minecraft.server.v1_15_R1.WorldServer
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

class EntityPlayerSynced(
    worldServer: WorldServer,
    gameProfile: GameProfile,
    interactManager: PlayerInteractManager,
    initialPos: PlayerPos = PlayerPos.ZERO,
    initialInventory: PartialInventory = PartialInventory.EMPTY,
    var tabListName: String = "",
) : EntityPlayer(
    worldServer.minecraftServer,
    worldServer,
    gameProfile,
    interactManager,
) {
    companion object {
        val players = ConcurrentHashMap<UUID, EntityPlayerSynced>()

        val SHARED_FLAGS_ID = T!!
        val DATA_PLAYER_MODE_CUSTOMIZATION = bq!!
        val DATA_PLAYER_MAIN_HAND = br!!
    }

    init {
        players.remove(gameProfile.id)?.unregister()
        players[gameProfile.id] = this
    }

    private var lastPos: PlayerPos = PlayerPos.ZERO
    private var lastYRot: Int = 0
    private var lastXRot: Int = 0
    var lastActive = System.currentTimeMillis()
    var currentPos: PlayerPos = initialPos
    var partialInventory: PartialInventory = initialInventory
    var vanished = false

    fun tickAndRefresh() {
        // 30秒以上データの更新がない場合は削除する
        if (System.currentTimeMillis() - lastActive > 30000) {
            unregister()
            return
        }
        ticksLived++
        // プレイヤーの位置を更新
        val pos = currentPos
        setPositionRotation(pos.x, pos.y, pos.z, pos.yaw, pos.pitch)
        PlayerUtil.packetBuilder {
            val xa = encodeX()
            val ya = encodeY()
            val za = encodeZ()
            val yaw = floor(pos.yaw * 256.0f / 360.0f).toInt()
            val pitch = floor(pos.pitch * 256.0f / 360.0f).toInt()
            val isFarEnough = (lastPos.toVector() - pos.toVector()).lengthSqr() >= 7.6293945E-6f
            val shouldSendPos = isFarEnough || ticksLived % 60 == 0
            val shouldSendRot =
                abs(yaw - lastYRot) >= 1 || abs(pitch - lastXRot) >= 1
            val shouldTeleport = xa < -32768L || xa > 32767L || ya < -32768L || ya > 32767L || za < -32768L || za > 32767L
            if (!shouldTeleport) {
                if (!shouldSendPos || !shouldSendRot) {
                    if (shouldSendPos) {
                        addPacket(
                            PacketPlayOutEntity.PacketPlayOutRelEntityMove(
                                id,
                                xa.toInt().toShort(),
                                ya.toInt().toShort(),
                                za.toInt().toShort(),
                                onGround,
                            )
                        )
                    } else if (shouldSendRot) {
                        addPacket(
                            PacketPlayOutEntity.PacketPlayOutEntityLook(id, yaw.toByte(), pitch.toByte(), onGround)
                        )
                    }
                } else {
                    addPacket(
                        PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                            id,
                            xa.toInt().toShort(),
                            ya.toInt().toShort(),
                            za.toInt().toShort(),
                            yaw.toByte(),
                            pitch.toByte(),
                            onGround,
                        )
                    )
                }
            } else {
                addPacket(PacketPlayOutEntityTeleport(this@EntityPlayerSynced))
            }

            if (shouldSendPos) {
                lastPos = pos
            }
            if (shouldSendRot) {
                lastYRot = yaw
                lastXRot = pitch
                addPacket(PacketPlayOutEntityHeadRotation(this@EntityPlayerSynced, yaw.toByte()))
            }
        }.broadcast(this)

        // エンティティのデータを送信
        sendDirtyEntityData()
    }

    /**
     * プレイヤーのposeをDataWatcherの値と一致するように更新する
     */
    fun updatePlayerPose() = dX()

    /**
     * DataWatcherのdirtyがtrueの場合はDataWatcherを送信する
     */
    private fun sendDirtyEntityData() {
        val list = dataWatcher.b() // packDirty
        if (list != null) {
            PlayerUtil.packetBuilder {
                addPacket(PacketPlayOutEntityMetadata(id, dataWatcher, true))
            }.broadcast(this)
        }
    }

    private fun encode(n: Double): Long = round(n * 4096).toLong()

    private fun encodeX() = encode(currentPos.x) - encode(lastPos.x)

    private fun encodeY() = encode(currentPos.y) - encode(lastPos.y)

    private fun encodeZ() = encode(currentPos.z) - encode(lastPos.z)

    private fun getSpawnPacketBuilder() =
        PlayerUtil.packetBuilder {
            addPacket(PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, this@EntityPlayerSynced))
            addPacket(PacketPlayOutNamedEntitySpawn(this@EntityPlayerSynced))
            addPacket(PacketPlayOutEntityHeadRotation(this@EntityPlayerSynced, floor(currentPos.yaw * 256.0f / 360.0f).toInt().toByte()))
            addPacket(PacketPlayOutEntityTeleport(this@EntityPlayerSynced))
            addPacket(PacketPlayOutEntityMetadata(id, dataWatcher, true))
            if (tabListName.isBlank()) {
                addPacket(PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, this@EntityPlayerSynced))
            }
        }

    fun spawn() {
        val spawnPacket = getSpawnPacketBuilder()
        val inventoryUpdatePacket = getInventoryUpdatePacketBuilder()
        Bukkit.getOnlinePlayers().forEach { player ->
            if (vanished && !player.hasPermission("playersync.see-vanished")) return@forEach
            if (!player.canSee(bukkitEntity)) return@forEach
            if (worldServer != (player as CraftPlayer).handle.world) return@forEach
            spawnPacket.sendTo(player)
            inventoryUpdatePacket.sendTo(player)
        }
    }

    /**
     * 指定したプレイヤーに対してNPCを表示する
     * @param player スポーンパケットを送信するプレイヤー
     */
    fun spawnFor(player: Player) {
        if (vanished && !player.hasPermission("playersync.see-vanished")) return
        if (!player.canSee(bukkitEntity)) return
        if (worldServer != (player as CraftPlayer).handle.world) return
        getSpawnPacketBuilder().sendTo(player)
        getInventoryUpdatePacketBuilder().sendTo(player)
    }

    override fun getEquipment(enumitemslot: EnumItemSlot?): ItemStack =
        when (enumitemslot) {
            EnumItemSlot.MAINHAND -> CraftItemStack.asNMSCopy(partialInventory.mainHand)
            EnumItemSlot.OFFHAND -> CraftItemStack.asNMSCopy(partialInventory.offHand)
            EnumItemSlot.HEAD -> CraftItemStack.asNMSCopy(partialInventory.helmet)
            EnumItemSlot.CHEST -> CraftItemStack.asNMSCopy(partialInventory.chestplate)
            EnumItemSlot.LEGS -> CraftItemStack.asNMSCopy(partialInventory.leggings)
            EnumItemSlot.FEET -> CraftItemStack.asNMSCopy(partialInventory.boots)
            else -> error("Invalid slot: $enumitemslot")
        }

    /**
     * プレイヤーのインベントリを更新するパケットを送信する
     */
    fun updateInventory() {
        // パケットを送信
        getInventoryUpdatePacketBuilder().broadcast(this)
    }

    private fun getInventoryUpdatePacketBuilder(): PacketBuilder {
        val inventory = partialInventory
        return PlayerUtil.packetBuilder {
            addPacket(PacketPlayOutEntityEquipment(id, EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(inventory.mainHand)))
            addPacket(PacketPlayOutEntityEquipment(id, EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(inventory.offHand)))
            addPacket(PacketPlayOutEntityEquipment(id, EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(inventory.helmet)))
            addPacket(PacketPlayOutEntityEquipment(id, EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(inventory.chestplate)))
            addPacket(PacketPlayOutEntityEquipment(id, EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(inventory.leggings)))
            addPacket(PacketPlayOutEntityEquipment(id, EnumItemSlot.FEET, CraftItemStack.asNMSCopy(inventory.boots)))
        }
    }

    /**
     * プレイヤーを削除する
     */
    fun unregister() {
        players.remove(profile.id)
        despawn()
    }

    override fun getPlayerListName(): IChatBaseComponent {
        return ChatComponentText(tabListName.ifBlank { name })
    }

    /**
     * プレイヤーをデスポーンさせる
     */
    fun despawn() {
        getDespawnPacketBuilder().broadcast()
    }

    fun getDespawnPacketBuilder() =
        PlayerUtil.packetBuilder {
            addPacket(PacketPlayOutEntityDestroy(id))
            addPacket(PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, this@EntityPlayerSynced))
        }

    /**
     * 「使用中」のアニメーションを開始/終了する
     */
    fun setHandRaised(handRaised: Boolean, hand: EnumHand) {
        if (handRaised && !isHandRaised) {
            c(hand)
        }
        if (!handRaised && isHandRaised) {
            dH()
        }
    }

    /**
     * プレイヤーのゲームモードを変更する
     */
    @Suppress("DEPRECATION")
    fun changeGameMode(gameMode: EnumGamemode) {
        if (gameMode != playerInteractManager.gameMode) {
            playerInteractManager.gameMode = gameMode
            if (gameMode == EnumGamemode.SPECTATOR) {
                releaseShoulderEntities()
                stopRiding()
            } else {
                setSpectatorTarget(this)
            }
            updateAbilities()
            dz()
            C()
        }
    }

    fun updateTabName() {
        PlayerUtil.packetBuilder {
            addPacket(PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, this@EntityPlayerSynced))
        }.broadcast(this)
    }
}
