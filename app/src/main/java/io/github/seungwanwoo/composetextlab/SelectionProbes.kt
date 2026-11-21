package io.github.seungwanwoo.composetextlab

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SelectionProbes(s: LabSettings, font: FontAsset?, composeValue: TextFieldValue,
    onComposeValue: (TextFieldValue) -> Unit, viewValue: TextFieldValue, onViewValue: (TextFieldValue) -> Unit,
    events: List<String>, onEvent: (String) -> Unit) {
    val style = probeStyle(s.copy(effect = TextEffect.None), font)
    val color = MaterialTheme.colorScheme.onSurface
    val viewChanged by rememberUpdatedState(onViewValue)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Long press each sample to compare selection handles and the platform text menu.")
        Text("Remote toolbar routing is unverified. Logs show public API observations only.", style = MaterialTheme.typography.bodySmall)
        ProbePanel("Compose · SelectionContainer", Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(s.text, style = style, modifier = Modifier.fillMaxWidth().padding(s.paddingDp.dp)
                    .testTag("compose-selectable"))
            }
            Text("Selection offsets are not exposed here.", style = MaterialTheme.typography.bodySmall)
        }
        ProbePanel("View · selectable TextView", Modifier.fillMaxWidth().testTag("view-selectable-panel")) {
            AndroidView(factory = { context -> TextView(context).apply {
                tag = "view-selectable"; applySelectionStyle(this, s, font, color.toArgb())
                text = s.text; setTextIsSelectable(true)
                customSelectionActionModeCallback = LabActionMode(this, s.customViewMenu, onEvent)
            } }, modifier = Modifier.fillMaxWidth())
        }
        ProbePanel("Compose · BasicTextField", Modifier.fillMaxWidth()) {
            BasicTextField(value = composeValue, onValueChange = { value ->
                if (value.selection != composeValue.selection || value.text != composeValue.text)
                    onEvent("Compose selection=${value.selection.start}..${value.selection.end}, length=${value.text.length}")
                onComposeValue(value)
            }, textStyle = style,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline).padding(s.paddingDp.dp).testTag("compose-editor"))
            Text("selection=${composeValue.selection.start}..${composeValue.selection.end}", style = MaterialTheme.typography.bodySmall)
        }
        ProbePanel("View · EditText", Modifier.fillMaxWidth()) {
            AndroidView(factory = { context -> ObservingEditText(context).apply {
                tag = "view-editor"; applySelectionStyle(this, s, font, color.toArgb())
                setText(viewValue.text)
                setSelection(viewValue.selection.start.coerceIn(0, text.length), viewValue.selection.end.coerceIn(0, text.length))
                onSelection = { start, end ->
                    viewChanged(TextFieldValue(text.toString(), TextRange(start.coerceAtLeast(0), end.coerceAtLeast(0))))
                    onEvent("EditText selection=$start..$end, length=${text.length}")
                }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(text: Editable?) {
                        viewChanged(TextFieldValue(text?.toString().orEmpty(), TextRange(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))))
                        onEvent("EditText changed, length=${text?.length ?: 0}")
                    }
                })
                customSelectionActionModeCallback = LabActionMode(this, s.customViewMenu, onEvent)
                customInsertionActionModeCallback = LabActionMode(this, s.customViewMenu, onEvent)
            } }, modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp))
        }
        Text("Last 60 public API events", style = MaterialTheme.typography.titleSmall)
        if (events.isEmpty()) Text("No events yet. Select or edit text above.")
        events.takeLast(60).asReversed().forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun applySelectionStyle(view: TextView, s: LabSettings, font: FontAsset?, color: Int) = with(view) {
    typeface = weightedTypeface(s, font)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, s.fontSizeSp)
    setTextColor(color)
    includeFontPadding = s.includeFontPadding; letterSpacing = s.letterSpacingEm
    val p = (s.paddingDp * resources.displayMetrics.density).toInt()
    setPadding(p, p, p, p)
    if (s.lineHeightSp > 0) setLineSpacing(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, s.lineHeightSp, resources.displayMetrics) -
            (paint.fontMetrics.descent - paint.fontMetrics.ascent), 1f)
}

class ObservingEditText(context: Context) : EditText(context) {
    var onSelection: ((Int, Int) -> Unit)? = null
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelection?.invoke(selStart, selEnd)
    }
}

private class LabActionMode(private val view: TextView, private val custom: Boolean,
    private val event: (String) -> Unit) : ActionMode.Callback {
    private val id = 0xC710
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        event("${view.tag}: ActionMode create, type=${mode.type}, menu=${titles(menu)}")
        if (custom) menu.add(Menu.NONE, id, Menu.NONE, "Lab: log selection").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        event("${view.tag}: ActionMode prepare, menu=${titles(menu)}")
        return false
    }
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        event("${view.tag}: click=${item.title}, selection=${view.selectionStart}..${view.selectionEnd}")
        if (item.itemId != id) return false
        mode.finish()
        return true
    }
    override fun onDestroyActionMode(mode: ActionMode) { event("${view.tag}: ActionMode destroy") }
    private fun titles(menu: Menu) = (0 until menu.size()).joinToString { menu.getItem(it).title.toString() }
}
