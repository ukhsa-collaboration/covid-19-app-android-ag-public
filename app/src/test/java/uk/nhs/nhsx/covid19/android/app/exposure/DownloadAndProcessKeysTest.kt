package uk.nhs.nhsx.covid19.android.app.exposure

import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadAndProcessKeys
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Daily
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Hourly
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.LastDownloadedKeyTimeProvider
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import uk.nhs.nhsx.covid19.android.app.util.FileHelper

class DownloadAndProcessKeysTest {

    private val keysDistributionApi = mockk<KeysDistributionApi>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val exposureConfigurationApi = mockk<ExposureConfigurationApi>(relaxed = true)
    private val fileProvider = mockk<FileHelper>()
    private val downloadKeysParam = mockk<DownloadKeysParams>()
    private val lastDownloadedKeyProvider = mockk<LastDownloadedKeyTimeProvider>(relaxed = true)

    private val testSubject =
        DownloadAndProcessKeys(
            keysDistributionApi,
            exposureConfigurationApi,
            exposureNotificationApi,
            fileProvider,
            downloadKeysParam,
            lastDownloadedKeyProvider
        )

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.isEnabled() } returns true
        coEvery { exposureConfigurationApi.getExposureConfiguration() } returns getConfigurationWithThreshold()
    }

    @Test
    fun `success downloading of Daily keys`() = runBlocking {

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(Daily("2014122110"))

        coEvery { keysDistributionApi.fetchDailyKeys("2014122110.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        every { fileProvider.provideFile(any()) } returns mockk()

        val result = testSubject.invoke()

        verify(exactly = 1) { lastDownloadedKeyProvider.saveLastStoredTime(any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `success downloading of multiple keys`() = runBlocking {

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(
            Daily("2014122110"),
            Hourly("2014122202")
        )
        coEvery { keysDistributionApi.fetchDailyKeys("2014122110.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        coEvery { keysDistributionApi.fetchHourlyKeys("2014122202.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        every { fileProvider.provideFile(any()) } returns mockk()

        val result = testSubject.invoke()

        verify(exactly = 2) { lastDownloadedKeyProvider.saveLastStoredTime(any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `failure when downloading keys returns error`() = runBlocking {
        val exception = Exception()

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(
            Daily("2014122110")
        )

        coEvery { keysDistributionApi.fetchDailyKeys("2014122110.zip") } throws exception

        val result = testSubject.invoke()

        assertThat(result).isEqualTo(Result.Failure(exception))
        verify { fileProvider.provideFile(any()) wasNot called }
    }

    @Test
    fun `does not run if exposure notification is disabled`() = runBlocking {
        coEvery { exposureNotificationApi.isEnabled() } returns false

        val result = testSubject.invoke()

        assertThat(result).isEqualTo(Result.Success(Unit))
        verify { downloadKeysParam wasNot called }
        verify { fileProvider.provideFile(any()) wasNot called }
    }

    private fun getConfigurationWithThreshold(threshold: Int = 900) =
        ExposureConfigurationResponse(
            exposureNotification = ExposureNotification(
                minimumRiskScore = 11,
                attenuationDurationThresholds = listOf(55, 63),
                attenuationLevelValues = listOf(0, 1, 1, 1, 1, 1, 1, 1),
                daysSinceLastExposureLevelValues = listOf(5, 5, 5, 5, 5, 5, 5, 5),
                durationLevelValues = listOf(0, 0, 0, 1, 1, 1, 1, 0),
                transmissionRiskLevelValues = listOf(1, 3, 4, 5, 6, 7, 8, 6),
                attenuationWeight = 50.0,
                daysSinceLastExposureWeight = 20,
                durationWeight = 50.0,
                transmissionRiskWeight = 50.0
            ),
            riskCalculation = RiskCalculation(
                durationBucketWeights = listOf(1.0, 0.5, 0.0),
                riskThreshold = threshold
            )
        )
}
