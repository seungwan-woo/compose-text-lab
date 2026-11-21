package io.github.seungwanwoo.composetextlab

import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class FontBytesTest {
    @Test fun acceptsTtfAndOtfContainersForLaterPlatformValidation() {
        for (header in listOf(byteArrayOf(0, 1, 0, 0), "OTTO".toByteArray())) {
            val data = header + ByteArray(20)
            assertArrayEquals(data, readFontBytes(ByteArrayInputStream(data), 64))
        }
    }
    @Test fun rejectsTruncatedAndNonFontInputs() {
        for (data in listOf(byteArrayOf(0, 1), "not a font file".toByteArray())) {
            assertThrows(IllegalArgumentException::class.java) { readFontBytes(ByteArrayInputStream(data), 64) }
        }
    }
    @Test fun rejectsStreamsOverLimitEvenWhenProviderSizeIsUnknown() {
        val data = byteArrayOf(0, 1, 0, 0) + ByteArray(100)
        assertThrows(IllegalArgumentException::class.java) { readFontBytes(ByteArrayInputStream(data), 32) }
    }
}
