package net.azisaba.playersync.data

import kotlinx.serialization.Serializable
import org.bukkit.Location
import xyz.acrylicstyle.util.serialization.codec.Codec
import kotlin.math.sqrt

@Serializable
data class PlayerPos(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {
    constructor(location: Location) : this(location.x, location.y, location.z, location.yaw, location.pitch)

    companion object {
        val ZERO = PlayerPos(0.0, 0.0, 0.0, 0.0f, 0.0f)

        val CODEC =
            Codec.builder<PlayerPos>()
                .group(
                    Codec.DOUBLE.fieldOf("x").getter { it.x },
                    Codec.DOUBLE.fieldOf("y").getter { it.y },
                    Codec.DOUBLE.fieldOf("z").getter { it.z },
                    Codec.FLOAT.fieldOf("yaw").getter { it.yaw },
                    Codec.FLOAT.fieldOf("pitch").getter { it.pitch },
                ).build(::PlayerPos)
    }

    fun distance(pos: PlayerPos): Double {
        val dx = x - pos.x
        val dy = y - pos.y
        val dz = z - pos.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun toVector() = SimpleVector(x, y, z)
}
