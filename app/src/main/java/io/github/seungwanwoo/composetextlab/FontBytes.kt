package io.github.seungwanwoo.composetextlab

import java.io.ByteArrayOutputStream
import java.io.InputStream

const val MAX_FONT_BYTES = 20 * 1024 * 1024

/** Container check only. Android's Typeface parser performs final font validation. */
fun readFontBytes(input: InputStream, limit: Int = MAX_FONT_BYTES): ByteArray {
    require(limit >= 12)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) continue
        require(output.size().toLong() + count <= limit) { "Font exceeds ${limit / 1024} KiB limit" }
        output.write(buffer, 0, count)
    }
    val bytes = output.toByteArray()
    require(bytes.size >= 12) { "Truncated font file" }
    val signature = bytes.take(4)
    require(signature == listOf<Byte>(0, 1, 0, 0) ||
        signature == "OTTO".toByteArray().toList() || signature == "true".toByteArray().toList()) {
        "Choose a TTF or OTF font (font collections are not supported)"
    }
    return bytes
}
