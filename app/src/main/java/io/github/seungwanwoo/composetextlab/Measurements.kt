package io.github.seungwanwoo.composetextlab

import org.json.JSONArray
import org.json.JSONObject

data class LineObservation(
    val startUtf16: Int,
    val endUtf16: Int,
    val paragraphDirection: String,
    val baselinePx: Float,
    val leftPx: Float,
    val rightPx: Float,
) {
    fun toJson() = JSONObject().apply {
        put("startUtf16", startUtf16); put("endUtf16", endUtf16)
        put("paragraphDirection", paragraphDirection)
        put("baselinePx", baselinePx); put("leftPx", leftPx); put("rightPx", rightPx)
    }
}

data class CaretObservation(
    val offsetUtf16: Int,
    val lineIndex: Int,
    val runDirection: String,
    val primaryXPx: Float,
    val secondaryXPx: Float,
) {
    fun toJson() = JSONObject().apply {
        put("offsetUtf16", offsetUtf16); put("lineIndex", lineIndex); put("runDirection", runDirection)
        put("primaryXPx", primaryXPx); put("secondaryXPx", secondaryXPx)
    }
}

data class ProbeMeasurement(
    val engine: String,
    val widgetWidthPx: Int,
    val widgetHeightPx: Int,
    val layoutWidthPx: Int,
    val layoutHeightPx: Int,
    val lineCount: Int,
    val firstBaselineFromWidgetTopPx: Float,
    val lastBaselineFromWidgetTopPx: Float,
    val ellipsized: Boolean,
    val overflow: Boolean,
    val lines: List<LineObservation> = emptyList(),
    val carets: List<CaretObservation> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("engine", engine); put("widgetWidthPx", widgetWidthPx); put("widgetHeightPx", widgetHeightPx)
        put("layoutWidthPx", layoutWidthPx); put("layoutHeightPx", layoutHeightPx); put("lineCount", lineCount)
        put("firstBaselineFromWidgetTopPx", firstBaselineFromWidgetTopPx)
        put("lastBaselineFromWidgetTopPx", lastBaselineFromWidgetTopPx)
        put("ellipsized", ellipsized); put("overflow", overflow)
        if (lines.isNotEmpty()) {
            put("lines", JSONArray(lines.map { it.toJson() }))
            put("carets", JSONArray(carets.map { it.toJson() }))
        }
    }
}

fun buildReport(
    settings: LabSettings,
    environment: Map<String, String>,
    fontIdentity: String,
    measurements: List<ProbeMeasurement>,
): JSONObject = JSONObject().apply {
    put("schemaVersion", 1); put("capturedAtEpochMs", System.currentTimeMillis())
    put("settings", settings.toJson()); put("environment", JSONObject(environment))
    put("fontIdentity", fontIdentity)
    put("measurements", JSONArray(measurements.map { it.toJson() }))
    put("pixelEquivalenceVerified", false)
    put("remoteToolbarRouting", "unverified: public SDK observation only")
    put("baselineOrigin", "widget top, px; external experiment padding included")
    put("viewLineHeightPolicy", "setLineSpacing(requestedSpPx - fontMetricsHeight, 1f)")
    put("measurementScope", "current rendered Text/TextView experiment; no selection/input metrics")
}
