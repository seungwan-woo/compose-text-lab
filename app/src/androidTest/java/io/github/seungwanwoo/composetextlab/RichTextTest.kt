package io.github.seungwanwoo.composetextlab

import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.text.style.URLSpan
import android.widget.TextView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RichTextTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun compareStylesAndActivateBothTypesOfLinks() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        rule.onNodeWithTag("system-scale").performScrollTo().performClick()
        rule.onNodeWithTag("preview-scale").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.6f) }
        rule.onNodeWithText("Hide settings").performClick()
        rule.onNodeWithText("Rich text", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("side-by-side").performClick().assertIsOn()

        val compose = rule.onNodeWithTag("rich-compose").performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        assertTrue(layout.layoutInput.text.spanStyles.isNotEmpty())
        assertEquals(2, layout.layoutInput.text.getLinkAnnotations(0, text.length).size)
        assertNotNull(layout.placeholderRects.single())
        rule.runOnIdle {
            val boldSpan = layout.layoutInput.text.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
            val boldFace = createFontFamilyResolver(rule.activity)
                .resolve(boldSpan.item.fontFamily, FontWeight.Bold).value as Typeface
            assertTrue(boldFace.isBold)
            val view = rule.activity.window.decorView.findViewWithTag<TextView>("rich-view")
            assertEquals(text, view.text.toString())
            val styled = view.text as Spanned
            assertEquals(1, styled.getSpans(0, styled.length, URLSpan::class.java).size)
            assertEquals(1, styled.getSpans(0, styled.length, ReplacementSpan::class.java).size)
            assertEquals(1.6f, view.resources.configuration.fontScale, 0.01f)
            assertTrue(view.baseline > 0)
        }

        for ((label, event) in listOf("Android docs" to "URL: https://developer.android.com", "Details" to "Action: details")) {
            val target = layout.getBoundingBox(text.indexOf(label) + 1).center
            compose.performTouchInput { click(target) }
            rule.onNodeWithText("Compose · $event").assertIsDisplayed()
            onView(withTagValue(equalTo<Any>("rich-view"))).perform(
                GeneralClickAction(Tap.SINGLE, CoordinatesProvider { source ->
                    val view = source as TextView
                    val location = IntArray(2).also(view::getLocationOnScreen)
                    val index = view.text.indexOf(label) + 1
                    val line = view.layout.getLineForOffset(index)
                    floatArrayOf(
                        location[0] + view.totalPaddingLeft +
                            (view.layout.getPrimaryHorizontal(index) + view.layout.getPrimaryHorizontal(index + 1)) / 2 - view.scrollX,
                        location[1] + view.totalPaddingTop +
                            (view.layout.getLineTop(line) + view.layout.getLineBottom(line)) / 2f - view.scrollY,
                    )
                }, Press.FINGER),
            )
            rule.onNodeWithText("TextView · $event").assertIsDisplayed()
        }

        val arguments = InstrumentationRegistry.getArguments()
        val output = arguments.getString("additionalTestOutputDir") ?: return
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(output, "rich-text-comparison.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
