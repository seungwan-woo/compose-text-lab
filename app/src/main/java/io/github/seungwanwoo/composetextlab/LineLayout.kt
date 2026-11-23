package io.github.seungwanwoo.composetextlab

import android.graphics.text.LineBreakConfig
import android.icu.text.BreakIterator
import android.os.Build
import android.text.Layout
import android.view.View
import android.widget.TextView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal enum class LineSample(val label: String, val locale: String, val text: String) {
    Korean("Korean + English", "ko-KR",
        "한글과 English typography를 함께 비교합니다. 좁은 카드에서도 internationalization과 어절 경계를 확인합니다."),
    Japanese("Japanese", "ja-JP",
        "「日本語の文章（かっこ付き）」を使い、句読点、長音符、小さい仮名の改行位置を比較します。"),
    Arabic("Arabic + digits", "ar",
        "مرحبا بالعالم! رقم الطلب ABC-123 (٤٥٦) — هذا سطر لاختبار اتجاه النص.\n(123) — (٤٥٦)"),
    German("German", "de-DE",
        "Die Donaudampfschifffahrtsgesellschaft untersucht Silbentrennung und Internationalisierung."),
    Emoji("Emoji ZWJ", "en-US",
        "Family 👩‍👩‍👧‍👦, developer 👩🏽‍💻, flag 🇰🇷 and e\u0301. Compare grapheme boundaries.");

    override fun toString() = label
}

internal enum class WrapStrategy(
    private val label: String,
    val compose: LineBreak.Strategy,
    val view: Int,
) {
    Simple("Simple", LineBreak.Strategy.Simple, Layout.BREAK_STRATEGY_SIMPLE),
    HighQuality("High quality", LineBreak.Strategy.HighQuality, Layout.BREAK_STRATEGY_HIGH_QUALITY),
    Balanced("Balanced", LineBreak.Strategy.Balanced, Layout.BREAK_STRATEGY_BALANCED);

    override fun toString() = label
}

internal enum class WrapStrictness(val compose: LineBreak.Strictness, val view: Int) {
    Loose(LineBreak.Strictness.Loose, LineBreakConfig.LINE_BREAK_STYLE_LOOSE),
    Normal(LineBreak.Strictness.Normal, LineBreakConfig.LINE_BREAK_STYLE_NORMAL),
    Strict(LineBreak.Strictness.Strict, LineBreakConfig.LINE_BREAK_STYLE_STRICT),
}

internal enum class WrapWordBreak(val compose: LineBreak.WordBreak, val view: Int) {
    Default(LineBreak.WordBreak.Default, LineBreakConfig.LINE_BREAK_WORD_STYLE_NONE),
    Phrase(LineBreak.WordBreak.Phrase, LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE),
}

internal enum class LineDirection(
    private val label: String,
    val compose: TextDirection,
    val view: Int,
) {
    FirstStrongLtr("First strong (LTR)", TextDirection.ContentOrLtr, View.TEXT_DIRECTION_FIRST_STRONG_LTR),
    FirstStrongRtl("First strong (RTL)", TextDirection.ContentOrRtl, View.TEXT_DIRECTION_FIRST_STRONG_RTL),
    Ltr("Force LTR", TextDirection.Ltr, View.TEXT_DIRECTION_LTR),
    Rtl("Force RTL", TextDirection.Rtl, View.TEXT_DIRECTION_RTL);

    override fun toString() = label
}

