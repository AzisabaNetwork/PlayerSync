package net.azisaba.playersync.network

import net.azisaba.playersync.network.packet.PacketDestroyPlayer
import net.azisaba.playersync.network.packet.PacketPlayerSwingHand
import net.azisaba.playersync.network.packet.PacketPlayerTick
import net.azisaba.playersync.network.packet.PacketSpawnPlayer
import net.azisaba.playersync.network.packet.PacketUpdateInventory

interface PacketListener {
    fun handleSpawnPlayer(packet: PacketSpawnPlayer)
    fun handlePlayerTick(packet: PacketPlayerTick)
    fun handleDestroyPlayer(packet: PacketDestroyPlayer)
    fun handleUpdateInventory(packet: PacketUpdateInventory)
    fun handlePlayerSwingHand(packet: PacketPlayerSwingHand)
}
