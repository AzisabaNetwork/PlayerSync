package net.azisaba.playersync.network

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.network.packet.PacketDestroyPlayer
import net.azisaba.playersync.network.packet.PacketPlayerSwingHand
import net.azisaba.playersync.network.packet.PacketPlayerTick
import net.azisaba.playersync.network.packet.PacketSpawnPlayer
import net.azisaba.playersync.network.packet.PacketUpdateInventory
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * Packet structure is as follows:
 *
 *  * Packet ID (int + char sequence)
 *  * (Additional data, if any)
 *
 */
object Protocol {
    private val PACKET_MAP: MutableMap<String, NamedPacket<*, *>> = ConcurrentHashMap()

    val SPAWN_PLAYER = register("spawn_player", PacketSpawnPlayer::class.java, ::PacketSpawnPlayer)
    val PLAYER_TICK = register("player_tick", PacketPlayerTick::class.java, ::PacketPlayerTick)
    val DESTROY_PLAYER = register("destroy_player", PacketDestroyPlayer::class.java, ::PacketDestroyPlayer)
    val UPDATE_INVENTORY = register("update_inventory", PacketUpdateInventory::class.java, ::PacketUpdateInventory)
    val PLAYER_SWING_HAND = register("player_swing_hand", PacketPlayerSwingHand::class.java, ::PacketPlayerSwingHand)

    private fun <P : PacketListener, T : Packet<P>> register(
        name: String,
        clazz: Class<T>,
        packetConstructor: Function<ByteBuf, T>
    ): NamedPacket<P, T> {
        require(!PACKET_MAP.containsKey(name)) { "Duplicate packet name: $name" }
        val packet = NamedPacket(name, clazz, packetConstructor)
        PACKET_MAP[packet.name] = packet
        return packet
    }

    fun getByName(name: String): NamedPacket<*, *>? = PACKET_MAP[name]
}