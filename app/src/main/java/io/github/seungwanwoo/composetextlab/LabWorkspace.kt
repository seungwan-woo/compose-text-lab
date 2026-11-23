package io.github.seungwanwoo.composetextlab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
internal fun LabWorkspace(
    showSettings: Boolean,
    settings: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val contentScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dockSettings = showSettings && maxWidth >= 1100.dp
        Row(Modifier.fillMaxSize()) {
            if (dockSettings) {
                Column(
                    Modifier.width(320.dp).fillMaxHeight().verticalScroll(settingsScroll)
                        .padding(16.dp).testTag("settings-pane"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = settings,
                )
                VerticalDivider(Modifier.fillMaxHeight())
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(contentScroll).padding(16.dp)
                    .testTag("comparison-pane"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (showSettings && !dockSettings) {
                    settings()
                    HorizontalDivider()
                }
                content()
            }
        }
    }
}
