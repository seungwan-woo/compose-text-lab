package io.github.seungwanwoo.composetextlab

import android.widget.TextView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class LabUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun resetExperimentSettings() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
    }

    @Test fun sharedTextAndScaleReachBothEnginesAndSurviveRecreation() {
        rule.onNodeWithTag("sample-input").performTextReplacement("한글 baseline Ag🙂")
        rule.onNodeWithTag("system-scale").performScrollTo().performClick()
        rule.onNodeWithTag("preview-scale").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(1.6f) }
        rule.onNodeWithTag("compose-probe").performScrollTo().assertTextEquals("한글 baseline Ag🙂")
        rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<TextView>("view-probe")
            assertNotNull(view)
            assertEquals("한글 baseline Ag🙂", view.text.toString())
            assertEquals(1.6f, view.resources.configuration.fontScale, 0.01f)
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("compose-probe").performScrollTo().assertTextEquals("한글 baseline Ag🙂")
        rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<TextView>("view-probe")
            assertEquals(1.6f, view.resources.configuration.fontScale, 0.01f)
        }
    }

    @Test fun effectsSwitchesAndSelectionFieldsAreUsable() {
        rule.onNodeWithText("Effects", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Hide settings").performClick()
        rule.onNodeWithText("Gradient").performClick().assertIsSelected()
        rule.onNodeWithTag("compose-probe").performScrollTo().assertExists()
        rule.onNodeWithText("Selection", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("compose-editor").performScrollTo().performTextReplacement("Compose edited")
        rule.onNodeWithTag("compose-editor").assertTextContains("Compose edited")
        rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<android.widget.EditText>("view-editor")
            assertNotNull(view)
            assertTrue(view.isFocusable)
            view.setText("View edited")
        }
        rule.onNodeWithTag("View custom ActionMode item").performScrollTo().performClick()
        rule.onNodeWithTag("compose-editor").performScrollTo().assertTextContains("Compose edited")
        rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<android.widget.EditText>("view-editor")
            assertEquals("View edited", view.text.toString())
        }
        rule.onNodeWithText("Render", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Selection", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("compose-editor").performScrollTo().assertTextContains("Compose edited")
    }

    @Test fun marqueeReportSeparatesViewportFromUnconstrainedTextLayout() {
        rule.onNodeWithTag("sample-input").performScrollTo().performTextReplacement("long sample ".repeat(20))
        rule.onNodeWithText("Effects", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Hide settings").performClick()
        rule.onNodeWithText("Marquee").performClick().assertIsSelected()
        rule.onNodeWithTag("compose-probe").performScrollTo()
        rule.onNodeWithText("Report", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Copy JSON").performScrollTo().performClick()
        rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val json = org.json.JSONObject(clipboard.primaryClip!!.getItemAt(0).text.toString())
            val measurements = json.getJSONArray("measurements")
            val compose = (0 until measurements.length()).map { measurements.getJSONObject(it) }.first { it.getString("engine") == "compose" }
            assertTrue(compose.toString(), compose.getInt("widgetWidthPx") < compose.getInt("layoutWidthPx"))
            assertTrue(compose.toString(), compose.getBoolean("overflow"))
        }
    }

    @Test fun darkThemeReachesPlatformContextAndReportsAreReal() {
        rule.onNodeWithText("Theme").performScrollTo()
        rule.onNodeWithText("Dark").performClick().assertIsSelected()
        rule.onNodeWithTag("compose-probe").performScrollTo()
        rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<TextView>("view-probe")
            val mode = view.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val preferences = rule.activity.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).getString("json", "")
            assertEquals("settings=$preferences, context=${view.context.javaClass.name}, config=${view.resources.configuration}",
                android.content.res.Configuration.UI_MODE_NIGHT_YES, mode)
            assertTrue(view.layout.lineCount > 0)
        }
        rule.onNodeWithText("Report", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Copy JSON").performScrollTo().performClick()
        rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val json = org.json.JSONObject(clipboard.primaryClip!!.getItemAt(0).text.toString())
            assertEquals(2, json.getJSONArray("measurements").length())
            assertFalse(json.getBoolean("pixelEquivalenceVerified"))
        }
    }
}
