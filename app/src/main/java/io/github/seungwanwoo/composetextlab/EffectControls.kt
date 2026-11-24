package io.github.seungwanwoo.composetextlab

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import org.json.JSONObject

@Composable
internal fun EffectControls(
    settings: LabSettings,
    onChange: (LabSettings) -> Unit,
    running: Boolean,
    runPlayback: () -> Unit,
) {
    ChoiceRow("Text effect", TextEffect.entries, settings.effect) { onChange(settings.copy(effect = it)) }
    val options = settings.effectSettings
    fun change(next: EffectSettings) = onChange(settings.copy(effectSettings = next))
    val caption = MaterialTheme.typography.bodySmall

    when (settings.effect) {
        TextEffect.Shadow -> {
            LabeledSlider("Shadow offset X/Y (dp)", options.shadowOffsetDp, 0f..12f) {
                change(options.copy(shadowOffsetDp = it))
            }
            LabeledSlider("Shadow blur radius (dp)", options.shadowRadiusDp, 0f..16f) {
                change(options.copy(shadowRadiusDp = it))
            }
        }
        TextEffect.Gradient -> Text("Gradient anchors follow the physical left and right text-layout edges.", style = caption)
        TextEffect.Stroke, TextEffect.FillStroke -> {
            LabeledSlider("Stroke width (dp)", options.strokeWidthDp, 0.5f..8f) {
                change(options.copy(strokeWidthDp = it))
            }
            Text("The outline can extend outside the measured text. Adjust Padding to inspect the edges.", style = caption)
        }
        TextEffect.Marquee -> {
            LabeledSlider("Compose speed (dp/s)", options.marqueeVelocity, 10f..140f) {
                change(options.copy(marqueeVelocity = it))
            }
            Toggle("Custom Compose gap", options.marqueeGapOverride) {
                change(options.copy(marqueeGapOverride = it))
            }
            if (options.marqueeGapOverride) {
                LabeledSlider("Compose gap (dp)", options.marqueeGapDp, 0f..160f) {
                    change(options.copy(marqueeGapDp = it))
                }
            }
            LabeledSlider("Repeat count (0 = infinite)", options.marqueeIterations.toFloat(), 0f..5f, steps = 4) {
                change(options.copy(marqueeIterations = it.roundToInt()))
            }
            LabeledSlider("Compose start delay (ms)", options.marqueeDelayMs.toFloat(), 0f..2000f, steps = 19) {
                change(options.copy(marqueeDelayMs = it.roundToInt()))
            }
            Text("Marquee uses one line. TextView uses the platform speed and spacing.", style = caption)
        }
        TextEffect.Motion -> {
            ChoiceRow("Animation path", MotionPath.entries, options.motionPath) {
                change(options.copy(motionPath = it))
            }
            Toggle("Animated TextMotion", options.animatedTextMotion, "animated-text-motion") {
                change(options.copy(animatedTextMotion = it))
            }
        }
        TextEffect.VariableFont -> Text(
            "Roboto Variable · wght 300 → 800 · wdth 75 → 100",
            style = caption,
        )
        TextEffect.None -> Unit
    }
    if (settings.effect == TextEffect.Motion || settings.effect == TextEffect.VariableFont) {
        Button(onClick = runPlayback, enabled = !running) { Text("Run FX sample") }
    }
}

@Composable
internal fun EffectWorkReport(report: JSONObject?, running: Boolean) {
    val summary = MaterialTheme.typography.bodySmall
    val title = when {
        running -> "Sampling effect work…"
        report != null -> "Effect work during playback"
        else -> "Run an FX sample to compare rendering work."
    }
    Text(title, style = MaterialTheme.typography.titleSmall)
    if (report != null) {
        for ((key, label) in listOf("compose" to "Compose", "textView" to "TextView")) {
            val metrics = report.getJSONObject(key)
            Text("$label · measure ${metrics.getInt("measure")} · text layout ${metrics.getInt("textLayouts")} · draw ${metrics.getInt("draw")}",
                style = summary)
        }
        val frames = report.getJSONObject("windowFrames")
        if (frames.has("p95Ms")) {
            Text("Window · ${frames.getInt("samples")} frames · p95 " +
                "%.1f ms".format(java.util.Locale.ROOT, frames.getDouble("p95Ms")), style = summary)
        }
    }
    val context = LocalContext.current
    OutlinedButton(enabled = report != null && !running, onClick = {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Text effect report", report.toString()))
    }) { Text("Copy FX report") }
}