internal data class LineLayoutPolicy(
    val localeTag: String,
    val strategy: WrapStrategy,
    val strictness: WrapStrictness,
    val wordBreak: WrapWordBreak,
    val hyphenate: Boolean,
    val direction: LineDirection,
) {
    val hasEastAsianRules get() = Build.VERSION.SDK_INT >= 33
    val composeLineBreak get() = LineBreak(strategy.compose, strictness.compose, wordBreak.compose)
    // AndroidX AndroidParagraph maps Hyphens.Auto to these same Android frequencies.
    val viewHyphenation get() = when {
        !hyphenate -> Layout.HYPHENATION_FREQUENCY_NONE
        Build.VERSION.SDK_INT >= 33 -> Layout.HYPHENATION_FREQUENCY_FULL_FAST
        else -> Layout.HYPHENATION_FREQUENCY_FULL
    }

    fun applyTo(view: TextView) {
        view.textLocale = Locale.forLanguageTag(localeTag)
        view.textDirection = direction.view
        view.breakStrategy = strategy.view
        view.hyphenationFrequency = viewHyphenation
        if (Build.VERSION.SDK_INT >= 33) {
            view.lineBreakStyle = strictness.view
            view.lineBreakWordStyle = wordBreak.view
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("localeTag", localeTag)
        put("strategy", strategy.name)
        put("strictness", strictness.name)
        put("wordBreak", wordBreak.name)
        put("eastAsianRulesSupported", hasEastAsianRules)
        put("hyphenation", if (hyphenate) "Auto" else "Off")
        put("textViewHyphenationFrequency", viewHyphenation)
        put("direction", direction.name)
    }
}

internal data class IcuBoundaries(val graphemes: List<Int>, val words: List<Int>, val lineOpportunities: List<Int>) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("icuVersion", android.icu.util.VersionInfo.ICU_VERSION.toString())
        put("graphemeUtf16", JSONArray(graphemes))
        put("wordBoundaryUtf16", JSONArray(words))
        put("lineOpportunityUtf16", JSONArray(lineOpportunities))
    }
}

internal fun inspectIcu(text: String, localeTag: String): IcuBoundaries {
    val locale = Locale.forLanguageTag(localeTag)
    fun boundaries(iterator: BreakIterator) = buildList {
        iterator.setText(text)
        var offset = iterator.first()
        while (offset != BreakIterator.DONE) {
            add(offset)
            offset = iterator.next()
        }
    }
    return IcuBoundaries(
        boundaries(BreakIterator.getCharacterInstance(locale)),
        boundaries(BreakIterator.getWordInstance(locale)),
        boundaries(BreakIterator.getLineInstance(locale)),
    )
}

internal fun TextLayoutResult.observeLines(padding: Int): List<LineObservation> = List(lineCount) { line ->
    val start = getLineStart(line)
    LineObservation(start, getLineEnd(line), getParagraphDirection(start).label,
        getLineBaseline(line) + padding, getLineLeft(line) + padding, getLineRight(line) + padding)
}

internal fun Layout.observeLines(visibleLines: Int, topPadding: Int, leftPadding: Int): List<LineObservation> =
    List(visibleLines) { line ->
        LineObservation(getLineStart(line), getLineEnd(line),
            if (getParagraphDirection(line) == Layout.DIR_RIGHT_TO_LEFT) "RTL" else "LTR",
            (getLineBaseline(line) + topPadding).toFloat(), getLineLeft(line) + leftPadding, getLineRight(line) + leftPadding)
    }

internal fun TextLayoutResult.observeCarets(graphemes: List<Int>, padding: Int): List<CaretObservation> =
    graphemes.takeWhile { it < getLineEnd(lineCount - 1) }.map { offset ->
        CaretObservation(offset, getLineForOffset(offset), getBidiRunDirection(offset).label,
            getHorizontalPosition(offset, usePrimaryDirection = true) + padding,
            getHorizontalPosition(offset, usePrimaryDirection = false) + padding)
    }

internal fun Layout.observeCarets(graphemes: List<Int>, visibleLines: Int, leftPadding: Int): List<CaretObservation> =
    graphemes.takeWhile { it < getLineEnd(visibleLines - 1) }.map { offset ->
        CaretObservation(offset, getLineForOffset(offset), if (isRtlCharAt(offset)) "RTL" else "LTR",
            getPrimaryHorizontal(offset) + leftPadding, getSecondaryHorizontal(offset) + leftPadding)
    }

private val ResolvedTextDirection.label get() = if (this == ResolvedTextDirection.Rtl) "RTL" else "LTR"
