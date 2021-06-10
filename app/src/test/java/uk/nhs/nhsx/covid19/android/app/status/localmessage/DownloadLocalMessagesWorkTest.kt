package uk.nhs.nhsx.covid19.android.app.status.localmessage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.LocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.status.ShowLocalMessageNotificationIfNeeded

class DownloadLocalMessagesWorkTest {
    private val mockLocalMessagesApi = mockk<LocalMessagesApi>(relaxed = true)
    private val mockLocalMessagesProvider = mockk<LocalMessagesProvider>(relaxUnitFun = true)
    private val showLocalInformationNotificationIfNeeded = mockk<ShowLocalMessageNotificationIfNeeded>()

    private val testSubject = DownloadLocalMessagesWork(
        mockLocalMessagesApi,
        mockLocalMessagesProvider,
        showLocalInformationNotificationIfNeeded
    )

    @Test
    fun `downloads and stores targeted local info from the backend, then checks if notification should be shown`() =
        runBlocking {
            val locallyStoredResponse = mockk<LocalMessagesResponse>()
            val receivedResponse = mockk<LocalMessagesResponse>()
            every { mockLocalMessagesProvider.localMessages } returns locallyStoredResponse
            coEvery { mockLocalMessagesApi.fetchLocalMessages() } returns receivedResponse

            testSubject.invoke()

            coVerify { mockLocalMessagesApi.fetchLocalMessages() }
            verify { mockLocalMessagesProvider.localMessages = receivedResponse }
            coVerify { showLocalInformationNotificationIfNeeded(locallyStoredResponse, receivedResponse) }
        }
}
