package io.github.seungwanwoo.composetextlab

import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.widget.TextView
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextMotion
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EffectsMotionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun appliesShadowAndStrokeAtDarkThemeAndScaledText() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        rule.onNodeWithText("Theme").performScrollTo()
        activate("Dark")
        rule.onNodeWithTag("system-scale").performScrollTo().performClick()
        changeSlider("preview-scale", 1.3f)
        activate("Effects")
        activate("Shadow")
        changeSlider("Shadow offset X/Y (dp)", 5f)
        changeSlider("Shadow blur radius (dp)", 7f)
        val shadow = checkNotNull(composeLayout().layoutInput.style.shadow)
        rule.runOnIdle {
            val view = viewProbe()
            val density = view.resources.displayMetrics.density
            assertEquals(1.3f, view.resources.configuration.fontScale, 0.01f)
            assertEquals(5f * density, shadow.offset.x, 0.1f)
            assertEquals(7f * density, shadow.blurRadius, 0.1f)
            assertEquals(shadow.offset.x, view.shadowDx, 0.1f)
            assertEquals(shadow.blurRadius, view.shadowRadius, 0.1f)
        }

        activate("Stroke")
        changeSlider("Stroke width (dp)", 4f)
        val strokeLayout = composeLayout()
        val stroke = strokeLayout.layoutInput.style.drawStyle as Stroke
        rule.runOnIdle {
            assertEquals(Paint.Style.STROKE, viewProbe().paint.style)
            assertEquals(stroke.width, viewProbe().paint.strokeWidth, 0.1f)
        }
        activate("Fill + stroke")
        assertEquals(strokeLayout.lineCount, composeLayout().lineCount)
        rule.runOnIdle { assertEquals(Paint.Style.FILL, viewProbe().paint.style) }
    }

    @Test fun samplesLayerDrawAndVariableFontWorkInBothEngines() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        activate("Hide settings")
        activate("Side by side")
        activate("Effects")
        // Capture the preview before the clipboard overlay appears.
        activate("Variable font")
        val variable = runProbe(capturePreview = true)
        assertEquals("VariableFont", variable.getString("path"))
        assertTrue(variable.getJSONObject("compose").getInt("textLayouts") > 0)
        assertTrue(variable.getJSONObject("textView").getInt("textLayouts") > 0)
        for (engine in listOf("compose", "textView")) {
            val widths = variable.getJSONObject(engine)
            assertNotEquals(widths.getDouble("firstLineWidthBeforePx"), widths.getDouble("firstLineWidthAfterPx"), 0.01)
        }
        val compose = composeLayout()
        assertTrue(compose.getLineRight(0) > compose.getLineLeft(0))
        rule.runOnIdle {
            assertTrue(viewProbe().fontVariationSettings.orEmpty().contains("wdth"))
        }

        activate("Motion")
        rule.onNodeWithTag("animated-text-motion").performScrollTo().performClick().assertIsOn()
        assertEquals(TextMotion.Animated, composeLayout().layoutInput.style.textMotion)
        rule.runOnIdle {
            val paint = viewProbe().paint
            assertTrue(paint.isSubpixelText)
            assertTrue(paint.isLinearText)
            assertEquals(Paint.HINTING_OFF, paint.hinting)
        }

        val layer = runProbe()
        assertEquals("Layer", layer.getString("path"))
        assertEquals(0, layer.getJSONObject("compose").getInt("textLayouts"))
        assertEquals(0, layer.getJSONObject("textView").getInt("textLayouts"))

        activate("Canvas redraw")
        val redraw = runProbe()
        assertEquals("Canvas", redraw.getString("path"))
        for (engine in listOf("compose", "textView")) {
            val work = redraw.getJSONObject(engine)
            assertTrue("$engine draw work: $work", work.getInt("draw") > 0)
        }

        val report = JSONObject().put("layer", layer).put("redraw", redraw).put("variableFont", variable)
        File(outputDirectory(), "ci-effects-motion.json").writeText(report.toString(2))
    }

    private fun runProbe(capturePreview: Boolean = false): JSONObject {
        val run = rule.onNodeWithText("Run FX sample").performScrollTo()
        run.performClick()
        val copy = rule.onNodeWithText("Copy FX report")
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodes(hasText("Copy FX report") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        if (capturePreview) capture()
        copy.performScrollTo().performClick()
        return rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(ClipboardManager::class.java)
            JSONObject(checkNotNull(clipboard.primaryClip).getItemAt(0).text.toString())
        }
    }

    private fun activate(label: String) {
        rule.onNodeWithText(label).performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
    }

    private fun changeSlider(tag: String, value: Float) {
        rule.onNodeWithTag(tag).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
            assertTrue(it(value))
        }
    }

    private fun composeLayout(): TextLayoutResult {
        val result = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("compose-probe").performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(result)) }
        return result.single()
    }

    private fun viewProbe(): TextView = rule.activity.window.decorView.findViewWithTag("view-probe")

    private fun outputDirectory(): File =
        (InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir).apply { mkdirs() }

    private fun capture() {
        rule.onNodeWithTag("compose-probe").performScrollTo()
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(outputDirectory(), "ci-effects-motion.png").outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            image.recycle()
        }
    }
}
