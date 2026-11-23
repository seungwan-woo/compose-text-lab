package io.github.seungwanwoo.composetextlab

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.abs
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class ResultCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun prepareSideBySide(sample: String? = null) {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        rule.onNodeWithText("Theme").performScrollTo()
        rule.onNodeWithText("Light").performClick().assertIsSelected()
        if (sample != null) {
            rule.onNodeWithTag("sample-input").performScrollTo().performTextReplacement(sample)
        }
        rule.onNodeWithTag("limit-panel-width").performScrollTo().performClick().assertIsOn()
        rule.onNodeWithTag("panel-width").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(420f) }
        rule.onNodeWithText("Hide settings").performClick()
        rule.onNodeWithText("Side by side").performClick().assertIsSelected()
        selectTab("Effects")
        selectTab("Render")
    }

    @Test fun wideTabletKeepsSettingsAndBothSelectionPanelsVisible() {
        assumeTrue(rule.activity.resources.configuration.screenWidthDp >= 1100)
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        selectTab("Selection")
        rule.onNodeWithText("Auto").assertIsSelected()
        val settings = rule.onNodeWithTag("settings-pane").fetchSemanticsNode().boundsInRoot
        val compose = rule.onNodeWithTag("compose-selectable-panel").fetchSemanticsNode().boundsInRoot
        val view = rule.onNodeWithTag("view-selectable-panel").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("compose-selectable-panel").assertIsDisplayed()
        rule.onNodeWithTag("view-selectable-panel").assertIsDisplayed()
        assertTrue("Settings and both comparison panels must be side by side",
            settings.right < compose.left && compose.right < view.left && compose.top == view.top)
        assertTrue("Comparison panels should share the remaining width", abs(compose.width - view.width) <= 1f)
        capture("tablet-selection")
    }

    @Test fun captureComparisonScreens() {
        prepareSideBySide()
        rule.onNodeWithTag("compose-probe").assertIsDisplayed()
        capture("render")

        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Theme").performScrollTo()
        rule.onNodeWithText("Dark").performClick().assertIsSelected()
        rule.onNodeWithTag("system-scale").performScrollTo().performClick()
        rule.onNodeWithTag("preview-scale").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.6f) }
        rule.onNodeWithText("Hide settings").performClick()
        selectTab("Effects")
        selectTab("Render")
        rule.onNodeWithTag("compose-probe").assertIsDisplayed()
        capture("dark-font-scale")

        selectTab("Effects")
        rule.onNodeWithText("Gradient").performClick().assertIsSelected()
        rule.onNodeWithTag("compose-probe").assertIsDisplayed()
        capture("gradient")

        withInteractiveWindows {
            selectTab("Selection")
            val compose = rule.onNodeWithTag("compose-selectable-panel").fetchSemanticsNode().boundsInRoot
            val view = rule.onNodeWithTag("view-selectable-panel").fetchSemanticsNode().boundsInRoot
            assertTrue("Compose and TextView must have equal widths on the same row",
                compose.right < view.left && compose.top == view.top && abs(compose.width - view.width) <= 1f)
            rule.onNodeWithTag("compose-selectable").assertIsDisplayed().performTouchInput {
                longClick(Offset(width * 0.15f, height * 0.15f))
            }
            captureToolbar("compose-selection")

            selectTab("Render")
            selectTab("Selection")
            rule.onNodeWithTag("view-selectable-panel").performScrollTo()
            onView(withTagValue(equalTo<Any>("view-selectable"))).perform(
                GeneralClickAction(Tap.LONG, CoordinatesProvider { view ->
                    val location = IntArray(2).also(view::getLocationOnScreen)
                    val text = view as android.widget.TextView
                    floatArrayOf(
                        location[0] + text.totalPaddingLeft + text.paint.measureText("Com") / 2,
                        location[1] + text.totalPaddingTop + text.layout.getLineBottom(0) / 2f,
                    )
                }, Press.FINGER),
            )
            captureToolbar("view-selection")
        }
    }

    @Test fun mouseSecondaryClickOpensBothContextMenus() {
        prepareSideBySide()
        withInteractiveWindows {
            for ((tag, screenshot) in listOf(
                "compose-selectable" to "compose-right-click",
                "view-selectable-panel" to "view-right-click",
            )) {
                selectTab("Selection")
                assertFalse("Menu should be closed before right click", menuHas("Select all"))
                rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performMouseInput {
                    click(position = Offset(width * 0.15f, height * 0.65f), button = MouseButton.Secondary)
                }
                rule.waitUntil(timeoutMillis = 5_000) { menuHas("Select all") }
                capture(screenshot)
                androidx.test.espresso.Espresso.pressBack()
                rule.waitUntil(timeoutMillis = 5_000) { !menuHas("Select all") }
                selectTab("Render")
            }
        }
    }

    @Test fun overflowAndMarqueeInBothEngines() {
        prepareSideBySide("Short sample")
        assertOverflow(expectedOverflow = false, expectedEllipsis = false)

        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("sample-input").performScrollTo().performTextReplacement(
            "01 Alpha, 02 Bravo, 03 Charlie, 04 Delta, 05 Echo, 06 Foxtrot, 07 Golf, 08 Hotel."
        )
        rule.onNodeWithTag("Max lines").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        rule.onNodeWithText("Hide settings").performClick()
        selectTab("Effects")
        selectTab("Render")
        assertOverflow(expectedOverflow = true, expectedEllipsis = true)
        capture("overflow-ellipsis")

        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("End ellipsis").performScrollTo().performClick().assertIsOff()
        rule.onNodeWithText("Hide settings").performClick()
        selectTab("Effects")
        selectTab("Render")
        assertOverflow(expectedOverflow = true, expectedEllipsis = false)
        capture("overflow-clip")

        selectTab("Effects")
        val marquee = rule.onNodeWithText("Marquee").assertIsDisplayed()
        // Compose cancels infinite animations while the test clock advances automatically.
        rule.mainClock.autoAdvance = false
        try {
            marquee.performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
            rule.mainClock.advanceTimeBy(100)
            marquee.assertIsSelected()
            assertMarqueeMotion()
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    private fun assertOverflow(expectedOverflow: Boolean, expectedEllipsis: Boolean) {
        val result = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("compose-probe").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(result))
        }
        val compose = result.single()
        assertEquals("Compose overflow", expectedOverflow, compose.hasVisualOverflow)
        assertEquals("Compose ellipsis", expectedEllipsis, compose.isLineEllipsized(0))
        rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<TextView>("view-probe")
            val layout = checkNotNull(view.layout)
            val ellipsis = layout.getEllipsisCount(0) > 0
            val clipped = layout.height > view.height - view.totalPaddingTop - view.totalPaddingBottom
            assertEquals("TextView overflow", expectedOverflow, ellipsis || clipped)
            assertEquals("TextView ellipsis", expectedEllipsis, ellipsis)
        }
    }

    private fun assertMarqueeMotion() {
        val compose = rule.onNodeWithTag("compose-render-panel").fetchSemanticsNode().boundsInWindow
        val composeBounds = Rect(compose.left.toInt(), compose.top.toInt(), compose.right.toInt(), compose.bottom.toInt())
        val viewBounds = rule.runOnIdle {
            val view = rule.activity.window.decorView.findViewWithTag<TextView>("view-probe")
            val xy = IntArray(2).also(view::getLocationOnScreen)
            Rect(xy[0], xy[1], xy[0] + view.width, xy[1] + view.height)
        }
        val first = screenshot()
        try {
            saveCapture(first, "marquee-start")
            var composeMoved = false
            var viewMoved = false
            repeat(20) {
                rule.mainClock.advanceTimeBy(200)
                SystemClock.sleep(150)
                rule.waitForIdle()
                val frame = screenshot()
                try {
                    val composeChanged = pixelsChanged(first, frame, composeBounds)
                    val viewChanged = pixelsChanged(first, frame, viewBounds)
                    composeMoved = composeMoved || composeChanged
                    viewMoved = viewMoved || viewChanged
                    if (composeChanged && viewChanged) {
                        saveCapture(frame, "marquee-moving")
                        return
                    }
                } finally {
                    frame.recycle()
                }
            }
            error("No frame showed motion in both marquee regions (Compose moved: $composeMoved; TextView moved: $viewMoved)")
        } finally {
            first.recycle()
        }
    }

    private fun pixelsChanged(first: Bitmap, frame: Bitmap, bounds: Rect): Boolean {
        val before = Bitmap.createBitmap(first, bounds.left, bounds.top, bounds.width(), bounds.height())
        val after = Bitmap.createBitmap(frame, bounds.left, bounds.top, bounds.width(), bounds.height())
        return try {
            !before.sameAs(after)
        } finally {
            before.recycle()
            after.recycle()
        }
    }

    private fun withInteractiveWindows(block: () -> Unit) {
        val automation = instrumentation.uiAutomation
        val serviceInfo = automation.serviceInfo
        val originalFlags = serviceInfo.flags
        serviceInfo.flags = originalFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = serviceInfo
        try {
            block()
        } finally {
            serviceInfo.flags = originalFlags
            automation.serviceInfo = serviceInfo
        }
    }

    private fun selectTab(name: String) {
        rule.onNodeWithText(name, useUnmergedTree = true).performClick()
    }

    private fun captureToolbar(name: String) {
        rule.waitUntil(timeoutMillis = 5_000) { menuHas("Copy") }
        capture(name)
    }

    private fun menuHas(label: String): Boolean = instrumentation.uiAutomation.windows.any {
        it.root?.containsLabel(label) == true
    }

    private fun AccessibilityNodeInfo.containsLabel(label: String): Boolean =
        text?.toString().equals(label, ignoreCase = true) || contentDescription?.toString().equals(label, ignoreCase = true) ||
            (0 until childCount).any { getChild(it)?.containsLabel(label) == true }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap = screenshot()
        try {
            saveCapture(bitmap, name)
        } finally {
            bitmap.recycle()
        }
    }

    private fun screenshot(): Bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())

    private fun saveCapture(bitmap: Bitmap, name: String) {
        // Gradle collects additional test output before uninstalling the application.
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let(::File) ?: instrumentation.targetContext.filesDir
        directory.mkdirs()
        File(directory, "ci-$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
