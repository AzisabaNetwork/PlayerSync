package net.azisaba.playersync.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable
import net.milkbowl.vault.chat.Chat
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

@Serializable
data class PlayerSyncConfig(
    @YamlComment(
        "プレイヤー情報の同期に使用されるRedisのキーを指定します。",
    )
    val group: String = "default",
    @YamlComment(
        "Tabに表示されるプレイヤー名を設定します。",
        "ここに設定した文字列は*他のサーバーに対して*表示されます。",
        "空白に設定すると表示されなくなります。",
        "以下の構文が使えます。",
        "  %player% => プレイヤー名",
        "  %prefix% => Prefix",
        "  %suffix% => Suffix",
        "  %display_name% => Display Name",
    )
    val tabListName: String = "%player% (Synced)",
    val redis: RedisConfig = RedisConfig(),
) {
    fun getFormattedTabListName(player: Player): String {
        val chat = Bukkit.getServicesManager().getRegistration(Chat::class.java)?.provider
        return tabListName.replace("%player%", player.name)
            .replace("%prefix%", chat?.getPlayerPrefix(player).toString())
            .replace("%suffix%", chat?.getPlayerSuffix(player).toString())
            .replace("%display_name%", player.displayName)
            .let { ChatColor.translateAlternateColorCodes('&', it) }
    }
}
