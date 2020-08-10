package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.DateWindow
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitTemporaryExposureKeysTest {

    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxed = true)
    private val latestTestResultProvider = mockk<LatestTestResultProvider>(relaxed = true)

    private val testSubject =
        SubmitTemporaryExposureKeys(
            exposureNotificationApi,
            periodicTasks,
            latestTestResultProvider
        )

    @Test
    fun `successful submission of keys`() = runBlocking {

        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } returns listOf()

        coEvery { periodicTasks.scheduleKeysSubmission(any(), any(), any()) } returns Unit

        val actualResult = testSubject.invoke()

        assertEquals(SubmitResult.Success, actualResult)
    }

    @Test
    fun `resolution required`() = runBlocking {
        val resolutionStatus = Status(CommonStatusCodes.RESOLUTION_REQUIRED)

        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } throws ApiException(
            resolutionStatus
        )

        val actualResult = testSubject.invoke()

        assertEquals(SubmitResult.ResolutionRequired(resolutionStatus), actualResult)
    }

    @Test
    fun `return failure`() = runBlocking {
        val testException = Exception("Test exception")
        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } throws testException

        val actualResult = testSubject.invoke()

        assertEquals(SubmitResult.Failure(testException), actualResult)
    }

    @Test
    fun `submission fails because no submission key present`() = runBlocking {
        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } returns listOf()

        coEvery { latestTestResultProvider.latestTestResult } returns null

        val actualResult = testSubject.invoke()

        assertTrue { actualResult is SubmitResult.Failure }
        val expectedFailureMessage = (actualResult as SubmitResult.Failure).throwable.message

        assertEquals(
            expectedFailureMessage,
            "Can not submit keys without diagnosisKeySubmissionToken from virology test result"
        )
    }

    @Test
    fun `send keys in date window only`() = runBlocking {

        val key1 = NHSTemporaryExposureKey("1", intervalStart("2014-12-22T00:00:00Z"), 144)
        val key2 = NHSTemporaryExposureKey("2", intervalStart("2014-12-23T00:00:00Z"), 144)
        val key3 = NHSTemporaryExposureKey("3", intervalStart("2014-12-24T00:00:00Z"), 144)
        val key4 = NHSTemporaryExposureKey("4", intervalStart("2014-12-25T00:00:00Z"), 144)
        val key5 = NHSTemporaryExposureKey("5", intervalStart("2014-12-26T00:00:00Z"), 144)
        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } returns listOf(
            key1,
            key2,
            key3,
            key4,
            key5
        )

        coEvery { periodicTasks.scheduleKeysSubmission(any(), any(), any()) } returns Unit
        val diagnosisKeySubmissionToken = "submission_key"
        coEvery { latestTestResultProvider.latestTestResult } returns LatestTestResult(
            diagnosisKeySubmissionToken,
            Instant.now(),
            POSITIVE
        )

        val actualResult = testSubject.invoke(
            DateWindow(
                LocalDate.parse("2014-12-24"),
                LocalDate.parse("2014-12-25")
            )
        )

        val payload = TemporaryExposureKeysPayload(
            diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
            temporaryExposureKeys = listOf(key3, key4)
        )
        coVerify {
            periodicTasks.scheduleKeysSubmission(
                payload
            )
        }
        assertEquals(SubmitResult.Success, actualResult)
    }

    private fun intervalStart(date: String): Int {
        val millisIn10Minutes = Duration.ofMinutes(10).toMillis()
        return (Instant.parse(date).toEpochMilli() / millisIn10Minutes).toInt()
    }
}
