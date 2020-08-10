package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class NetworkTrafficStatsTest {
    private val networkStatsStorage = mockk<NetworkStatsStorage>(relaxed = true)

    private val testSubject = NetworkTrafficStats(
        networkStatsStorage,
        networkDownloadDataProvider = { NUMBER_OF_DOWNLOADED_BYTES },
        networkUploadDataProvider = { NUMBER_OF_UPLOADED_BYTES }
    )

    @Test
    fun `last stored value is empty returns null and updates the last stored value`() {
        every { networkStatsStorage.lastDownloadedBytes } returns null

        val result = testSubject.getTotalBytesDownloaded()

        verify { networkStatsStorage.lastDownloadedBytes = NUMBER_OF_DOWNLOADED_BYTES }
        assertEquals(null, result)
    }

    @Test
    fun `last stored value is not empty returns value and updates the last stored value`() {
        val lastDownloadedBytes = 500
        every { networkStatsStorage.lastDownloadedBytes } returns lastDownloadedBytes

        val result = testSubject.getTotalBytesDownloaded()

        val expected = NUMBER_OF_DOWNLOADED_BYTES - lastDownloadedBytes

        verify { networkStatsStorage.lastDownloadedBytes = expected }
        assertEquals(expected, result)
    }

    @Test
    fun `last stored value is not empty and data provider value is smaller`() {
        val lastDownloadedBytes = 1500
        every { networkStatsStorage.lastDownloadedBytes } returns lastDownloadedBytes

        val result = testSubject.getTotalBytesDownloaded()

        val expected = NUMBER_OF_DOWNLOADED_BYTES

        verify { networkStatsStorage.lastDownloadedBytes = expected }
        assertEquals(expected, result)
    }

    companion object {
        const val NUMBER_OF_DOWNLOADED_BYTES = 1000
        const val NUMBER_OF_UPLOADED_BYTES = 1000
    }
}
