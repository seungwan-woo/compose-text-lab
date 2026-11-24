package io.github.seungwanwoo.composetextlab

import android.content.ClipboardManager
import android.graphics.Bitmap
import android.widget.TextView
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

    @Test fun samplesLayerDrawAndVariableFontWorkInBothEngines() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        activate("Hide settings")
        activate("Side by side")
        activate("Effects")
        activate("Motion")
        rule.onNodeWithTag("animated-text-motion").performScrollTo().performClick().assertIsOn()
        assertEquals(TextMotion.Animated, composeLayout().layoutInput.style.textMotion)
        rule.runOnIdle { assertTrue(viewProbe().paint.isSubpixelText) }

        val layer = runProbe()
        assertEquals("Layer", layer.getString("path"))
        assertTrue(layer.getJSONObject("compose").getInt("textLayouts") == 0)
        assertTrue(layer.getJSONObject("textView").getInt("measure") == 0)

        activate("Canvas redraw")
        val redraw = runProbe()
        assertEquals("Canvas", redraw.getString("path"))
        for (engine in listOf("compose", "textView")) {
            assertTrue(redraw.getJSONObject(engine).toString(), redraw.getJSONObject(engine).getInt("draw") > 0)
        }

        activate("Variable font")
        val variable = runProbe()
        assertEquals("VariableFont", variable.getString("path"))
        assertTrue(variable.getJSONObject("compose").getInt("textLayouts") > 0)
        assertTrue(variable.getJSONObject("textView").getInt("measure") > 0)
        val compose = composeLayout()
        assertTrue(compose.getLineRight(0) > compose.getLineLeft(0))
        rule.runOnIdle {
            assertTrue(viewProbe().fontVariationSettings.orEmpty().contains("wdth"))
        }
        capture(JSONObject().put("layer", layer).put("redraw", redraw).put("variableFont", variable))
    }

    private fun runProbe(): JSONObject {
        val run = rule.onNodeWithText("Run FX sample").performScrollTo()
        run.performClick()
        val copy = rule.onNodeWithText("Copy FX report")
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodes(hasText("Copy FX report") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        copy.performScrollTo().performClick()
        return rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(ClipboardManager::class.java)
            JSONObject(checkNotNull(clipboard.primaryClip).getItemAt(0).text.toString())
        }
    }

    private fun activate(label: String) {
        rule.onNodeWithText(label).performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
    }

    private fun composeLayout(): TextLayoutResult {
        val result = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("compose-probe").performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(result)) }
        return result.single()
    }

    private fun viewProbe(): TextView = rule.activity.window.decorView.findViewWithTag("view-probe")

    private fun capture(report: JSONObject) {
        rule.onNodeWithTag("compose-probe").performScrollTo()
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: instrumentation.targetContext.filesDir
        directory.mkdirs()
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "ci-effects-motion.png").outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            File(directory, "ci-effects-motion.json").writeText(report.toString(2))
        } finally {
            image.recycle()
        }
    }
}
