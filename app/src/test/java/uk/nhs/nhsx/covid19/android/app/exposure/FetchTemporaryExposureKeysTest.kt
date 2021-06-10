package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FetchTemporaryExposureKeysTest {

    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val transmissionRiskLevelApplier = mockk<TransmissionRiskLevelApplier>(relaxed = true)

    private val testSubject =
        FetchTemporaryExposureKeys(exposureNotificationApi, transmissionRiskLevelApplier)

    // From COV-3645:
    // Change the key upload window key to include all days with transmissionRiskLevel above 0
    // (assuming a key for that day exists)
    @Test
    fun `filters out keys with transmissionRiskLevel of 0`() = runBlocking {
        val key1 = NHSTemporaryExposureKey("1", intervalStart("2014-12-22T00:00:00Z"), 144)
        val key2 = NHSTemporaryExposureKey("2", intervalStart("2014-12-23T00:00:00Z"), 144)
        val key3 = NHSTemporaryExposureKey("3", intervalStart("2014-12-24T00:00:00Z"), 144)
        val key4 = NHSTemporaryExposureKey("4", intervalStart("2014-12-25T00:00:00Z"), 144)
        val key5 = NHSTemporaryExposureKey("5", intervalStart("2014-12-26T00:00:00Z"), 144)

        val exposureKeys = listOf(key1, key2, key3, key4, key5)

        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } returns exposureKeys

        every { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } returns
            exposureKeys.map { if (it.key == "1") it.copy(transmissionRiskLevel = 0) else it.copy(transmissionRiskLevel = 1) }

        val result = testSubject(keySharingInfo)

        val expected = Success(
            listOf(
                key2.copy(transmissionRiskLevel = 1),
                key3.copy(transmissionRiskLevel = 1),
                key4.copy(transmissionRiskLevel = 1),
                key5.copy(transmissionRiskLevel = 1)
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `fetch fails when transmissionRiskLevelApplier throws`() = runBlocking {
        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } returns listOf()

        val expectedMessage = "Something went wrong"
        every { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } throws Exception(
            expectedMessage
        )

        val result = testSubject(keySharingInfo)

        assertTrue { result is Failure }
        assertTrue { (result as Failure).throwable.message == expectedMessage }
    }

    @Test
    fun `fetch fails because of resolution required api exception returns resolution required`() =
        runBlocking {
            val expectedStatus = Status(ConnectionResult.RESOLUTION_REQUIRED)
            val exception = ApiException(expectedStatus)

            coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } throws exception

            val result = testSubject(keySharingInfo)

            assertEquals(ResolutionRequired(expectedStatus), result)
        }

    @Test
    fun `fetch fails because of non-resolution-required api exception returns failure`() =
        runBlocking {
            val expectedStatus = Status(ConnectionResult.DEVELOPER_ERROR)
            val exception = ApiException(expectedStatus)

            coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } throws exception

            val result = testSubject(keySharingInfo)

            assertEquals(Failure(exception), result)
        }

    @Test
    fun `fetch fails because of api exception returns failure`() = runBlocking {
        val exception = ApiException(Status(10))

        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } throws exception

        val result = testSubject(keySharingInfo)

        assertEquals(Failure(exception), result)
    }

    @Test
    fun `fetch fails because of generic exception returns failure`() = runBlocking {
        val exception = Exception("test")

        coEvery { exposureNotificationApi.temporaryExposureKeyHistory() } throws exception

        val result = testSubject(keySharingInfo)

        assertEquals(Failure(exception), result)
    }

    private fun intervalStart(date: String): Int {
        val millisIn10Minutes = Duration.ofMinutes(10).toMillis()
        return (Instant.parse(date).toEpochMilli() / millisIn10Minutes).toInt()
    }

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token",
        acknowledgedDate = Instant.parse("2014-12-26T12:00:00Z"),
        notificationSentDate = null
    )
}
