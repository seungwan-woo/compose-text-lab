package io.github.seungwanwoo.composetextlab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
internal fun MultilingualProbes(
    settings: LabSettings,
    font: FontAsset?,
    fontIdentity: String,
    environment: Map<String, String>,
    previewContext: Context,
    previewDensity: Density,
    showComparison: ShowComparison,
) {
    var sample by rememberSaveable { mutableStateOf(LineSample.Korean) }
    var locale by rememberSaveable { mutableStateOf("Sample locale") }
    var strategy by rememberSaveable { mutableStateOf(WrapStrategy.HighQuality) }
    var strictness by rememberSaveable { mutableStateOf(WrapStrictness.Normal) }
    var wordBreak by rememberSaveable { mutableStateOf(WrapWordBreak.Default) }
    var direction by rememberSaveable { mutableStateOf(LineDirection.FirstStrongLtr) }
    var hyphenate by rememberSaveable { mutableStateOf(false) }
    var panelWidth by rememberSaveable { mutableStateOf(300f) }
    val systemLocale = LocalConfiguration.current.locales[0].toLanguageTag()
    val localeTag = when (locale) {
        "Sample locale" -> sample.locale
        "System locale" -> systemLocale
        else -> locale
    }
    val policy = LineLayoutPolicy(localeTag, strategy, strictness, wordBreak, hyphenate, direction)
    val icu = remember(sample, localeTag) { inspectIcu(sample.text, localeTag) }
    // Keep a limit so extreme font scale and narrow widths can be inspected without an unbounded panel.
    val probe = settings.copy(text = sample.text, maxLines = 20, ellipsis = false,
        previewWidthDp = panelWidth, effect = TextEffect.None)
    var measurements by remember(probe, font?.sha256, policy, previewContext) {
        mutableStateOf<Map<String, ProbeMeasurement>>(emptyMap())
    }
    val compose = measurements["compose"]
    val view = measurements["textView"]
    val sameWidth = compose != null && view != null && compose.layoutWidthPx == view.layoutWidthPx
    val complete = compose?.overflow == false && view?.overflow == false
    val sameRanges = compose?.lines?.map { it.startUtf16 to it.endUtf16 } ==
        view?.lines?.map { it.startUtf16 to it.endUtf16 }
    val caption = MaterialTheme.typography.bodySmall

    showComparison({
        val sampleControls: @Composable () -> Unit = {
            ChoiceRow("Sample", LineSample.entries, sample) { sample = it }
            ChoiceRow("Break strategy", WrapStrategy.entries, strategy) { strategy = it }
            ChoiceRow("BiDi paragraph direction", LineDirection.entries, direction) { direction = it }
        }
        val localeControls: @Composable () -> Unit = {
            ChoiceRow("Locale · $localeTag", listOf("Sample locale", "System locale", "ko-KR", "en-US", "ja-JP", "ar", "de-DE"),
                locale) { locale = it }
            if (policy.hasEastAsianRules) {
                ChoiceRow("East Asian line rules", WrapStrictness.entries, strictness) { strictness = it }
                ChoiceRow("Word break", WrapWordBreak.entries, wordBreak) { wordBreak = it }
            } else {
                Text("East Asian line rules and phrase breaking require Android API 33.", style = caption)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 680.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { sampleControls() }
                    Column(Modifier.weight(1f)) { localeControls() }
                }
            } else {
                Column {
                    sampleControls()
                    localeControls()
                }
            }
        }
        Toggle("Automatic hyphenation", hyphenate, "line-hyphenation") { hyphenate = it }
        LabeledSlider("Maximum line panel width (dp)", panelWidth, 140f..600f, "line-panel-width") { panelWidth = it }
    }) {
        Text("Multilingual line layout", style = MaterialTheme.typography.titleMedium)
        key(probe, policy, font?.sha256, previewContext) {
            CompositionLocalProvider(LocalContext provides previewContext,
                LocalConfiguration provides previewContext.resources.configuration, LocalDensity provides previewDensity) {
                ComparisonProbes(probe, font, policy, icu.graphemes) { value ->
                    measurements = measurements + (value.engine to value)
                }
            }
        }
        if (compose != null && view != null) {
            val ranges = if (sameRanges) "match" else "differ"
            val scope = if (complete) "All line ranges" else "Measured line ranges (clipped)"
            val widths = if (sameWidth) "Shared text width ${compose.layoutWidthPx} px"
                else "Text widths ${compose.layoutWidthPx} / ${view.layoutWidthPx} px (Compose / View)"
            Text("$scope $ranges · $widths", style = MaterialTheme.typography.labelLarge)
            Text("UTF-16 ranges [start, end) · paragraph direction · widget-relative coordinates in px", style = caption)
            ProbePair(probe, { LineMetricsPanel("Compose lines", compose.lines) },
                { LineMetricsPanel("TextView lines", view.lines) })
        }
        Text("ICU boundaries · ${icu.graphemes.size} grapheme · ${icu.words.size} word · " +
            "${icu.lineOpportunities.size} line opportunities", style = caption)
        Text("Line opportunities are ICU diagnostic offsets. The rows above come from the rendered layouts.", style = caption)
        val context = LocalContext.current
        OutlinedButton(enabled = compose != null && view != null, onClick = {
            if (compose != null && view != null) {
                val report = buildReport(probe, environment + ("experiment" to "MultilingualLines"),
                    fontIdentity, listOf(compose, view)).apply {
                    put("measurementScope", "rendered multilingual lines and BiDi carets at ICU grapheme starts")
                    put("lineSample", sample.name)
                    put("linePolicy", policy.toJson())
                    put("icuBoundaries", icu.toJson())
                    put("sameLayoutWidthPx", sameWidth)
                    put("completeTextVisible", complete)
                    put("matchingMeasuredLineRanges", sameRanges)
                }
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Multilingual line report", report.toString(2)))
            }
        }) { Text("Copy line report") }
    }
}

@Composable
private fun LineMetricsPanel(title: String, lines: List<LineObservation>) {
    ProbePanel(title, Modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            val position = String.format(Locale.ROOT, "y %.1f · x %.1f–%.1f",
                line.baselinePx, line.leftPx, line.rightPx)
            Text("${index + 1}. [${line.startUtf16}, ${line.endUtf16}) ${line.paragraphDirection} · $position",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
