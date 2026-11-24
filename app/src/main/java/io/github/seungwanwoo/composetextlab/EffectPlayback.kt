package io.github.seungwanwoo.composetextlab

import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.roundToInt
import org.json.JSONObject

internal class EffectPlayback(
    val effect: TextEffect,
    val options: EffectSettings,
    val travelPx: Float,
) {
    val compose = EffectWork()
    val textView = EffectWork()
    var view: MeasuredTextView? = null
    var offsetPx by mutableFloatStateOf(0f)
        private set
    var fontStep by mutableIntStateOf(0)
        private set

    private fun applyProgress(progress: Float) {
        if (effect == TextEffect.VariableFont) {
            val step = (progress * FONT_STEPS).roundToInt()
            if (fontStep != step) {
                fontStep = step
                view?.setVariableFont(VariableAxes.at(step))
            }
        } else {
            offsetPx = progress * travelPx
            view?.setMotionOffset(offsetPx, options.motionPath)
        }
    }

    suspend fun sample(window: Window?): JSONObject {
        applyProgress(0f)
        // Let the reset and the button state settle before opening the observation interval.
        repeat(3) { withFrameNanos { } }
        val composeBefore = compose.copy()
        val viewBefore = textView.copy()
        val frames = WindowFrames()
        window?.addOnFrameMetricsAvailableListener(frames, Handler(Looper.getMainLooper()))
        try {
            val start = withFrameNanos { it }
            var progress: Float
            do {
                progress = withFrameNanos { now ->
                    ((now - start).toDouble() / PLAYBACK_NANOS).toFloat().coerceIn(0f, 1f)
                }
                applyProgress(progress)
            } while (progress < 1f)
            repeat(2) { withFrameNanos { } }
        } finally {
            window?.removeOnFrameMetricsAvailableListener(frames)
        }
        return JSONObject().apply {
            put("path", if (effect == TextEffect.VariableFont) effect.name else options.motionPath.name)
            put("animatedTextMotion", options.animatedTextMotion && effect == TextEffect.Motion)
            put("durationMs", PLAYBACK_NANOS / 1_000_000)
            put("compose", compose.since(composeBefore))
            put("textView", textView.since(viewBefore))
            put("windowFrames", frames.toJson(view?.display?.refreshRate))
            if (effect == TextEffect.VariableFont) {
                put("font", "Roboto Variable")
                put("fromAxes", VariableAxes.at(0).asViewSettings())
                put("toAxes", VariableAxes.at(fontStep).asViewSettings())
            }
        }
    }
}

internal data class EffectWork(
    var composition: Int = 0,
    var measure: Int = 0,
    var textLayouts: Int = 0,
    var draw: Int = 0,
    var firstLineWidth: Float? = null,
) {
    fun since(start: EffectWork) = JSONObject().apply {
        put("composition", composition - start.composition)
        put("measure", measure - start.measure)
        put("textLayouts", textLayouts - start.textLayouts)
        put("draw", draw - start.draw)
        put("firstLineWidthBeforePx", start.firstLineWidth ?: JSONObject.NULL)
        put("firstLineWidthAfterPx", firstLineWidth ?: JSONObject.NULL)
    }
}

private const val FONT_STEPS = 12
private const val PLAYBACK_NANOS = 1_000_000_000L

internal data class VariableAxes(val weight: Int, val width: Float) {
    fun asViewSettings() = "'wght' $weight, 'wdth' $width"

    companion object {
        fun at(step: Int) = VariableAxes(300 + 500 * step / FONT_STEPS, 75f + 25f * step / FONT_STEPS)
    }
}

private class WindowFrames : Window.OnFrameMetricsAvailableListener {
    private val durations = mutableListOf<Long>()
    private var droppedReports = 0

    override fun onFrameMetricsAvailable(window: Window, metrics: FrameMetrics, dropped: Int) {
        metrics.getMetric(FrameMetrics.TOTAL_DURATION).takeIf { it >= 0 }?.let(durations::add)
        droppedReports += dropped
    }

    fun toJson(refreshRate: Float?) = JSONObject().apply {
        val sorted = durations.sorted()
        put("scope", "whole window, combined renderers")
        put("samples", sorted.size)
        put("droppedReports", droppedReports)
        put("displayRefreshRateHz", refreshRate ?: JSONObject.NULL)
        if (sorted.isNotEmpty()) {
            put("p50Ms", sorted[(sorted.lastIndex * 0.5).roundToInt()] / 1_000_000.0)
            put("p95Ms", sorted[(sorted.lastIndex * 0.95).roundToInt()] / 1_000_000.0)
        }
    }
}
