package net.azisaba.playersync.network

import io.netty.buffer.ByteBuf
import net.azisaba.playersync.util.toByteArray
import xyz.acrylicstyle.util.serialization.codec.Codec
import xyz.acrylicstyle.util.serialization.decoder.ByteBufValueDecoder
import xyz.acrylicstyle.util.serialization.encoder.ByteBufValueEncoder
import java.nio.charset.StandardCharsets

interface Packet<T : PacketListener> {
    fun encode(buf: ByteBuf)
    fun handle(packetListener: T)

    companion object {
        fun <T : Any> ByteBuf.writeOptional(value: T?, action: ByteBuf.(T) -> Unit) {
            if (value == null) {
                writeBoolean(false)
            } else {
                writeBoolean(true)
                action(this, value)
            }
        }

        fun <T : Any> ByteBuf.readOptional(action: ByteBuf.() -> T): T? {
            if (!readBoolean()) return null
            return action(this)
        }

        fun <A : Any> ByteBuf.writeCodec(codec: Codec<A>, value: A) =
            codec.encode(value, ByteBufValueEncoder(this))

        fun <A : Any> ByteBuf.readCodec(codec: Codec<A>): A =
            codec.decode(ByteBufValueDecoder(this))

        /**
         * Writes a string to the buffer.
         * @param str the string
         */
        fun ByteBuf.writeString(str: String) {
            val bytes = str.toByteArray(StandardCharsets.UTF_8)
            writeInt(bytes.size)
            writeBytes(bytes)
        }

        /**
         * Reads a string from the buffer.
         * @throws IllegalArgumentException if the string is not valid (e.g. length is < 0)
         * @return the string
         */
        fun ByteBuf.readString(): String {
            val len = readInt()
            require(len >= 0) { "length < 0" }
            return String(readBytes(len).toByteArray(), StandardCharsets.UTF_8);
        }

        /**
         * Writes a byte array to the buffer.
         * @param buf the buffer
         * @param bytes the byte array
         */
        fun ByteBuf.writeByteArray(bytes: ByteArray) {
            writeInt(bytes.size)
            writeBytes(bytes)
        }

        /**
         * Reads a byte array from the buffer.
         * @param buf the buffer
         * @return the byte array
         */
        fun ByteBuf.readByteArray(): ByteArray {
            val len = readInt()
            require(len >= 0) { "length < 0" }
            val bytes = ByteArray(len)
            readBytes(bytes)
            return bytes
        }
    }
}