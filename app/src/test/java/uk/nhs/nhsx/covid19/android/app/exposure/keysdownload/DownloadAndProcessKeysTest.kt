package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Daily
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Hourly
import uk.nhs.nhsx.covid19.android.app.remote.KeysDistributionApi
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class DownloadAndProcessKeysTest {

    private val keysDistributionApi = mockk<KeysDistributionApi>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val keyFileCache = mockk<KeyFilesCache>(relaxed = true)
    private val downloadKeysParam = mockk<DownloadKeysParams>()
    private val lastDownloadedKeyProvider = mockk<LastDownloadedKeyTimeProvider>(relaxed = true)
    private var clock: Clock = Clock.fixed(Instant.parse("2014-12-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject =
        DownloadAndProcessKeys(
            keysDistributionApi,
            exposureNotificationApi,
            keyFileCache,
            downloadKeysParam,
            lastDownloadedKeyProvider,
            clock
        )

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.isEnabled() } returns true
        coEvery { exposureNotificationApi.version() } returns 2L
        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.now(clock).minusHours(5)
    }

    @Test
    fun `success process of Daily keys from network call`() = runBlocking {

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(Daily("2014122110"))

        coEvery { keysDistributionApi.fetchDailyKeys("2014122110.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        every { keyFileCache.createFile("2014122110", any()) } returns mockk()

        every { keyFileCache.getFile(any()) } returns null

        val result = testSubject.invoke()

        coVerify { exposureNotificationApi.provideDiagnosisKeys(any()) }

        verify(exactly = 1) { lastDownloadedKeyProvider.saveLastStoredTime("2014122110") }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `success process of Daily keys from network call when last downloaded keys timestamp is null`() = runBlocking {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns null

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(Daily("2014122110"))

        coEvery { keysDistributionApi.fetchDailyKeys("2014122110.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        every { keyFileCache.createFile("2014122110", any()) } returns mockk()

        every { keyFileCache.getFile(any()) } returns null

        val result = testSubject.invoke()

        coVerify { exposureNotificationApi.provideDiagnosisKeys(any()) }

        verify(exactly = 1) { lastDownloadedKeyProvider.saveLastStoredTime("2014122110") }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `success process of Daily keys from cache`() = runBlocking {

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(Daily("2014122110"))

        every { keyFileCache.getFile("2014122110") } returns mockk()

        val result = testSubject.invoke()

        coVerify(exactly = 0) { keysDistributionApi.fetchDailyKeys("2014122110.zip") }

        verify(exactly = 0) { keyFileCache.createFile("2014122110", any()) }

        coVerify { exposureNotificationApi.provideDiagnosisKeys(any()) }

        verify(exactly = 1) { lastDownloadedKeyProvider.saveLastStoredTime("2014122110") }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `success downloading of multiple keys from network call`() = runBlocking {

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(
            Daily("2014122100"),
            Hourly("2014122202")
        )
        coEvery { keysDistributionApi.fetchDailyKeys("2014122100.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        coEvery { keysDistributionApi.fetchHourlyKeys("2014122202.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        every { keyFileCache.createFile(any(), any()) } returns mockk()

        every { keyFileCache.getFile(any()) } returns null

        val result = testSubject.invoke()

        coVerify { exposureNotificationApi.provideDiagnosisKeys(any()) }

        verify(exactly = 1) { lastDownloadedKeyProvider.saveLastStoredTime("2014122202") }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `failure when downloading of multiple keys from network call will still process downloaded keys`() =
        runBlocking {
            val exception = Exception()

            coEvery { downloadKeysParam.getNextQueries() } returns listOf(
                Daily("2014122100"),
                Hourly("2014122202")
            )

            every { keyFileCache.getFile(any()) } returns null

            coEvery { keysDistributionApi.fetchDailyKeys("2014122100.zip") } returns ""
                .toResponseBody("binary/octet-stream".toMediaType())

            coEvery { keysDistributionApi.fetchHourlyKeys("2014122202.zip") } throws exception

            every { keyFileCache.createFile(any(), any()) } returns mockk()

            val slot = slot<List<File>>()
            coEvery { exposureNotificationApi.provideDiagnosisKeys(capture(slot)) } just runs

            val result = testSubject.invoke()

            coVerify { exposureNotificationApi.provideDiagnosisKeys(any()) }

            assertEquals(1, slot.captured.size)

            verify(exactly = 1) { lastDownloadedKeyProvider.saveLastStoredTime("2014122100") }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `failure when processing keys returns error`() = runBlocking {
        val exception = Exception()

        coEvery { downloadKeysParam.getNextQueries() } returns listOf(
            Daily("2014122110")
        )

        every { keyFileCache.getFile(any()) } returns null

        coEvery { keysDistributionApi.fetchDailyKeys("2014122110.zip") } returns ""
            .toResponseBody("binary/octet-stream".toMediaType())

        coEvery { exposureNotificationApi.provideDiagnosisKeys(any()) } throws exception

        val result = testSubject.invoke()

        assertThat(result).isEqualTo(Result.Failure(exception))

        verify(exactly = 0) { keyFileCache.clearOutdatedFiles() }
        verify(exactly = 0) { lastDownloadedKeyProvider.saveLastStoredTime("2014122202") }
    }

    @Test
    fun `does not run if exposure notification is disabled`() = runBlocking {
        coEvery { exposureNotificationApi.isEnabled() } returns false

        val result = testSubject.invoke()

        assertThat(result).isEqualTo(Result.Success(Unit))
        verify { downloadKeysParam wasNot called }
        verify { keyFileCache.createFile(any(), any()) wasNot called }
    }

    @Test
    fun `will return if last downloaded file was less than 4 hours ago`() = runBlocking {
        coEvery { exposureNotificationApi.isEnabled() } returns true

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.now(clock).minusHours(3)

        val result = testSubject.invoke()

        assertThat(result).isEqualTo(Result.Success(Unit))
        verify { downloadKeysParam wasNot called }
        verify { keyFileCache.createFile(any(), any()) wasNot called }
    }
}
