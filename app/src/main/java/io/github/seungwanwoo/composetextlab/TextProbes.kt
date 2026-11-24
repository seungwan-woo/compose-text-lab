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
import androidx.compose.foundation.MarqueeSpacing
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
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
internal fun probeStyle(s: LabSettings, font: FontAsset?, linePolicy: LineLayoutPolicy? = null): TextStyle {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface
    val effects = s.effectSettings
    val shadowOffset = with(density) { effects.shadowOffsetDp.dp.toPx() }
    // A new TypefaceFontFamily on every onTextLayout recomposition would trigger layout again.
    return remember(s, font?.sha256, density, color, linePolicy) { TextStyle(
        color = color,
        fontSize = s.fontSizeSp.sp, fontFamily = composeFamily(s, font), fontWeight = FontWeight(s.weight),
        letterSpacing = s.letterSpacingEm.em,
        lineHeight = if (s.lineHeightSp == 0f) androidx.compose.ui.unit.TextUnit.Unspecified else s.lineHeightSp.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = s.includeFontPadding),
        lineBreak = linePolicy?.composeLineBreak ?: LineBreak.Simple,
        hyphens = when (linePolicy?.hyphenate) {
            true -> Hyphens.Auto
            false -> Hyphens.None
            null -> Hyphens.Unspecified
        },
        localeList = linePolicy?.let { LocaleList(it.localeTag) },
        textAlign = TextAlign.Start,
        textDirection = linePolicy?.direction?.compose ?: when (s.paragraphDirection) {
            ParagraphDirection.FirstStrong -> TextDirection.Content
            ParagraphDirection.LTR -> TextDirection.Ltr
            ParagraphDirection.RTL -> TextDirection.Rtl
        },
        shadow = if (s.effect == TextEffect.Shadow) Shadow(Color.Gray,
            Offset(shadowOffset, shadowOffset), with(density) { effects.shadowRadiusDp.dp.toPx() }) else null,
        drawStyle = when (s.effect) {
            TextEffect.Stroke -> Stroke(with(density) { effects.strokeWidthDp.dp.toPx() })
            TextEffect.FillStroke -> Fill
            else -> null
        },
        textMotion = if (s.effect == TextEffect.Motion) {
            if (effects.animatedTextMotion) TextMotion.Animated else TextMotion.Static
        } else null,
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
internal fun ComparisonProbes(
    s: LabSettings,
    font: FontAsset?,
    linePolicy: LineLayoutPolicy? = null,
    graphemes: List<Int> = emptyList(),
    playback: EffectPlayback? = null,
    onMeasure: (ProbeMeasurement) -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.primary
    val compose: @Composable () -> Unit = {
        ProbePanel("Compose Text", Modifier.fillMaxWidth()
            .testTag("compose-render-panel")) {
            ComposeProbe(s, font, linePolicy, graphemes, playback, onMeasure)
        }
    }
    val view: @Composable () -> Unit = {
        ProbePanel("Android TextView", Modifier.fillMaxWidth()) {
            AndroidView(factory = { context ->
                MeasuredTextView(context).apply {
                    tag = "view-probe"
                    configure(s, font, color.toArgb(), linePolicy, graphemes, playback, outline.toArgb())
                    measurement = onMeasure
                }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
    ProbePair(s, compose, view)
}

@Composable
@OptIn(ExperimentalTextApi::class)
private fun ComposeProbe(
    s: LabSettings,
    font: FontAsset?,
    linePolicy: LineLayoutPolicy?,
    graphemes: List<Int>,
    playback: EffectPlayback?,
    onMeasure: (ProbeMeasurement) -> Unit,
) {
    var result by remember { mutableStateOf<TextLayoutResult?>(null) }
    var widgetSize by remember { mutableStateOf(IntSize.Zero) }
    val padding = with(LocalDensity.current) { s.paddingDp.dp.roundToPx() }
    val baseStyle = probeStyle(s, font, linePolicy)
    val style = if (s.effect == TextEffect.VariableFont && playback != null) {
        val axes = VariableAxes.at(playback.fontStep)
        val family = remember(axes) {
            FontFamily(Font(R.font.lab_roboto_variable, variationSettings = FontVariation.Settings(
                FontVariation.weight(axes.weight), FontVariation.width(axes.width),
            )))
        }
        baseStyle.copy(fontFamily = family, fontWeight = FontWeight.Normal)
    } else baseStyle
    val work = playback?.compose
    if (work != null) SideEffect { work.composition++ }
    val effects = s.effectSettings
    val outline = MaterialTheme.colorScheme.primary
    val marquee = s.effect == TextEffect.Marquee
    LaunchedEffect(result, widgetSize) {
        val layout = result
        if (layout != null && widgetSize.width > 0 && widgetSize.height > 0) {
            onMeasure(ProbeMeasurement("compose", widgetSize.width, widgetSize.height,
                layout.size.width, layout.size.height, layout.lineCount,
                layout.firstBaseline + padding, layout.lastBaseline + padding,
                (0 until layout.lineCount).any { layout.isLineEllipsized(it) },
                layout.hasVisualOverflow || layout.size.width + padding * 2 > widgetSize.width,
                if (linePolicy != null) layout.observeLines(padding) else emptyList(),
                if (linePolicy != null) layout.observeCarets(graphemes, padding) else emptyList()))
        }
    }
    Text(s.renderText(), style = style, maxLines = if (marquee) 1 else s.maxLines,
        overflow = if (s.ellipsis && !marquee) TextOverflow.Ellipsis else TextOverflow.Clip,
        softWrap = !marquee,
        modifier = Modifier.fillMaxWidth().onSizeChanged { widgetSize = it }
            .then(if (marquee) Modifier.basicMarquee(
                iterations = effects.marqueeIterations.takeIf { it > 0 } ?: Int.MAX_VALUE,
                velocity = effects.marqueeVelocity.dp,
                initialDelayMillis = effects.marqueeDelayMs,
                spacing = if (effects.marqueeGapOverride) MarqueeSpacing(effects.marqueeGapDp.dp)
                    else MarqueeSpacing.fractionOfContainer(1f / 3f),
            ) else Modifier)
            .observeEffectWork(playback)
            .padding(s.paddingDp.dp).testTag("compose-probe").drawWithContent {
                if (s.effect == TextEffect.FillStroke) result?.let {
                    drawText(it, color = outline, drawStyle = Stroke(effects.strokeWidthDp.dp.toPx()))
                }
                drawContent()
                if (s.showBaselines) result?.let { layout ->
                    for (line in 0 until layout.lineCount) drawLine(Color(0xFFDA4453),
                        Offset(0f, layout.getLineBaseline(line)), Offset(size.width, layout.getLineBaseline(line)), 1.dp.toPx())
                }
            },
        onTextLayout = { layout ->
            work?.let {
                it.textLayouts++
                it.firstLineWidth = layout.getLineRight(0) - layout.getLineLeft(0)
            }
            result = layout
        })
}

private fun Modifier.observeEffectWork(playback: EffectPlayback?): Modifier {
    if (playback == null) return this
    val work = playback.compose
    val observed = layout { measurable, constraints ->
        work.measure++
        val child = measurable.measure(constraints)
        layout(child.width, child.height) { child.place(0, 0) }
    }
    val layer = if (playback.effect == TextEffect.Motion && playback.options.motionPath == MotionPath.Layer) {
        Modifier.graphicsLayer { translationX = playback.offsetPx }
    } else Modifier
    return observed.then(layer).drawWithContent {
        work.draw++
        if (playback.effect == TextEffect.Motion && playback.options.motionPath == MotionPath.Canvas) {
            val content = this
            translate(left = playback.offsetPx) { content.drawContent() }
        } else {
            drawContent()
        }
    }
}

internal class MeasuredTextView(context: Context) : TextView(context) {
    var measurement: ((ProbeMeasurement) -> Unit)? = null
    private var settings = LabSettings()
    private val baselinePaint = Paint().apply { color = 0xFFDA4453.toInt() }
    private var lastMeasurement: ProbeMeasurement? = null
    private var linePolicy: LineLayoutPolicy? = null
    private var graphemes: List<Int> = emptyList()
    private var playback: EffectPlayback? = null
    private var outlineColor: Int = 0
    private var drawOffsetPx: Float = 0f
    private var observedLayout: android.text.Layout? = null

    fun configure(s: LabSettings, font: FontAsset?, color: Int, linePolicy: LineLayoutPolicy?,
        graphemes: List<Int>, playback: EffectPlayback?, outlineColor: Int) {
        settings = s
        this.linePolicy = linePolicy
        this.graphemes = graphemes
        this.playback = playback
        this.outlineColor = outlineColor
        text = s.renderText()
        typeface = if (s.effect == TextEffect.VariableFont) resources.getFont(R.font.lab_roboto_variable)
            else weightedTypeface(s, font)
        if (s.effect == TextEffect.VariableFont) setVariableFont(VariableAxes.at(playback?.fontStep ?: 0))
        if (s.effect == TextEffect.Motion) {
            paint.isSubpixelText = s.effectSettings.animatedTextMotion
            paint.isLinearText = s.effectSettings.animatedTextMotion
            paint.hinting = if (s.effectSettings.animatedTextMotion) Paint.HINTING_OFF else Paint.HINTING_ON
        }
        setTextSize(TypedValue.COMPLEX_UNIT_SP, s.fontSizeSp)
        setTextColor(color)
        includeFontPadding = s.includeFontPadding
        letterSpacing = s.letterSpacingEm
        gravity = Gravity.TOP or Gravity.START
        applyProbeParagraphStyle(s.paragraphDirection)
        linePolicy?.applyTo(this)
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
            marqueeRepeatLimit = s.effectSettings.marqueeIterations.takeIf { it > 0 } ?: -1
            isSelected = true
        }
        if (s.effect == TextEffect.Shadow) {
            val d = resources.displayMetrics.density
            val offset = s.effectSettings.shadowOffsetDp * d
            // Android Paint disables the shadow at zero. Compose keeps an effectively sharp shadow.
            val radius = (s.effectSettings.shadowRadiusDp * d).coerceAtLeast(Float.MIN_VALUE)
            setShadowLayer(radius, offset, offset, android.graphics.Color.GRAY)
            if (Build.VERSION.SDK_INT < 28) setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
        if (s.effect == TextEffect.Stroke) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s.effectSettings.strokeWidthDp * resources.displayMetrics.density
        }
    }

    fun setVariableFont(axes: VariableAxes) {
        setFontVariationSettings(axes.asViewSettings())
    }

    fun setMotionOffset(offset: Float, path: MotionPath) {
        if (path == MotionPath.Layer) {
            translationX = offset
        } else {
            drawOffsetPx = offset
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        playback?.let {
            it.view = this
            if (it.effect == TextEffect.Motion) setMotionOffset(it.offsetPx, it.options.motionPath)
        }
    }

    override fun onDetachedFromWindow() {
        playback?.let { if (it.view === this) it.view = null }
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        playback?.textView?.let { it.measure++ }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val layout = layout ?: return
        if (settings.effect == TextEffect.Gradient) paint.shader = LinearGradient(0f, 0f,
            maxOf(1, layout.width).toFloat(), 0f,
            intArrayOf(0xFF2364E8.toInt(), 0xFFE23F86.toInt()), null, Shader.TileMode.CLAMP)
        val count = layout.lineCount
        if (count == 0) return
        observeTextLayout()
        val visibleLines = minOf(count, maxLines)
        val ellipsized = (0 until count).any { layout.getEllipsisCount(it) > 0 }
        val overflow = ellipsized || count > maxLines || layout.height > height - totalPaddingTop - totalPaddingBottom ||
            (0 until count).any { layout.getLineWidth(it) > width - compoundPaddingLeft - compoundPaddingRight + 1 }
        val measured = ProbeMeasurement("textView", width, height, layout.width, layout.height, count,
            baseline.toFloat(), (totalPaddingTop + layout.getLineBaseline(visibleLines - 1)).toFloat(), ellipsized, overflow,
            if (linePolicy != null) layout.observeLines(visibleLines, totalPaddingTop, compoundPaddingLeft) else emptyList(),
            if (linePolicy != null) layout.observeCarets(graphemes, visibleLines, compoundPaddingLeft) else emptyList())
        if (measured != lastMeasurement) {
            lastMeasurement = measured
            post { if (isAttachedToWindow) measurement?.invoke(measured) }
        }
    }

    override fun onDraw(canvas: Canvas) {
        playback?.textView?.let { it.draw++ }
        observeTextLayout()
        val saved = canvas.save()
        canvas.translate(drawOffsetPx, 0f)
        if (settings.effect == TextEffect.FillStroke) drawOutline(canvas)
        super.onDraw(canvas)
        if (settings.showBaselines) layout?.let { layout ->
            baselinePaint.strokeWidth = resources.displayMetrics.density
            for (line in 0 until minOf(layout.lineCount, maxLines)) {
                val y = (totalPaddingTop + layout.getLineBaseline(line)).toFloat()
                canvas.drawLine(0f, y, width.toFloat(), y, baselinePaint)
            }
        }
        canvas.restoreToCount(saved)
    }

    private fun drawOutline(canvas: Canvas) {
        val textLayout = layout ?: return
        val saved = canvas.save()
        val fillColor = paint.color
        val fillStyle = paint.style
        val strokeWidth = paint.strokeWidth
        try {
            canvas.clipRect(0, 0, width, height)
            canvas.translate(compoundPaddingLeft.toFloat(), totalPaddingTop.toFloat())
            paint.color = outlineColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = settings.effectSettings.strokeWidthDp * resources.displayMetrics.density
            textLayout.draw(canvas)
        } finally {
            paint.color = fillColor
            paint.style = fillStyle
            paint.strokeWidth = strokeWidth
            canvas.restoreToCount(saved)
        }
    }

    private fun observeTextLayout() {
        val work = playback?.textView ?: return
        val textLayout = layout ?: return
        if (textLayout === observedLayout || textLayout.lineCount == 0) return
        observedLayout = textLayout
        work.textLayouts++
        work.firstLineWidth = textLayout.getLineRight(0) - textLayout.getLineLeft(0)
    }
}
