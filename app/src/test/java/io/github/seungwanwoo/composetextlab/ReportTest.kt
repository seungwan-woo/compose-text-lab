package io.github.seungwanwoo.composetextlab

import org.junit.Assert.*
import org.junit.Test

class ReportTest {
    @Test fun retainsActualUnitsAndDoesNotInventMissingMeasurements() {
        val report = buildReport(LabSettings(), mapOf("sdk" to "36"), "system",
            listOf(ProbeMeasurement("compose", 240, 50, 240, 50, 2, 18f, 42f, false, false)))
        assertEquals(1, report.getJSONArray("measurements").length())
        val measurement = report.getJSONArray("measurements").getJSONObject(0)
        assertEquals(18.0, measurement.getDouble("firstBaselineFromWidgetTopPx"), 0.0)
        assertEquals("36", report.getJSONObject("environment").getString("sdk"))
        assertFalse(report.getBoolean("pixelEquivalenceVerified"))
        assertEquals(0, buildReport(LabSettings(), emptyMap(), "system", emptyList())
            .getJSONArray("measurements").length())
    }
    @Test fun exportsSettingsAndUnicodeAsValidJson() {
        val report = buildReport(LabSettings(text = "한글\n\"A\"🙂"), emptyMap(), "font.ttf", emptyList())
        val parsed = org.json.JSONObject(report.toString())
        assertEquals("한글\n\"A\"🙂", parsed.getJSONObject("settings").getString("text"))
        assertEquals("font.ttf", parsed.getString("fontIdentity"))
    }
}
