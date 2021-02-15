package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class VirologyTestResultResponse(
    val testEndDate: Instant,
    val testResult: VirologyTestResult,
    val testKit: VirologyTestKitType,
    val diagnosisKeySubmissionSupported: Boolean,
    val requiresConfirmatoryTest: Boolean
)

enum class VirologyTestResult {
    POSITIVE,
    NEGATIVE,
    VOID,
}
