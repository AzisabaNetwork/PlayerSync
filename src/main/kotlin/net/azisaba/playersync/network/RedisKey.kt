package net.azisaba.playersync.network

data class RedisKey(val group: String = "default", val path: String? = null) {
    fun group(group: String) = RedisKey(group, path)

    fun path(path: String) = RedisKey(group, path)

    override fun toString() = "playersync:$group:$path"
}
