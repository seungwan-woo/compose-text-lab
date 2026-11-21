package io.github.seungwanwoo.composetextlab

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

private enum class Mark { Plain, Emphasis, Highlight, Underline, Url, Action, Badge }
private data class RichRun(val text: String, val mark: Mark = Mark.Plain)

private const val DOCS_URL = "https://developer.android.com"
private const val BADGE_KEY = "badge"
private const val DETAILS = "details"
private val sample = listOf(
    RichRun("Hello "), RichRun("Compose", Mark.Emphasis), RichRun(" and "),
    RichRun("Android docs", Mark.Url), RichRun("\n"),
    RichRun("한글", Mark.Highlight), RichRun(" · "), RichRun("العربية", Mark.Underline),
    RichRun(" · 🙂\n"), RichRun("Details", Mark.Action), RichRun(" · "),
    RichRun("[new]", Mark.Badge), RichRun(" New message"),
)

@Composable
internal fun RichTextProbes(settings: LabSettings, font: FontAsset?) {
    val colors = MaterialTheme.colorScheme
    val style = probeStyle(settings.copy(effect = TextEffect.None), font)
    val paddingPx = with(LocalDensity.current) { settings.paddingDp.dp.roundToPx() }
    var composeAction by rememberSaveable { mutableStateOf("No link activated") }
    var viewAction by rememberSaveable { mutableStateOf("No link activated") }
    var composeMetrics by remember { mutableStateOf("") }
    var viewMetrics by remember { mutableStateOf("") }
    val annotated = remember(colors) { buildComposeText(colors) { composeAction = it } }
    val inlineBadge = mapOf(BADGE_KEY to InlineTextContent(
        Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize(.7f).background(colors.primary, CircleShape))
        }
    })

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tap Android docs or Details to compare link dispatch. The URL target is shown here.")
        ProbePair(settings, compose = {
            ProbePanel("Compose · AnnotatedString", Modifier.fillMaxWidth()) {
                Text(annotated, style = style, inlineContent = inlineBadge,
                    modifier = Modifier.fillMaxWidth().padding(settings.paddingDp.dp).testTag("rich-compose"),
                    onTextLayout = { layout ->
                        composeMetrics = "First baseline: ${(layout.firstBaseline + paddingPx).roundToInt()} px · " +
                            "lines: ${layout.lineCount} · box: ${layout.size.height + 2 * paddingPx} px"
                    })
                Text(composeMetrics, style = MaterialTheme.typography.bodySmall)
                Text("Compose · $composeAction", style = MaterialTheme.typography.bodySmall)
            }
        }, view = {
            ProbePanel("View · Spannable", Modifier.fillMaxWidth()) {
                AndroidView(factory = { context ->
                    TextView(context).apply {
                        tag = "rich-view"
                        text = buildViewText(colors) { viewAction = it }
                        movementMethod = LinkMovementMethod.getInstance()
                        typeface = weightedTypeface(settings, font)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
                        setTextColor(colors.onSurface.toArgb())
                        includeFontPadding = settings.includeFontPadding
                        letterSpacing = settings.letterSpacingEm
                        applyProbeParagraphStyle(settings.paragraphDirection)
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        if (settings.lineHeightSp > 0) {
                            val lineHeight = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP, settings.lineHeightSp, resources.displayMetrics,
                            )
                            val metrics = paint.fontMetrics
                            setLineSpacing(lineHeight - (metrics.descent - metrics.ascent), 1f)
                        }
                        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                            viewMetrics = "First baseline: $baseline px · lines: $lineCount · box: $height px"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth())
                Text(viewMetrics, style = MaterialTheme.typography.bodySmall)
                Text("TextView · $viewAction", style = MaterialTheme.typography.bodySmall)
            }
        })
    }
}

private fun buildComposeText(colors: ColorScheme, onAction: (String) -> Unit) = buildAnnotatedString {
    val linkStyle = TextLinkStyles(style = SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline))
    for ((text, mark) in sample) {
        when (mark) {
            Mark.Plain -> append(text)
            Mark.Emphasis -> withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) { append(text) }
            Mark.Highlight -> withStyle(SpanStyle(color = colors.onSecondaryContainer, background = colors.secondaryContainer)) { append(text) }
            Mark.Underline -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(text) }
            Mark.Url -> withLink(LinkAnnotation.Url(DOCS_URL, linkStyle) { onAction("URL: $DOCS_URL") }) { append(text) }
            Mark.Action -> withLink(LinkAnnotation.Clickable(DETAILS, linkStyle) { onAction("Action: $DETAILS") }) { append(text) }
            Mark.Badge -> appendInlineContent(BADGE_KEY, text)
        }
    }
}

private fun buildViewText(colors: ColorScheme, onAction: (String) -> Unit) = SpannableStringBuilder().apply {
    val accent = colors.primary.toArgb()
    for ((text, mark) in sample) {
        val start = length
        append(text)
        fun add(span: Any) = setSpan(span, start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        when (mark) {
            Mark.Plain -> Unit
            Mark.Emphasis -> {
                add(StyleSpan(Typeface.BOLD))
                add(ForegroundColorSpan(accent))
            }
            Mark.Highlight -> {
                add(BackgroundColorSpan(colors.secondaryContainer.toArgb()))
                add(ForegroundColorSpan(colors.onSecondaryContainer.toArgb()))
            }
            Mark.Underline -> add(UnderlineSpan())
            Mark.Url -> add(object : URLSpan(DOCS_URL) {
                override fun onClick(widget: View) = onAction("URL: $url")
                override fun updateDrawState(paint: TextPaint) = styleLink(paint, accent)
            })
            Mark.Action -> add(object : ClickableSpan() {
                override fun onClick(widget: View) = onAction("Action: $DETAILS")
                override fun updateDrawState(paint: TextPaint) = styleLink(paint, accent)
            })
            Mark.Badge -> add(BadgeSpan(accent))
        }
    }
}

private fun styleLink(paint: TextPaint, color: Int) {
    paint.color = color
    paint.isUnderlineText = true
}

private class BadgeSpan(color: Int) : ReplacementSpan() {
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        paint.textSize.roundToInt()

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float,
        top: Int, y: Int, bottom: Int, paint: Paint) {
        val centerY = y + (paint.ascent() + paint.descent()) / 2f
        canvas.drawCircle(x + paint.textSize / 2f, centerY, paint.textSize * .35f, badgePaint)
    }
}
