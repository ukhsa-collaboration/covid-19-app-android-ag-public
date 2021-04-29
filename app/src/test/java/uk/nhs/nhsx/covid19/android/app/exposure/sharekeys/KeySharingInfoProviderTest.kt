package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant
import kotlin.test.assertEquals

class KeySharingInfoProviderTest {
    private val keySharingInfoJsonStorage = mockk<KeySharingInfoJsonStorage>(relaxUnitFun = true)
    private val moshi = Moshi.Builder()
        .add(InstantAdapter())
        .build()

    private val testSubject = KeySharingInfoProvider(keySharingInfoJsonStorage, moshi)

    @Test
    fun `test writing`() {
        testSubject.keySharingInfo = keySharingInfo

        verify { keySharingInfoJsonStorage.value = keySharingInfoJson() }
    }

    @Test
    fun `test reading`() {
        every { keySharingInfoJsonStorage.value } returns keySharingInfoJson()

        assertEquals(keySharingInfo, testSubject.keySharingInfo)
    }

    @Test
    fun `test setting notificationSent true`() {
        every { keySharingInfoJsonStorage.value } returns keySharingInfoJson()
        val notificationSentDate = "2020-07-26T10:00:00Z"

        testSubject.setNotificationSentDate(notificationSentDate = Instant.parse(notificationSentDate))

        verify { keySharingInfoJsonStorage.value = keySharingInfoJson(notificationSentDate = notificationSentDate) }
    }

    @Test
    fun `test setting hasDeclinedSharingKeys true`() {
        every { keySharingInfoJsonStorage.value } returns keySharingInfoJson()

        testSubject.setHasDeclinedSharingKeys()

        verify { keySharingInfoJsonStorage.value = keySharingInfoJson(hasDeclinedSharingKeys = true) }
    }

    @Test
    fun `test calling reset`() {
        every { keySharingInfoJsonStorage.value } returns keySharingInfoJson()

        testSubject.reset()

        verify { keySharingInfoJsonStorage.value = "null" }
    }

    val token = "token"
    val acknowledgedDate = "2020-07-25T10:00:00Z"

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = token,
        acknowledgedDate = Instant.parse(acknowledgedDate),
        notificationSentDate = null,
        hasDeclinedSharingKeys = false,
        testKitType = null,
        requiresConfirmatoryTest = false
    )

    private fun keySharingInfoJson(hasDeclinedSharingKeys: Boolean = false) =
        """{"diagnosisKeySubmissionToken":"$token","acknowledgedDate":"$acknowledgedDate","hasDeclinedSharingKeys":$hasDeclinedSharingKeys,"requiresConfirmatoryTest":false}"""

    private fun keySharingInfoJson(notificationSentDate: String, hasDeclinedSharingKeys: Boolean = false) =
        """{"diagnosisKeySubmissionToken":"$token","acknowledgedDate":"$acknowledgedDate","notificationSentDate":"$notificationSentDate","hasDeclinedSharingKeys":$hasDeclinedSharingKeys,"requiresConfirmatoryTest":false}"""
}
