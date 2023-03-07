package net.azisaba.playersync.codec

import xyz.acrylicstyle.util.serialization.codec.Codec
import xyz.acrylicstyle.util.serialization.decoder.ValueDecoder
import xyz.acrylicstyle.util.serialization.encoder.ValueEncoder

class MapCodec<K : Any, V : Any>(
    private val keyElementCodec: Codec<K>,
    private val valueElementCodec: Codec<V>,
) : Codec<Map<K, V>>() {
    override fun encode(map: Map<K, V>, encoder: ValueEncoder) {
        encoder.encodeInt(map.size)
        map.forEach { (k, v) ->
            keyElementCodec.encode(k, encoder)
            valueElementCodec.encode(v, encoder)
        }
    }

    override fun decode(decoder: ValueDecoder): Map<K, V> {
        val size = decoder.decodeInt()
        val map = mutableMapOf<K, V>()
        repeat(size) {
            val k = keyElementCodec.decode(decoder)
            val v = valueElementCodec.decode(decoder)
            map[k] = v
        }
        return map
    }
}
