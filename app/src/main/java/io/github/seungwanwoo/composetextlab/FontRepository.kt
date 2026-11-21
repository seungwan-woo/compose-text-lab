package io.github.seungwanwoo.composetextlab

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FontAsset(val file: File, val displayName: String, val sha256: String, val typeface: Typeface)

class FontRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("font", Context.MODE_PRIVATE)
    private val directory get() = File(context.filesDir, "fonts").apply { mkdirs() }

    suspend fun restore(): FontAsset? = withContext(Dispatchers.IO) {
        runCatching {
            val hash = prefs.getString("hash", null) ?: return@withContext null
            require(hash.matches(Regex("[0-9a-f]{64}")))
            val file = File(directory, "$hash.font")
            FontAsset(file, prefs.getString("name", "Imported font")!!, hash, Typeface.Builder(file).build())
        }.getOrNull()
    }

    suspend fun import(uri: Uri): FontAsset = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { readFontBytes(it) }
            ?: error("Unable to read document")
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: "Imported font"
        val staging = File.createTempFile("font-", ".tmp", directory)
        var promoted: File? = null
        var saved = false
        val previousHash = prefs.getString("hash", null)
        val previousName = prefs.getString("name", null)
        try {
            staging.writeBytes(bytes)
            // Reject malformed SFNT tables before changing the saved active font.
            val typeface = checkNotNull(Typeface.Builder(staging).build()) { "Android rejected this font file" }
            val destination = File(directory, "$hash.font")
            if (!destination.exists()) {
                check(staging.renameTo(destination)) { "Unable to store font" }
                promoted = destination
            }
            val asset = FontAsset(destination, name, hash, typeface)
            saved = prefs.edit().putString("hash", hash).putString("name", name).commit()
            if (!saved) {
                prefs.edit().putString("hash", previousHash).putString("name", previousName).commit()
                error("Unable to save font")
            }
            // Cleanup failure must not turn a successful import into an apparent failure.
            runCatching { directory.listFiles()?.filter { it != destination && it.extension == "font" }?.forEach { it.delete() } }
            asset
        } finally {
            staging.delete()
            if (!saved) promoted?.delete()
        }
    }
}
