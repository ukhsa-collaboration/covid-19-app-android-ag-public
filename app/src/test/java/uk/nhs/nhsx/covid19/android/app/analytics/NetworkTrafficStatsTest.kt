package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.lang.Exception
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkTrafficStatsTest {
    private val networkStatsStorage = mockk<NetworkStatsStorage>(relaxed = true)

    private val testSubject = NetworkTrafficStats(
        networkStatsStorage,
        networkDownloadDataProvider = { NUMBER_OF_DOWNLOADED_BYTES },
        networkUploadDataProvider = { NUMBER_OF_UPLOADED_BYTES }
    )

    @Test
    fun `last downloaded stored value is empty returns zero and updates the last stored value`() {
        every { networkStatsStorage.lastDownloadedBytes } returns null

        val result = testSubject.getTotalBytesDownloaded()

        verify { networkStatsStorage.lastDownloadedBytes = NUMBER_OF_DOWNLOADED_BYTES }
        assertEquals(0, result)
    }

    @Test
    fun `last downloaded stored value is not empty returns value and updates the last stored value`() {
        val lastDownloadedBytes = 500
        every { networkStatsStorage.lastDownloadedBytes } returns lastDownloadedBytes

        val result = testSubject.getTotalBytesDownloaded()

        val expected = NUMBER_OF_DOWNLOADED_BYTES - lastDownloadedBytes

        verify { networkStatsStorage.lastDownloadedBytes = expected }
        assertEquals(expected, result)
    }

    @Test
    fun `last downloaded stored value is not empty and data provider value is smaller`() {
        val lastDownloadedBytes = 1500
        every { networkStatsStorage.lastDownloadedBytes } returns lastDownloadedBytes

        val result = testSubject.getTotalBytesDownloaded()

        val expected = NUMBER_OF_DOWNLOADED_BYTES

        verify { networkStatsStorage.lastDownloadedBytes = expected }
        assertEquals(expected, result)
    }

    @Test
    fun `when network download data provider throws exception total bytes downloaded is null`() {
        val customTestSubject = NetworkTrafficStats(
            networkStatsStorage,
            networkDownloadDataProvider = { throw Exception() },
            networkUploadDataProvider = { throw Exception() }
        )

        every { networkStatsStorage.lastDownloadedBytes } returns 0

        val result = customTestSubject.getTotalBytesDownloaded()

        assertNull(result)
    }

    @Test
    fun `last uploaded stored value is empty returns zero and updates the last stored value`() {
        every { networkStatsStorage.lastUploadedBytes } returns null

        val result = testSubject.getTotalBytesUploaded()

        verify { networkStatsStorage.lastUploadedBytes = NUMBER_OF_UPLOADED_BYTES }
        assertEquals(0, result)
    }

    @Test
    fun `last uploaded stored value is not empty returns value and updates the last stored value`() {
        val lastUploadedBytes = 500
        every { networkStatsStorage.lastUploadedBytes } returns lastUploadedBytes

        val result = testSubject.getTotalBytesUploaded()

        val expected = NUMBER_OF_DOWNLOADED_BYTES - lastUploadedBytes

        verify { networkStatsStorage.lastUploadedBytes = expected }
        assertEquals(expected, result)
    }

    @Test
    fun `last uploaded stored value is not empty and data provider value is smaller`() {
        val lastUploadedBytes = 1500
        every { networkStatsStorage.lastUploadedBytes } returns lastUploadedBytes

        val result = testSubject.getTotalBytesUploaded()

        val expected = NUMBER_OF_DOWNLOADED_BYTES

        verify { networkStatsStorage.lastUploadedBytes = expected }
        assertEquals(expected, result)
    }

    @Test
    fun `when network upload data provider throws exception total bytes uploaded is null`() {
        val customTestSubject = NetworkTrafficStats(
            networkStatsStorage,
            networkDownloadDataProvider = { throw Exception() },
            networkUploadDataProvider = { throw Exception() }
        )

        every { networkStatsStorage.lastUploadedBytes } returns 0

        val result = customTestSubject.getTotalBytesUploaded()

        assertNull(result)
    }

    companion object {
        const val NUMBER_OF_DOWNLOADED_BYTES = 1000
        const val NUMBER_OF_UPLOADED_BYTES = 1000
    }
}
