package net.azisaba.playersync.data

import net.azisaba.playersync.codec.ExtraCodecs
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import xyz.acrylicstyle.util.serialization.codec.Codec

data class PartialInventory(
    val mainHand: ItemStack,
    val offHand: ItemStack,
    val helmet: ItemStack,
    val chestplate: ItemStack,
    val leggings: ItemStack,
    val boots: ItemStack,
) {
    companion object {
        val EMPTY =
            PartialInventory(
                ItemStack(Material.AIR),
                ItemStack(Material.AIR),
                ItemStack(Material.AIR),
                ItemStack(Material.AIR),
                ItemStack(Material.AIR),
                ItemStack(Material.AIR),
            )

        val CODEC =
            Codec.builder<PartialInventory>()
                .group(
                    ExtraCodecs.itemStack.fieldOf("mainHand").getter { it.mainHand },
                    ExtraCodecs.itemStack.fieldOf("offHand").getter { it.offHand },
                    ExtraCodecs.itemStack.fieldOf("helmet").getter { it.helmet },
                    ExtraCodecs.itemStack.fieldOf("chestplate").getter { it.chestplate },
                    ExtraCodecs.itemStack.fieldOf("leggings").getter { it.leggings },
                    ExtraCodecs.itemStack.fieldOf("boots").getter { it.boots },
                ).build(::PartialInventory)

        fun fromBukkitInventory(inventory: PlayerInventory): PartialInventory {
            return PartialInventory(
                inventory.itemInMainHand,
                inventory.itemInOffHand,
                inventory.helmet ?: ItemStack(Material.AIR),
                inventory.chestplate ?: ItemStack(Material.AIR),
                inventory.leggings ?: ItemStack(Material.AIR),
                inventory.boots ?: ItemStack(Material.AIR),
            )
        }
    }
}
