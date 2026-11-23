package io.github.seungwanwoo.composetextlab

import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.text.Layout
import android.view.View
import android.widget.TextView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MultilingualLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun comparesAppliedLinePoliciesAndExportsLiveBidiMeasurements() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        rule.onNodeWithText("Hide settings").performClick()
        rule.onNodeWithText("Side by side").performClick()
        rule.onNodeWithText("Lines").performClick()
        rule.onNodeWithText("Japanese").performScrollTo().performClick()
        rule.onNodeWithText("Balanced").performScrollTo().performClick()

        if (Build.VERSION.SDK_INT >= 33) {
            rule.onNodeWithText("Strict").performScrollTo().performClick()
            rule.onNodeWithText("Phrase").performScrollTo().performClick()
        }
        val japanese = composeLayout()
        assertEquals(LineBreak.Strategy.Balanced, japanese.layoutInput.style.lineBreak.strategy)
        assertEquals("ja-JP", japanese.layoutInput.style.localeList?.firstOrNull()?.toLanguageTag())
        rule.runOnIdle {
            val view = textView()
            assertEquals(Layout.BREAK_STRATEGY_BALANCED, view.breakStrategy)
            assertEquals("ja-JP", view.textLocale.toLanguageTag())
            if (Build.VERSION.SDK_INT >= 33) {
                assertEquals(LineBreak.Strictness.Strict, japanese.layoutInput.style.lineBreak.strictness)
                assertEquals(LineBreak.WordBreak.Phrase, japanese.layoutInput.style.lineBreak.wordBreak)
                assertEquals(LineBreakConfig.LINE_BREAK_STYLE_STRICT, view.lineBreakStyle)
                assertEquals(LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE, view.lineBreakWordStyle)
            }
        }

        rule.onNodeWithText("German").performScrollTo().performClick()
        rule.onNodeWithTag("line-hyphenation").performScrollTo().performClick().assertIsOn()
        assertEquals(Hyphens.Auto, composeLayout().layoutInput.style.hyphens)
        rule.runOnIdle {
            val expected = if (Build.VERSION.SDK_INT >= 33) Layout.HYPHENATION_FREQUENCY_FULL_FAST
                else Layout.HYPHENATION_FREQUENCY_FULL
            assertEquals(expected, textView().hyphenationFrequency)
            assertEquals("de-DE", textView().textLocale.toLanguageTag())
        }

        rule.onNodeWithText("Arabic + digits").performScrollTo().performClick()
        rule.onNodeWithText("First strong (RTL)").performScrollTo().performClick()
        val compose = composeLayout()
        assertEquals(TextDirection.ContentOrRtl, compose.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, compose.getParagraphDirection(0))

        rule.onNodeWithText("Multilingual line layout").performScrollTo()
        capture()
        val arabic = copyReport()
        val data = arabic.getJSONArray("measurements")
        val composeData = data.getJSONObject(0)
        val viewData = data.getJSONObject(1)
        assertEquals("compose", composeData.getString("engine"))
        assertEquals("textView", viewData.getString("engine"))
        assertEquals(composeData.getInt("layoutWidthPx"), viewData.getInt("layoutWidthPx"))
        val composeLines = composeData.getJSONArray("lines")
        assertEquals(compose.lineCount, composeLines.length())
        for (line in 0 until compose.lineCount) {
            assertEquals(compose.getLineStart(line), composeLines.getJSONObject(line).getInt("startUtf16"))
            assertEquals(compose.getLineEnd(line), composeLines.getJSONObject(line).getInt("endUtf16"))
        }
        rule.runOnIdle {
            val view = textView()
            val layout = checkNotNull(view.layout)
            assertEquals(View.TEXT_DIRECTION_FIRST_STRONG_RTL, view.textDirection)
            assertEquals(Layout.DIR_RIGHT_TO_LEFT, layout.getParagraphDirection(0))
            val viewLines = viewData.getJSONArray("lines")
            assertEquals(layout.lineCount, viewLines.length())
            for (line in 0 until layout.lineCount) {
                assertEquals(layout.getLineStart(line), viewLines.getJSONObject(line).getInt("startUtf16"))
                assertEquals(layout.getLineEnd(line), viewLines.getJSONObject(line).getInt("endUtf16"))
            }
        }
        for (engine in listOf(composeData, viewData)) {
            val carets = engine.getJSONArray("carets")
            val directions = List(carets.length()) { carets.getJSONObject(it).getString("runDirection") }.toSet()
            assertEquals(setOf("LTR", "RTL"), directions)
        }
        File(outputDirectory(), "ci-multilingual-lines.json").writeText(arabic.toString(2))

        rule.onNodeWithText("Emoji ZWJ").performScrollTo().performClick()
        val emoji = copyReport()
        val text = emoji.getJSONObject("settings").getString("text")
        val family = "👩‍👩‍👧‍👦"
        val start = text.indexOf(family)
        assertTrue(start >= 0)
        val boundaries = emoji.getJSONObject("icuBoundaries").getJSONArray("graphemeUtf16")
        val offsets = List(boundaries.length()) { boundaries.getInt(it) }
        assertTrue(start in offsets && start + family.length in offsets)
        assertFalse(offsets.any { it > start && it < start + family.length })
    }

    private fun composeLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("compose-probe").performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(results)) }
        return results.single()
    }

    private fun textView(): TextView = rule.activity.window.decorView.findViewWithTag("view-probe")

    private fun copyReport(): JSONObject {
        rule.onNodeWithText("Copy line report").performScrollTo().assertIsEnabled().performClick()
        return rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(ClipboardManager::class.java)
            JSONObject(checkNotNull(clipboard.primaryClip).getItemAt(0).text.toString())
        }
    }

    private fun outputDirectory(): File =
        (InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir).apply { mkdirs() }

    private fun capture() {
        rule.waitForIdle()
        val image = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(outputDirectory(), "ci-multilingual-lines.png").outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            image.recycle()
        }
    }
}
