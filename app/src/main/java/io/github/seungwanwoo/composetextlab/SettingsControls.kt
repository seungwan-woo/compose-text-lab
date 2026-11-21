package io.github.seungwanwoo.composetextlab

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun SettingsControls(s: LabSettings, fontName: String?, importing: Boolean,
    onChange: (LabSettings) -> Unit, onImport: () -> Unit, onSystemSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(value = s.text, onValueChange = { onChange(s.copy(text = it)) },
            label = { Text("Shared sample text") }, modifier = Modifier.fillMaxWidth().testTag("sample-input"),
            minLines = 2, maxLines = 4)
        ChoiceRow("Theme", ThemeChoice.entries, s.theme) { onChange(s.copy(theme = it)) }
        ChoiceRow("Font family", FontChoice.entries.filter { it != FontChoice.Imported || fontName != null }, s.font) {
            onChange(s.copy(font = it))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onImport, enabled = !importing) { Text(if (importing) "Importing…" else "Import TTF / OTF") }
            TextButton(onClick = onSystemSettings) { Text("System display settings") }
        }
        fontName?.let { Text("Imported: $it", style = MaterialTheme.typography.bodySmall) }
        Toggle("Follow system fontScale", s.useSystemFontScale, "system-scale") {
            onChange(s.copy(useSystemFontScale = it))
        }
        if (!s.useSystemFontScale) LabeledSlider("Preview fontScale", s.fontScale, 0.8f..2f,
            "preview-scale", 11) { onChange(s.copy(fontScale = it)) }
        LabeledSlider("Font size (sp)", s.fontSizeSp, 8f..72f) { onChange(s.copy(fontSizeSp = it)) }
        ChoiceRow("Weight", listOf(100, 300, 400, 500, 700, 900), s.weight) { onChange(s.copy(weight = it)) }
        LabeledSlider("Letter spacing (em)", s.letterSpacingEm, -0.05f..0.3f) {
            onChange(s.copy(letterSpacingEm = it))
        }
        LabeledSlider("Line height (sp; 0 = engine default)", s.lineHeightSp, 0f..100f) {
            onChange(s.copy(lineHeightSp = it))
        }
        LabeledSlider("Max lines", s.maxLines.toFloat(), 1f..20f, steps = 18) { onChange(s.copy(maxLines = it.toInt())) }
        LabeledSlider("Padding (dp)", s.paddingDp, 0f..32f) { onChange(s.copy(paddingDp = it)) }
        LabeledSlider("Requested panel width (dp)", s.previewWidthDp, 120f..600f) {
            onChange(s.copy(previewWidthDp = it))
        }
        Toggle("Include font padding", s.includeFontPadding) { onChange(s.copy(includeFontPadding = it)) }
        Toggle("End ellipsis", s.ellipsis) { onChange(s.copy(ellipsis = it)) }
        Toggle("Side by side", s.sideBySide) { onChange(s.copy(sideBySide = it)) }
        Toggle("Show baselines", s.showBaselines) { onChange(s.copy(showBaselines = it)) }
    }
}

@Composable
fun <T> ChoiceRow(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { choice ->
                FilterChip(selected = selected == choice, onClick = { onSelect(choice) }, label = { Text(choice.toString()) })
            }
        }
    }
}

@Composable
fun Toggle(label: String, checked: Boolean, tag: String = label, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    tag: String = label, steps: Int = 0, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${String.format(Locale.ROOT, "%.2f", value)}", style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps,
            modifier = Modifier.testTag(tag))
    }
}
