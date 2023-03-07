package net.azisaba.playersync.codec

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.util.serialization.codec.Codec

object ExtraCodecs {
    val property: Codec<Property> =
        Codec.builder<Property>()
            .group(
                Codec.STRING.fieldOf("name").getter { it.name },
                Codec.STRING.fieldOf("value").getter { it.value },
                Codec.STRING.fieldOf("signature").getter { it.signature }
            )
            .build(::Property)

    val propertyMap: Codec<PropertyMap> =
        Codec.builder<PropertyMap>()
            .group(
                MapCodec(Codec.STRING, Codec.list(property))
                    .fieldOf("properties")
                    .getter { it.asMap().mapValues { (_, v) -> v.toList() } }
            ).build { properties -> PropertyMap().apply { properties.forEach { (k, v) -> this.putAll(k, v) } } }

    val gameProfile: Codec<GameProfile> =
        Codec.builder<GameProfile>()
            .group(
                Codec.UUID.fieldOf("id").getter { it.id },
                Codec.STRING.fieldOf("name").getter { it.name },
                propertyMap.fieldOf("properties").getter { it.properties }
            )
            .build { id, name, properties -> GameProfile(id, name).apply { properties.putAll(properties) } }

    val byteArray: Codec<ByteArray> =
        Codec.of({ value, encoder ->
            encoder.encodeInt(value.size)
            value.forEach { encoder.encodeByte(it) }
        }, { decoder ->
            val size = decoder.decodeInt()
            ByteArray(size) { decoder.decodeByte() }
        }, "ByteArray")

    val itemStack: Codec<ItemStack> =
        Codec.builder<ItemStack>()
            .group(byteArray.fieldOf("data").getter {
                if (it.type.isAir) {
                    byteArrayOf(0)
                } else {
                    it.serializeAsBytes()
                }
            })
            .build {
                if (it.size == 1 && it[0] == (0).toByte()) {
                    ItemStack(Material.AIR)
                } else {
                    ItemStack.deserializeBytes(it)
                }
            }
}
