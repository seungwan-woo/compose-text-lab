package io.github.seungwanwoo.composetextlab

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import org.junit.Rule
import org.junit.Test

class ResultCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun captureComparisonScreens() {
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        rule.onNodeWithText("Theme").performScrollTo()
        rule.onNodeWithText("Light").performClick().assertIsSelected()
        rule.onNodeWithTag("panel-width").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(420f) }
        rule.onNodeWithText("Hide settings").performClick()
        rule.onNodeWithTag("side-by-side").performScrollTo().performClick().assertIsOn()
        selectTab("Effects")
        selectTab("Render")
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

        val automation = instrumentation.uiAutomation
        val serviceInfo = automation.serviceInfo
        val originalFlags = serviceInfo.flags
        serviceInfo.flags = originalFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = serviceInfo
        try {
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
        } finally {
            serviceInfo.flags = originalFlags
            automation.serviceInfo = serviceInfo
        }
    }

    private fun selectTab(name: String) {
        rule.onNodeWithText(name, useUnmergedTree = true).performClick()
    }

    private fun captureToolbar(name: String) {
        val automation = instrumentation.uiAutomation
        rule.waitUntil(timeoutMillis = 5_000) {
            automation.windows.any { it.root?.containsCopyAction() == true }
        }
        capture(name)
    }

    private fun AccessibilityNodeInfo.containsCopyAction(): Boolean =
        text?.toString() == "Copy" || contentDescription?.toString() == "Copy" ||
            (0 until childCount).any { getChild(it)?.containsCopyAction() == true }

    private fun capture(name: String) {
        rule.waitForIdle()
        // Gradle collects additional test output before uninstalling the application.
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let(::File) ?: instrumentation.targetContext.filesDir
        directory.mkdirs()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "ci-$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
