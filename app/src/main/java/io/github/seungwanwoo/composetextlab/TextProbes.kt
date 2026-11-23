package io.github.seungwanwoo.composetextlab

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

fun baseTypeface(s: LabSettings, imported: FontAsset?): Typeface = when (s.font) {
    FontChoice.System -> Typeface.DEFAULT
    FontChoice.Sans -> Typeface.SANS_SERIF
    FontChoice.Serif -> Typeface.SERIF
    FontChoice.Monospace -> Typeface.MONOSPACE
    FontChoice.Imported -> imported?.typeface ?: Typeface.DEFAULT
}

fun weightedTypeface(s: LabSettings, imported: FontAsset?): Typeface =
    if (Build.VERSION.SDK_INT >= 28) Typeface.create(baseTypeface(s, imported), s.weight, false)
    else Typeface.create(baseTypeface(s, imported), if (s.weight >= 600) Typeface.BOLD else Typeface.NORMAL)

// FontFamily(Typeface) uses the supplied face as-is. Resolve weight once for both engines,
// including API 26/27's NORMAL/BOLD fallback, rather than relying on wrapper synthesis.
fun composeFamily(s: LabSettings, imported: FontAsset?): FontFamily = FontFamily(weightedTypeface(s, imported))

@Composable
fun probeStyle(s: LabSettings, font: FontAsset?): TextStyle {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface
    // A new TypefaceFontFamily on every onTextLayout recomposition would trigger layout again.
    return remember(s, font?.sha256, density, color) { TextStyle(
        color = color,
        fontSize = s.fontSizeSp.sp, fontFamily = composeFamily(s, font), fontWeight = FontWeight(s.weight),
        letterSpacing = s.letterSpacingEm.em,
        lineHeight = if (s.lineHeightSp == 0f) androidx.compose.ui.unit.TextUnit.Unspecified else s.lineHeightSp.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = s.includeFontPadding),
        lineBreak = LineBreak.Simple,
        textAlign = TextAlign.Start,
        textDirection = when (s.paragraphDirection) {
            ParagraphDirection.FirstStrong -> TextDirection.Content
            ParagraphDirection.LTR -> TextDirection.Ltr
            ParagraphDirection.RTL -> TextDirection.Rtl
        },
        shadow = if (s.effect == TextEffect.Shadow) Shadow(Color.Gray,
            with(density) { Offset(2.dp.toPx(), 2.dp.toPx()) }, with(density) { 3.dp.toPx() }) else null,
        drawStyle = if (s.effect == TextEffect.Stroke) Stroke(with(density) { 1.dp.toPx() }) else null,
    ).let { if (s.effect == TextEffect.Gradient) it.copy(brush = Brush.horizontalGradient(listOf(Color(0xFF2364E8), Color(0xFFE23F86)))) else it } }
}

internal fun TextView.applyProbeParagraphStyle(direction: ParagraphDirection) {
    textDirection = when (direction) {
        ParagraphDirection.FirstStrong -> View.TEXT_DIRECTION_FIRST_STRONG
        ParagraphDirection.LTR -> View.TEXT_DIRECTION_LTR
        ParagraphDirection.RTL -> View.TEXT_DIRECTION_RTL
    }
    textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    // These TextView setters are available since API 23.
    breakStrategy = android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE
    hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NONE
}

@Composable
internal fun ProbePair(s: LabSettings, compose: @Composable () -> Unit, view: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val paired = when (s.panelLayout) {
            PanelLayout.Auto -> maxWidth >= 680.dp
            PanelLayout.Stacked -> false
            PanelLayout.SideBySide -> true
        }
        if (paired) {
            val direction = LocalLayoutDirection.current
            val rowWidth = s.previewWidthDp?.let { (it * 2 + 8).dp } ?: maxWidth
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.widthIn(max = rowWidth).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        Box(Modifier.weight(1f)) { compose() }
                        Box(Modifier.weight(1f)) { view() }
                    }
                }
            }
        } else {
            Column(Modifier.widthIn(max = (s.previewWidthDp ?: 760f).dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) { compose(); view() }
        }
    }
}

