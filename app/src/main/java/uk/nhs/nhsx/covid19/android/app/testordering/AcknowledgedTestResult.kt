package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class AcknowledgedTestResult(
    val testEndDate: LocalDate,
    val testResult: RelevantVirologyTestResult,
    override val testKitType: VirologyTestKitType?,
    val acknowledgedDate: LocalDate,
    override val requiresConfirmatoryTest: Boolean = false,
    val confirmedDate: LocalDate? = null
) : TestResult {

    override fun isPositive(): Boolean =
        testResult == POSITIVE

    override fun isConfirmed(): Boolean =
        !requiresConfirmatoryTest || confirmedDate != null
}

enum class RelevantVirologyTestResult(val relevance: Int) {
    POSITIVE(1),
    NEGATIVE(0)
}

fun VirologyTestResult.toRelevantVirologyTestResult(): RelevantVirologyTestResult? =
    when (this) {
        VirologyTestResult.POSITIVE -> POSITIVE
        NEGATIVE -> RelevantVirologyTestResult.NEGATIVE
        VOID -> null
    }
