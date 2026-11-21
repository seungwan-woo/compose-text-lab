package io.github.seungwanwoo.composetextlab

import org.json.JSONArray
import org.json.JSONObject

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
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("engine", engine); put("widgetWidthPx", widgetWidthPx); put("widgetHeightPx", widgetHeightPx)
        put("layoutWidthPx", layoutWidthPx); put("layoutHeightPx", layoutHeightPx); put("lineCount", lineCount)
        put("firstBaselineFromWidgetTopPx", firstBaselineFromWidgetTopPx)
        put("lastBaselineFromWidgetTopPx", lastBaselineFromWidgetTopPx)
        put("ellipsized", ellipsized); put("overflow", overflow)
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
