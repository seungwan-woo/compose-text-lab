package io.github.seungwanwoo.composetextlab

import org.json.JSONObject

enum class MotionPath(private val label: String) {
    Layer("Layer translation"), Canvas("Canvas redraw");

    override fun toString() = label
}

data class EffectSettings(
    val shadowOffsetDp: Float = 2f,
    val shadowRadiusDp: Float = 3f,
    val strokeWidthDp: Float = 1f,
    val marqueeVelocity: Float = 30f,
    val marqueeGapDp: Float = 32f,
    val marqueeGapOverride: Boolean = false,
    val marqueeIterations: Int = 0,
    val marqueeDelayMs: Int = 1200,
    val animatedTextMotion: Boolean = false,
    val motionPath: MotionPath = MotionPath.Layer,
) {
    fun normalized() = copy(
        shadowOffsetDp = shadowOffsetDp.bounded(2f, 0f, 12f),
        shadowRadiusDp = shadowRadiusDp.bounded(3f, 0f, 16f),
        strokeWidthDp = strokeWidthDp.bounded(1f, 0.5f, 8f),
        marqueeVelocity = marqueeVelocity.bounded(30f, 10f, 140f),
        marqueeGapDp = marqueeGapDp.bounded(32f, 0f, 160f),
        marqueeIterations = marqueeIterations.coerceIn(0, 5),
        marqueeDelayMs = marqueeDelayMs.coerceIn(0, 2000),
    )

    fun toJson() = JSONObject().apply {
        put("shadowOffsetDp", shadowOffsetDp)
        put("shadowRadiusDp", shadowRadiusDp)
        put("strokeWidthDp", strokeWidthDp)
        put("marqueeVelocity", marqueeVelocity)
        put("marqueeGapDp", marqueeGapDp)
        put("marqueeGapOverride", marqueeGapOverride)
        put("marqueeIterations", marqueeIterations)
        put("marqueeDelayMs", marqueeDelayMs)
        put("animatedTextMotion", animatedTextMotion)
        put("motionPath", motionPath.name)
    }

    companion object {
        fun fromJson(json: JSONObject?): EffectSettings {
            if (json == null) return EffectSettings()
            val defaults = EffectSettings()
            return EffectSettings(
                shadowOffsetDp = json.optDouble("shadowOffsetDp", defaults.shadowOffsetDp.toDouble()).toFloat(),
                shadowRadiusDp = json.optDouble("shadowRadiusDp", defaults.shadowRadiusDp.toDouble()).toFloat(),
                strokeWidthDp = json.optDouble("strokeWidthDp", defaults.strokeWidthDp.toDouble()).toFloat(),
                marqueeVelocity = json.optDouble("marqueeVelocity", defaults.marqueeVelocity.toDouble()).toFloat(),
                marqueeGapDp = json.optDouble("marqueeGapDp", defaults.marqueeGapDp.toDouble()).toFloat(),
                marqueeGapOverride = json.optBoolean("marqueeGapOverride"),
                marqueeIterations = json.optInt("marqueeIterations"),
                marqueeDelayMs = json.optInt("marqueeDelayMs", defaults.marqueeDelayMs),
                animatedTextMotion = json.optBoolean("animatedTextMotion"),
                motionPath = MotionPath.entries.firstOrNull { it.name == json.optString("motionPath") }
                    ?: defaults.motionPath,
            ).normalized()
        }
    }
}

private fun Float.bounded(default: Float, min: Float, max: Float) =
    if (isFinite()) coerceIn(min, max) else default
