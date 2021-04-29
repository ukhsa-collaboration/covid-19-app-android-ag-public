package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider

class ClearOutdatedKeySharingInfoTest {

    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val canShareKeys = mockk<CanShareKeys>()

    private val testSubject = ClearOutdatedKeySharingInfo(keySharingInfoProvider, canShareKeys)

    @Test
    fun `when keys can be shared then do not reset keySharingInfoProvider`() {
        every { canShareKeys() } returns KeySharingPossible(mockk())

        testSubject.invoke()

        verify(exactly = 0) { keySharingInfoProvider.reset() }
    }

    @Test
    fun `when keys can not be shared then reset keySharingInfoProvider`() {
        every { canShareKeys() } returns NoKeySharingPossible

        testSubject.invoke()

        verify { keySharingInfoProvider.reset() }
    }
}
