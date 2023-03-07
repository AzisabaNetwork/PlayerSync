package net.azisaba.playersync.data

import org.bukkit.util.Vector
import xyz.acrylicstyle.util.serialization.codec.Codec

data class SimpleVector(val x: Double, val y: Double, val z: Double) {
    companion object {
        val ZERO = SimpleVector(0.0, 0.0, 0.0)

        val CODEC = Codec.builder<SimpleVector>()
            .group(
                Codec.DOUBLE.fieldOf("x").getter { it.x },
                Codec.DOUBLE.fieldOf("y").getter { it.y },
                Codec.DOUBLE.fieldOf("z").getter { it.z },
            ).build(::SimpleVector)

        fun fromBukkitVector(vector: Vector): SimpleVector {
            return SimpleVector(vector.x, vector.y, vector.z)
        }
    }

    fun lengthSqr(): Double = x * x + y * y + z * z

    operator fun minus(other: SimpleVector): SimpleVector {
        return SimpleVector(x - other.x, y - other.y, z - other.z)
    }
}
