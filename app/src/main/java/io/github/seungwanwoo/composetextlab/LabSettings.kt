package io.github.seungwanwoo.composetextlab

import org.json.JSONObject

enum class ThemeChoice { System, Light, Dark, Dynamic }
enum class FontChoice { System, Sans, Serif, Monospace, Imported }
enum class TextEffect { None, Shadow, Gradient, Stroke, Marquee }

data class LabSettings(
    val text: String = "Compose ↔ TextView\n한글 기준선 Ag 0123 🙂\nالعربية 日本語 — text layout comparison",
    val theme: ThemeChoice = ThemeChoice.System,
    val font: FontChoice = FontChoice.System,
    val fontSizeSp: Float = 24f,
    val weight: Int = 400,
    val letterSpacingEm: Float = 0f,
    val lineHeightSp: Float = 0f,
    val includeFontPadding: Boolean = false,
    val maxLines: Int = 5,
    val ellipsis: Boolean = true,
    val paddingDp: Float = 8f,
    val previewWidthDp: Float = 320f,
    val sideBySide: Boolean = false,
    val showBaselines: Boolean = true,
    val useSystemFontScale: Boolean = true,
    val fontScale: Float = 1f,
    val effect: TextEffect = TextEffect.None,
    val customViewMenu: Boolean = false,
) {
    fun renderText(): String = if (effect == TextEffect.Marquee) text.replace(Regex("\r\n|\r|\n"), " ") else text

    fun normalized(): LabSettings = copy(
        fontSizeSp = fontSizeSp.finiteOr(24f).coerceIn(8f, 72f),
        fontScale = fontScale.finiteOr(1f).coerceIn(0.8f, 2f),
        weight = weight.coerceIn(100, 900),
        letterSpacingEm = letterSpacingEm.finiteOr(0f).coerceIn(-0.05f, 0.3f),
        lineHeightSp = lineHeightSp.finiteOr(0f).coerceIn(0f, 100f),
        maxLines = maxLines.coerceIn(1, 20),
        paddingDp = paddingDp.finiteOr(8f).coerceIn(0f, 32f),
        previewWidthDp = previewWidthDp.finiteOr(320f).coerceIn(120f, 600f),
    )
    fun effectiveFontScale(systemScale: Float): Float =
        if (useSystemFontScale) systemScale else normalized().fontScale

    fun toJson(): JSONObject = JSONObject().apply {
        put("text", text); put("theme", theme.name); put("font", font.name)
        put("fontSizeSp", fontSizeSp); put("weight", weight); put("letterSpacingEm", letterSpacingEm)
        put("lineHeightSp", lineHeightSp); put("includeFontPadding", includeFontPadding)
        put("maxLines", maxLines); put("ellipsis", ellipsis); put("paddingDp", paddingDp)
        put("previewWidthDp", previewWidthDp); put("sideBySide", sideBySide)
        put("showBaselines", showBaselines); put("useSystemFontScale", useSystemFontScale)
        put("fontScale", fontScale); put("effect", effect.name); put("customViewMenu", customViewMenu)
    }

    companion object {
        fun fromJson(value: String): LabSettings = runCatching {
            val j = JSONObject(value)
            val d = LabSettings()
            d.copy(
                text = j.optString("text", d.text),
                theme = ThemeChoice.valueOf(j.optString("theme", d.theme.name)),
                font = FontChoice.valueOf(j.optString("font", d.font.name)),
                fontSizeSp = j.optDouble("fontSizeSp", 24.0).toFloat(),
                weight = j.optInt("weight", 400),
                letterSpacingEm = j.optDouble("letterSpacingEm", 0.0).toFloat(),
                lineHeightSp = j.optDouble("lineHeightSp", 0.0).toFloat(),
                includeFontPadding = j.optBoolean("includeFontPadding"),
                maxLines = j.optInt("maxLines", 5), ellipsis = j.optBoolean("ellipsis", true),
                paddingDp = j.optDouble("paddingDp", 8.0).toFloat(),
                previewWidthDp = j.optDouble("previewWidthDp", 320.0).toFloat(),
                sideBySide = j.optBoolean("sideBySide"), showBaselines = j.optBoolean("showBaselines", true),
                useSystemFontScale = j.optBoolean("useSystemFontScale", true),
                fontScale = j.optDouble("fontScale", 1.0).toFloat(),
                effect = TextEffect.valueOf(j.optString("effect", d.effect.name)),
                customViewMenu = j.optBoolean("customViewMenu"),
            ).normalized()
        }.getOrDefault(LabSettings())
    }
}

private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback
