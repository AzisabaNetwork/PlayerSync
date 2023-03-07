package net.azisaba.playersync.config

import kotlinx.serialization.Serializable

@Serializable
data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val username: String? = null,
    val password: String? = null,
)
