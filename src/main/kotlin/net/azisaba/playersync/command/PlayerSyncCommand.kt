package net.azisaba.playersync.command

import net.azisaba.playersync.PlayerSyncPlugin
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Collections

object PlayerSyncCommand : TabExecutor {
    private val plugin by lazy { PlayerSyncPlugin.getInstance() }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return true
        }
        if (args[0] == "vanish") {
            val vanished = plugin.isVanished(sender)
            plugin.vanish(sender, !vanished)
            sender.sendMessage("${ChatColor.GOLD}Vanishを${if (!vanished) "${ChatColor.GREEN}オン" else "${ChatColor.RED}オフ"}${ChatColor.GOLD}にしました")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("vanish").filter { it.startsWith(args[0]) }
        }
        return Collections.emptyList()
    }
}
