package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider.Companion.LATEST_TEST_RESULT_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import java.time.Instant

class LatestTestResultProviderTest : ProviderTest<LatestTestResultProvider, LatestTestResult?>() {
    override val getTestSubject = ::LatestTestResultProvider
    override val property = LatestTestResultProvider::latestTestResult
    override val key = LATEST_TEST_RESULT_KEY
    override val defaultValue: LatestTestResult? = null
    override val expectations: List<ProviderTestExpectation<LatestTestResult?>> = listOf(
        ProviderTestExpectation(json = latestTestResultJson, objectValue = latestTestResult)
    )

    companion object {
        private const val latestTestResultJson: String =
            """{"diagnosisKeySubmissionToken":"token","testEndDate":"2020-11-18T13:40:56.333Z","testResult":"POSITIVE"}"""
        private val latestTestResult = LatestTestResult(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.parse("2020-11-18T13:40:56.333Z"),
            testResult = POSITIVE
        )
    }
}
