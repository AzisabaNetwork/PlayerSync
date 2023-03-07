package net.azisaba.playersync

import org.bukkit.entity.Player

interface PlayerSync {
    fun vanish(player: Player, vanish: Boolean)

    fun isVanished(player: Player): Boolean
}
