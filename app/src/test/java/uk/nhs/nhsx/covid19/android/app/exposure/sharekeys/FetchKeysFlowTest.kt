package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity.RESULT_OK
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import java.time.Instant

class FetchKeysFlowTest {
    private val callback = mockk<FetchKeysFlow.Callback>(relaxUnitFun = true)
    private val exposureNotificationManager = mockk<ExposureNotificationManager>()
    private val exposureNotificationPermissionHelperFactory =
        mockk<ExposureNotificationPermissionHelper.Factory>()
    private val fetchKeysHelperFactory = mockk<FetchKeysHelper.Factory>()
    private val coroutineScope = TestCoroutineScope()
    private val exposureNotificationPermissionHelper = mockk<ExposureNotificationPermissionHelper>(relaxUnitFun = true)
    private val fetchKeysHelper = mockk<FetchKeysHelper>(relaxUnitFun = true)

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "a",
        acknowledgedDate = Instant.now(),
        notificationSentDate = null,
        testKitType = null,
        requiresConfirmatoryTest = false
    )

    lateinit var testSubject: FetchKeysFlow

    @Before
    fun setUp() {
        every { exposureNotificationPermissionHelperFactory.create(any(), any()) } returns
            exposureNotificationPermissionHelper
        every { fetchKeysHelperFactory.create(any(), any(), any()) } returns fetchKeysHelper

        testSubject = FetchKeysFlow(
            callback,
            exposureNotificationManager,
            exposureNotificationPermissionHelperFactory,
            fetchKeysHelperFactory,
            coroutineScope,
            keySharingInfo
        )
    }

    @Test
    fun `invoking the fetch keys flow when exposure notifications are enabled should trigger fetchKeysHelper`() =
        coroutineScope.runBlockingTest {
            coEvery { exposureNotificationManager.isEnabled() } returns true

            testSubject()

            verify { fetchKeysHelper.fetchKeys() }
        }

    @Test
    fun `invoking the fetch keys flow when exposure notifications are disabled should start exposure notification activation flow`() =
        coroutineScope.runBlockingTest {
            coEvery { exposureNotificationManager.isEnabled() } returns false

            testSubject()

            verify { exposureNotificationPermissionHelper.startExposureNotifications() }
        }

    @Test
    fun `onActivityResult delegates requestCode and resultCode to helpers`() {
        val requestCode = 1234
        val resultCode = RESULT_OK

        testSubject.onActivityResult(requestCode, resultCode)

        verify { exposureNotificationPermissionHelper.onActivityResult(requestCode, resultCode) }
        verify { fetchKeysHelper.onActivityResult(requestCode, resultCode) }
    }
}