@Composable
fun ProbePanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
fun ComparisonProbes(s: LabSettings, font: FontAsset?, onMeasure: (ProbeMeasurement) -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface
    val compose: @Composable () -> Unit = {
        ProbePanel("Compose Text", Modifier.fillMaxWidth()
            .testTag("compose-render-panel")) {
            ComposeProbe(s, font, onMeasure)
        }
    }
    val view: @Composable () -> Unit = {
        ProbePanel("Android TextView", Modifier.fillMaxWidth()) {
            AndroidView(factory = { context ->
                MeasuredTextView(context).apply {
                    tag = "view-probe"
                    configure(s, font, color.toArgb())
                    measurement = onMeasure
                }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
    ProbePair(s, compose, view)
}

@Composable
private fun ComposeProbe(s: LabSettings, font: FontAsset?, onMeasure: (ProbeMeasurement) -> Unit) {
    var result by remember { mutableStateOf<TextLayoutResult?>(null) }
    var widgetSize by remember { mutableStateOf(IntSize.Zero) }
    val padding = with(LocalDensity.current) { s.paddingDp.dp.roundToPx() }
    val style = probeStyle(s, font)
    val marquee = s.effect == TextEffect.Marquee
    LaunchedEffect(result, widgetSize) {
        val layout = result
        if (layout != null && widgetSize.width > 0 && widgetSize.height > 0) {
            onMeasure(ProbeMeasurement("compose", widgetSize.width, widgetSize.height,
                layout.size.width, layout.size.height, layout.lineCount,
                layout.firstBaseline + padding, layout.lastBaseline + padding,
                (0 until layout.lineCount).any { layout.isLineEllipsized(it) },
                layout.hasVisualOverflow || layout.size.width + padding * 2 > widgetSize.width))
        }
    }
    Text(s.renderText(), style = style, maxLines = if (marquee) 1 else s.maxLines,
        overflow = if (s.ellipsis && !marquee) TextOverflow.Ellipsis else TextOverflow.Clip,
        softWrap = !marquee,
        modifier = Modifier.fillMaxWidth().onSizeChanged { widgetSize = it }
            .then(if (marquee) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier)
            .padding(s.paddingDp.dp).testTag("compose-probe").drawWithContent {
                drawContent()
                if (s.showBaselines) result?.let { layout ->
                    for (line in 0 until layout.lineCount) drawLine(Color(0xFFDA4453),
                        Offset(0f, layout.getLineBaseline(line)), Offset(size.width, layout.getLineBaseline(line)), 1.dp.toPx())
                }
            },
        onTextLayout = { layout ->
            result = layout
        })
}

class MeasuredTextView(context: Context) : TextView(context) {
    var measurement: ((ProbeMeasurement) -> Unit)? = null
    private var settings = LabSettings()
    private val baselinePaint = Paint().apply { color = 0xFFDA4453.toInt() }
    private var lastMeasurement: ProbeMeasurement? = null

    fun configure(s: LabSettings, font: FontAsset?, color: Int) {
        settings = s
        text = s.renderText()
        typeface = weightedTypeface(s, font)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, s.fontSizeSp)
        setTextColor(color)
        includeFontPadding = s.includeFontPadding
        letterSpacing = s.letterSpacingEm
        gravity = Gravity.TOP or Gravity.START
        applyProbeParagraphStyle(s.paragraphDirection)
        val padding = (s.paddingDp * resources.displayMetrics.density).roundToInt()
        setPadding(padding, padding, padding, padding)
        if (s.lineHeightSp > 0) {
            val target = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, s.lineHeightSp, resources.displayMetrics)
            setLineSpacing(target - (paint.fontMetrics.descent - paint.fontMetrics.ascent), 1f)
        }
        maxLines = s.maxLines
        ellipsize = if (s.ellipsis) TextUtils.TruncateAt.END else null
        if (s.effect == TextEffect.Marquee) {
            setSingleLine(true); ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1; isSelected = true
        }
        if (s.effect == TextEffect.Shadow) {
            val d = resources.displayMetrics.density
            setShadowLayer(3 * d, 2 * d, 2 * d, android.graphics.Color.GRAY)
            if (Build.VERSION.SDK_INT < 28) setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
        if (s.effect == TextEffect.Stroke) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = resources.displayMetrics.density
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val layout = layout ?: return
        if (settings.effect == TextEffect.Gradient) paint.shader = LinearGradient(0f, 0f,
            maxOf(1, width - paddingLeft - paddingRight).toFloat(), 0f,
            intArrayOf(0xFF2364E8.toInt(), 0xFFE23F86.toInt()), null, Shader.TileMode.CLAMP)
        val count = layout.lineCount
        if (count == 0) return
        val visibleLines = minOf(count, maxLines)
        val ellipsized = (0 until count).any { layout.getEllipsisCount(it) > 0 }
        val overflow = ellipsized || count > maxLines || layout.height > height - totalPaddingTop - totalPaddingBottom ||
            (0 until count).any { layout.getLineWidth(it) > width - compoundPaddingLeft - compoundPaddingRight + 1 }
        val measured = ProbeMeasurement("textView", width, height, layout.width, layout.height, count,
            baseline.toFloat(), (totalPaddingTop + layout.getLineBaseline(visibleLines - 1)).toFloat(), ellipsized, overflow)
        if (measured != lastMeasurement) {
            lastMeasurement = measured
            post { if (isAttachedToWindow) measurement?.invoke(measured) }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (settings.showBaselines) layout?.let { layout ->
            baselinePaint.strokeWidth = resources.displayMetrics.density
            for (line in 0 until minOf(layout.lineCount, maxLines)) {
                val y = (totalPaddingTop + layout.getLineBaseline(line)).toFloat()
                canvas.drawLine(0f, y, width.toFloat(), y, baselinePaint)
            }
        }
    }
}
