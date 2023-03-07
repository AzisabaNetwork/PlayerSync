package net.azisaba.playersync.config

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import java.io.File

object Config {
    /**
     * PlayerSyncの設定ファイル
     */
    private val file = File("./plugins/PlayerSync/config.yml")

    /**
     * PlayerSyncの設定
     */
    val config = file.let {
        if (!it.exists()) {
            it.parentFile.mkdirs()
            it.writeText(Yaml.default.encodeToString(PlayerSyncConfig()))
        }
        Yaml.default.decodeFromString(PlayerSyncConfig.serializer(), it.readText())
    }

    /**
     * 設定を保存する
     */
    fun save() {
        try {
            file.writeText(Yaml.default.encodeToString(config))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        save()
    }
}
