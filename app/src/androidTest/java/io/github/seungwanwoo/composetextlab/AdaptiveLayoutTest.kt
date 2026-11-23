package io.github.seungwanwoo.composetextlab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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
