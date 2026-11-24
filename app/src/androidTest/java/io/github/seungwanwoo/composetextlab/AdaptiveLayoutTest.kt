package io.github.seungwanwoo.composetextlab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdaptiveLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test fun reflowsAsTheWindowWidensAndRespectsManualLayout() {
        var width by mutableStateOf(420.dp)
        var layout by mutableStateOf(PanelLayout.Auto)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, 780.dp))) {
                MaterialTheme {
                    LabWorkspace(showSettings = true, settings = { Text("Settings") }) {
                        ProbePair(LabSettings(panelLayout = layout), compose = {
                            Text("Compose", Modifier.fillMaxWidth().testTag("left"))
                        }, view = {
                            Text("TextView", Modifier.fillMaxWidth().testTag("right"))
                        })
                    }
                }
            }
        }

        assertStacked()
        rule.onNodeWithTag("settings-pane").assertDoesNotExist()
        rule.runOnIdle { width = 850.dp }
        assertPaired()
        rule.onNodeWithTag("settings-pane").assertDoesNotExist()
        rule.runOnIdle { width = 1200.dp }
        assertPaired()
        assertTrue(bounds("settings-pane").right < bounds("left").left)
        rule.runOnIdle { layout = PanelLayout.Stacked }
        assertStacked()
    }

    @Test fun scrollingToASettingKeepsTheLiveComparisonAtTheTop() {
        var width by mutableStateOf(420.dp)
        var sample by mutableStateOf("Original sample")
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, 700.dp))) {
                MaterialTheme {
                    LabWorkspace(showSettings = true, settings = {
                        repeat(24) { Text("Setting ${it + 1}") }
                        TextButton(onClick = { sample = "Updated sample" }, modifier = Modifier.testTag("change-sample")) {
                            Text("Change sample")
                        }
                    }) {
                        Text(sample, Modifier.testTag("live-preview"))
                    }
                }
            }
        }

        val preview = rule.onNodeWithTag("live-preview")
        preview.assertIsDisplayed().assertTextEquals("Original sample")
        val compactTop = bounds("live-preview").top
        rule.onNodeWithTag("change-sample").performScrollTo().performClick()
        preview.assertIsDisplayed().assertTextEquals("Updated sample")
        assertEquals(compactTop, bounds("live-preview").top, 1f)

        rule.runOnIdle { width = 1200.dp }
        preview.assertIsDisplayed()
        val tabletTop = bounds("live-preview").top
        rule.onNodeWithTag("change-sample").performScrollTo().performClick()
        preview.assertIsDisplayed()
        assertEquals(tabletTop, bounds("live-preview").top, 1f)
    }

    private fun bounds(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun assertPaired() {
        val left = bounds("left")
        val right = bounds("right")
        assertTrue(left.right < right.left && left.top == right.top && abs(left.width - right.width) <= 1f)
    }

    private fun assertStacked() {
        val top = bounds("left")
        val bottom = bounds("right")
        assertTrue(top.left == bottom.left && top.bottom < bottom.top)
    }
}
