package io.github.seungwanwoo.composetextlab

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LabTab(val label: String) {
    Render("Render"), Effects("Effects"), Selection("Selection"), RichText("Rich text"), Lines("Lines"), Report("Report")
}

@Composable
fun LabApp() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var savedSettings by rememberSaveable { mutableStateOf(prefs.getString("json", "") ?: "") }
    val s = remember(savedSettings) { LabSettings.fromJson(savedSettings) }
    var composeInput by rememberSaveable(s.text, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(s.text)) }
    var viewInput by rememberSaveable(s.text, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(s.text)) }
    fun change(next: LabSettings) { savedSettings = next.normalized().toJson().toString() }
    LaunchedEffect(savedSettings) { prefs.edit().putString("json", savedSettings).apply() }
    var tab by rememberSaveable { mutableStateOf(LabTab.Render) }
    val tabState = rememberSaveableStateHolder()
    var showSettings by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val fontRepository = remember { FontRepository(context.applicationContext) }
    var importedFont by remember { mutableStateOf<FontAsset?>(null) }
    var importing by remember { mutableStateOf(false) }
    var fontRestored by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var exportJson by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        importedFont = fontRepository.restore()
        fontRestored = true
        if (s.font == FontChoice.Imported && importedFont == null) {
            change(s.copy(font = FontChoice.System))
            status = "Saved font is unavailable; using System."
        }
    }
    val importFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try {
                importedFont = fontRepository.import(uri)
                change(LabSettings.fromJson(savedSettings).copy(font = FontChoice.Imported))
                status = "Imported ${importedFont?.displayName}"
            } catch (e: Exception) { status = "Font import failed: ${e.message}" }
            finally { importing = false }
        }
    }
    val saveReport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val payload = exportJson
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                        ?: error("Unable to open output document")
                }
                status = "Report saved"
            } catch (e: Exception) { status = "Report save failed: ${e.message}" }
        }
    }
    val systemDark = isSystemInDarkTheme()
    val dark = when (s.theme) { ThemeChoice.Dark -> true; ThemeChoice.Light -> false; else -> systemDark }
    val colors = if (s.theme == ThemeChoice.Dynamic && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme() else lightColorScheme()
    val previewConfiguration = Configuration(configuration).apply {
        fontScale = s.effectiveFontScale(configuration.fontScale)
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    }
    val previewContext = remember(context, previewConfiguration) {
        ContextThemeWrapper(context.createConfigurationContext(previewConfiguration),
            if (dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
    }
    val previewDensity = remember(previewContext) { Density(previewContext) }
    // Report can retain the last rendered experiment when its tab is opened. Any input/config change invalidates it.
    val experiment = if (tab == LabTab.Effects) s.effect else TextEffect.None
    var measuredEffect by remember { mutableStateOf(TextEffect.None) }
    LaunchedEffect(tab, s.effect) { if (tab == LabTab.Render || tab == LabTab.Effects) measuredEffect = experiment }
    var measurements by remember(s, importedFont?.sha256, previewConfiguration, measuredEffect) {
        mutableStateOf<Map<String, ProbeMeasurement>>(emptyMap())
    }
    val events = remember { mutableStateListOf<String>() }
    val onEvent: (String) -> Unit = { event ->
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(Date())
        events.add("$time $event")
        if (events.size > 60) events.removeAt(0)
    }
    val sdkFull = androidSdkVersion()
    val environment = mapOf(
        "manufacturer" to Build.MANUFACTURER, "model" to Build.MODEL, "sdk" to Build.VERSION.SDK_INT.toString(), "sdkFull" to sdkFull,
        "osRelease" to Build.VERSION.RELEASE, "fingerprint" to Build.FINGERPRINT,
        "systemFontScale" to configuration.fontScale.toString(),
        "previewFontScale" to previewContext.resources.configuration.fontScale.toString(),
        "densityDpi" to configuration.densityDpi.toString(), "locales" to configuration.locales.toLanguageTags(),
        "uiMode" to previewConfiguration.uiMode.toString(), "experiment" to measuredEffect.name,
        "composeBom" to "2025.12.00", "appVersion" to BuildConfig.VERSION_NAME,
    )
    val fontIdentity = if (s.font == FontChoice.Imported) "${importedFont?.displayName}; sha256=${importedFont?.sha256}" else s.font.name
    fun report() = buildReport(s.copy(effect = measuredEffect), environment,
        if (measuredEffect == TextEffect.VariableFont) "Roboto Variable (bundled)" else fontIdentity,
        measurements.values.sortedBy { it.engine }).toString(2)

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                val scrollTabs = maxWidth < 600.dp || configuration.fontScale >= 1.6f
                Column(Modifier.widthIn(max = 1440.dp).fillMaxSize().align(Alignment.TopCenter)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Compose Text Lab", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { showSettings = !showSettings }) { Text(if (showSettings) "Hide settings" else "Settings") }
                    }
                    val tabs: @Composable () -> Unit = {
                        LabTab.entries.forEach { item -> Tab(selected = tab == item, onClick = { tab = item }, text = { Text(item.label) }) }
                    }
                    if (scrollTabs) {
                        PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp, tabs = tabs)
                    } else {
                        PrimaryTabRow(selectedTabIndex = tab.ordinal, tabs = tabs)
                    }
                    key(tab) {
                        val workspace: ShowComparison = { tabControls, preview ->
                            val comparisonControls: @Composable () -> Unit = {
                                ChoiceRow("Comparison layout", PanelLayout.entries, s.panelLayout) {
                                    change(s.copy(panelLayout = it))
                                }
                                tabControls?.invoke()
                            }
                            LabWorkspace(showSettings, settings = {
                                SettingsControls(s, importedFont?.displayName, importing || !fontRestored,
                                    onChange = ::change,
                                    onImport = { importFont.launch(arrayOf("*/*")) },
                                    onSystemSettings = {
                                        runCatching { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
                                            .onFailure { status = "Display settings unavailable: ${it.message}" }
                                    })
                                TextButton(onClick = { change(LabSettings()); status = "Settings reset" }) {
                                    Text("Reset settings")
                                }
                            }, controls = if (tab == LabTab.Report) null else comparisonControls) {
                                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                                Text("System fontScale ${configuration.fontScale} · Preview ${previewConfiguration.fontScale} · API $sdkFull",
                                    style = MaterialTheme.typography.labelMedium)
                                preview()
                            }
                        }
                        when (tab) {
                            LabTab.Render, LabTab.Effects -> {
                                val playback = remember(s, previewContext, experiment, importedFont?.sha256) {
                                    if (experiment == TextEffect.Motion || experiment == TextEffect.VariableFont) {
                                        EffectPlayback(experiment, s.effectSettings, with(previewDensity) { 16.dp.toPx() })
                                    } else null
                                }
                                var runId by remember(playback) { mutableIntStateOf(0) }
                                var running by remember(playback) { mutableStateOf(false) }
                                var effectReport by remember(playback) { mutableStateOf<org.json.JSONObject?>(null) }
                                LaunchedEffect(playback, runId) {
                                    if (playback != null && runId > 0) {
                                        running = true
                                        effectReport = null
                                        try {
                                            effectReport = playback.sample((context as? Activity)?.window).apply {
                                                put("settings", s.toJson())
                                                put("environment", org.json.JSONObject(environment))
                                            }
                                        } finally {
                                            running = false
                                        }
                                    }
                                }
                                val effectControls: (@Composable () -> Unit)? = if (tab == LabTab.Effects) {
                                    @Composable {
                                        EffectControls(s, ::change, running) { runId++ }
                                    }
                                } else null
                                workspace(effectControls) {
                                    key(s, importedFont?.sha256, previewConfiguration, measuredEffect) {
                                        CompositionLocalProvider(LocalContext provides previewContext,
                                            LocalConfiguration provides previewContext.resources.configuration,
                                            LocalDensity provides previewDensity) {
                                            ComparisonProbes(s.copy(effect = experiment), importedFont, playback = playback) { value ->
                                                measurements = measurements + (value.engine to value)
                                            }
                                            if (tab == LabTab.Render) {
                                                ProbePanel("Theme defaults · intentionally different", Modifier.fillMaxWidth()) {
                                                    Text("Material 3 bodyLarge", style = MaterialTheme.typography.bodyLarge)
                                                    AndroidView(factory = { ctx -> TextView(ctx).apply {
                                                        text = "Framework default TextView"; tag = "theme-default-view"
                                                    } }, modifier = Modifier.fillMaxWidth())
                                                }
                                            }
                                        }
                                    }
                                    if (playback != null) EffectWorkReport(effectReport, running)
                                    measurements.values.sortedBy { it.engine }.forEach { m ->
                                        Text("${m.engine}: widget ${m.widgetWidthPx}×${m.widgetHeightPx}px · layout ${m.layoutWidthPx}×${m.layoutHeightPx}px\n" +
                                            "lines=${m.lineCount}, baseline=${m.firstBaselineFromWidgetTopPx}/${m.lastBaselineFromWidgetTopPx}px · ellipsis=${m.ellipsized}, overflow=${m.overflow}",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            LabTab.Selection -> workspace({
                                Toggle("View custom ActionMode item", s.customViewMenu) { change(s.copy(customViewMenu = it)) }
                                TextButton(onClick = { events.clear() }) { Text("Clear events") }
                            }) {
                                key(s, importedFont?.sha256, previewConfiguration) {
                                    CompositionLocalProvider(LocalContext provides previewContext,
                                        LocalConfiguration provides previewContext.resources.configuration, LocalDensity provides previewDensity) {
                                        SelectionProbes(s, importedFont, composeInput, { composeInput = it },
                                            viewInput, { viewInput = it }, events.toList(), onEvent)
                                    }
                                }
                            }
                            LabTab.RichText -> workspace(null) {
                                key(s, importedFont?.sha256, previewConfiguration) {
                                    CompositionLocalProvider(LocalContext provides previewContext,
                                        LocalConfiguration provides previewContext.resources.configuration, LocalDensity provides previewDensity) {
                                        RichTextProbes(s, importedFont)
                                    }
                                }
                            }
                            LabTab.Lines -> tabState.SaveableStateProvider(LabTab.Lines.name) {
                                MultilingualProbes(s, importedFont, fontIdentity, environment, previewContext, previewDensity, workspace)
                            }
                            LabTab.Report -> workspace(null) {
                                Text("${measurements.size}/2 render measurements captured. Open Render or Effects after changing settings.")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Compose Text Lab report", report()))
                                        status = "JSON copied"
                                    }) { Text("Copy JSON") }
                                    OutlinedButton(onClick = {
                                        exportJson = report()
                                        saveReport.launch("compose-text-lab-${System.currentTimeMillis()}.json")
                                    }) { Text("Save JSON") }
                                }
                                Text("Includes shared sample text. Remote toolbar routing and pixel equivalence are unverified.",
                                    style = MaterialTheme.typography.bodySmall)
                                Text(report(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Lint 8.13 treats this argument as a fixed list, but SDK_INT_FULL is the documented runtime value.
@android.annotation.SuppressLint("WrongConstant")
private fun androidSdkVersion(): String {
    val major = Build.VERSION.SDK_INT
    if (major < Build.VERSION_CODES.BAKLAVA) return major.toString()
    return "$major.${Build.getMinorSdkVersion(Build.VERSION.SDK_INT_FULL)}"
}
