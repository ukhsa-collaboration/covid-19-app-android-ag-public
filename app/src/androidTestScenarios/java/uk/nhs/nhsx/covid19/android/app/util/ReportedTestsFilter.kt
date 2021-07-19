package uk.nhs.nhsx.covid19.android.app.util

import androidx.test.internal.runner.filters.ParentFilter
import org.junit.runner.Description
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.isRunningReporterTool

class ReportedTestsFilter : ParentFilter() {
    override fun evaluateTest(description: Description): Boolean {
        if (!isRunningReporterTool()) {
            return true
        }
        val testAnnotation = description.getAnnotation(Reported::class.java)
        return testAnnotation != null
    }

    override fun describe(): String {
        return "skip tests not annotated with '${Reported::class.simpleName}' " +
            "if running document reporting tool (DoReTo)"
    }
}
