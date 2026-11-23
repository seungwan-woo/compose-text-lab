package io.github.seungwanwoo.composetextlab

import org.junit.Assert.*
import org.junit.Test

class LabSettingsTest {
    @Test fun legacySettingsKeepExplicitLayoutAndWidthChoices() {
        val defaults = LabSettings.fromJson("""{"sideBySide":false,"previewWidthDp":320}""")
        assertEquals(PanelLayout.Auto, defaults.panelLayout)
        assertNull(defaults.previewWidthDp)

        val customized = LabSettings.fromJson("""{"sideBySide":true,"previewWidthDp":420}""")
        assertEquals(PanelLayout.SideBySide, customized.panelLayout)
        assertEquals(420f, customized.previewWidthDp)

        val explicit = LabSettings(panelLayout = PanelLayout.Stacked, previewWidthDp = 320f)
        assertEquals(explicit, LabSettings.fromJson(explicit.toJson().toString()))
    }

    @Test fun clampsPreviewInputsBeforeTheyReachEitherEngine() {
        val s = LabSettings(fontSizeSp = -4f, fontScale = 8f, maxLines = 0,
            letterSpacingEm = Float.NaN, lineHeightSp = Float.POSITIVE_INFINITY).normalized()
        assertEquals(8f, s.fontSizeSp)
        assertEquals(2f, s.fontScale)
        assertEquals(1, s.maxLines)
        assertEquals(0f, s.letterSpacingEm)
        assertEquals(0f, s.lineHeightSp)
    }
    @Test fun systemModeUsesSystemScaleInsteadOfStaleOverride() {
        assertEquals(1.6f, LabSettings(fontScale = 2f).effectiveFontScale(1.6f))
        assertEquals(0.8f, LabSettings(useSystemFontScale = false, fontScale = 0.8f).effectiveFontScale(1.6f))
    }
    @Test fun marqueeUsesSameSingleLineSampleForBothEngines() {
        assertEquals("A B C D", LabSettings(text = "A\r\nB\nC\rD", effect = TextEffect.Marquee).renderText())
        assertEquals("A\nB", LabSettings(text = "A\nB").renderText())
    }
}
