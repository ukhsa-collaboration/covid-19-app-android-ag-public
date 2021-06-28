package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import java.time.Clock
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class AcknowledgedTestResult(
    val testEndDate: LocalDate,
    val testResult: RelevantVirologyTestResult,
    override val testKitType: VirologyTestKitType?,
    val acknowledgedDate: LocalDate,
    val requiresConfirmatoryTest: Boolean = false,
    val confirmedDate: LocalDate? = null,
    override val confirmatoryDayLimit: Int? = null,
    val confirmatoryTestCompletionStatus: ConfirmatoryTestCompletionStatus? = null
) : TestResult {
    override fun testEndDate(clock: Clock): LocalDate = testEndDate

    override fun isPositive(): Boolean =
        testResult == POSITIVE

    override fun isNegative(): Boolean =
        testResult == NEGATIVE

    fun isConfirmed(): Boolean =
        confirmatoryTestCompletionStatus == COMPLETED_AND_CONFIRMED || !requiresConfirmatoryTest

    fun isCompleted(): Boolean =
        confirmatoryTestCompletionStatus == COMPLETED ||
            confirmatoryTestCompletionStatus == COMPLETED_AND_CONFIRMED
}

enum class ConfirmatoryTestCompletionStatus {
    COMPLETED, COMPLETED_AND_CONFIRMED
}

enum class RelevantVirologyTestResult(val relevance: Int) {
    POSITIVE(1),
    NEGATIVE(0)
}

fun VirologyTestResult.toRelevantVirologyTestResult(): RelevantVirologyTestResult? =
    when (this) {
        VirologyTestResult.POSITIVE -> POSITIVE
        VirologyTestResult.NEGATIVE -> NEGATIVE
        VOID, PLOD -> null
    }
