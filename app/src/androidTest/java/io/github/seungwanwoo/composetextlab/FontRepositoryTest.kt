package io.github.seungwanwoo.composetextlab

import android.graphics.Typeface
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.FontWeight

class FontRepositoryTest {
    @Test fun importsRestoresAndKeepsActiveFontWhenNewFontIsMalformed() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = FontRepository(context)
        val systemFont = File("/system/fonts").listFiles()!!.first {
            it.extension == "ttf" && runCatching { Typeface.Builder(it).build() }.isSuccess
        }
        val imported = repository.import(Uri.fromFile(systemFont))
        assertTrue(imported.file.isFile)
        assertEquals(imported.sha256, repository.restore()!!.sha256)
        val settings = LabSettings(font = FontChoice.Imported, weight = 700)
        val composeFace = createFontFamilyResolver(context).resolve(
            fontFamily = composeFamily(settings, imported), fontWeight = FontWeight(700)).value as Typeface
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            assertEquals(700, composeFace.weight)
            assertEquals(700, weightedTypeface(settings, imported).weight)
        }
        val malformed = File(context.cacheDir, "malformed.ttf")
        try {
            malformed.writeBytes(byteArrayOf(0, 1, 0, 0) + ByteArray(20))
            assertTrue(runCatching { repository.import(Uri.fromFile(malformed)) }.isFailure)
            assertEquals(imported.sha256, repository.restore()!!.sha256)
            assertEquals(1, File(context.filesDir, "fonts").listFiles()!!.count { it.extension == "font" })
        } finally { malformed.delete() }
    }
}
