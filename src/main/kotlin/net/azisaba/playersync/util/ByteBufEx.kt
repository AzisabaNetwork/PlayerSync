package net.azisaba.playersync.util

import io.netty.buffer.ByteBuf

fun ByteBuf.toByteArray(): ByteArray {
    val bytes = ByteArray(this.readableBytes())
    getBytes(readerIndex(), bytes)
    release()
    return bytes
}
