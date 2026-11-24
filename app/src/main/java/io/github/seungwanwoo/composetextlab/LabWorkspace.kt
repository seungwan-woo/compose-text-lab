package io.github.seungwanwoo.composetextlab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal typealias ShowComparison = @Composable (
    controls: (@Composable () -> Unit)?,
    preview: @Composable () -> Unit,
) -> Unit

@Composable
internal fun LabWorkspace(
    showSettings: Boolean,
    settings: @Composable ColumnScope.() -> Unit,
    controls: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val contentScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dockSettings = showSettings && maxWidth >= 1100.dp
        val controlsBelow = !dockSettings && (showSettings || controls != null)
        val previewSize = if (controlsBelow) {
            // A stable viewport keeps controls under the pointer while changes reflow the preview.
            val previewFraction = if (showSettings) 0.55f else 0.68f
            Modifier.height(maxHeight * previewFraction)
        } else {
            Modifier.fillMaxHeight()
        }
        val options: @Composable ColumnScope.() -> Unit = {
            controls?.invoke()
            if (showSettings) settings()
        }
        Row(Modifier.fillMaxSize()) {
            if (dockSettings) {
                Column(
                    Modifier.width(320.dp).fillMaxHeight().verticalScroll(settingsScroll)
                        .padding(16.dp).testTag("settings-pane"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = options,
                )
                VerticalDivider(Modifier.fillMaxHeight())
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxWidth().then(previewSize).verticalScroll(contentScroll)
                        .padding(16.dp).testTag("comparison-pane"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
                if (controlsBelow) {
                    HorizontalDivider()
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(settingsScroll).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        content = options,
                    )
                }
            }
        }
    }
}
