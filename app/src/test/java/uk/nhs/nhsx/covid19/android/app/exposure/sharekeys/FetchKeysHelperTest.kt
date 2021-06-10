package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.time.Instant

class FetchKeysHelperTest {
    private val callback = mockk<FetchKeysHelper.Callback>(relaxUnitFun = true)
    private val fetchTemporaryExposureKeys = mockk<FetchTemporaryExposureKeys>()
    private val coroutineScope = TestCoroutineScope()

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token1",
        acknowledgedDate = Instant.now(),
        notificationSentDate = null
    )

    private val testSubject = spyk(FetchKeysHelper(callback, fetchTemporaryExposureKeys, coroutineScope, keySharingInfo))

    @Test
    fun `invokes callback's onSuccess after fetching keys successfully`() = coroutineScope.runBlockingTest {
        val temporaryExposureKeys = listOf<NHSTemporaryExposureKey>()
        coEvery { fetchTemporaryExposureKeys(keySharingInfo) } returns
            TemporaryExposureKeysFetchResult.Success(temporaryExposureKeys)

        testSubject.fetchKeys()

        verify { callback.onSuccess(temporaryExposureKeys, keySharingInfo.diagnosisKeySubmissionToken) }

        confirmVerified(callback)
    }

    @Test
    fun `invokes callback's onError after failing to fetch keys`() = coroutineScope.runBlockingTest {
        val throwable = RuntimeException()
        coEvery { fetchTemporaryExposureKeys(keySharingInfo) } returns
            TemporaryExposureKeysFetchResult.Failure(throwable)

        testSubject.fetchKeys()

        verify { callback.onError(throwable) }

        confirmVerified(callback)
    }

    @Test
    fun `invokes callback's onPermissionRequired after failing to fetch keys caused by lack of permission`() = coroutineScope.runBlockingTest {
        val status = mockk<Status>(relaxUnitFun = true)
        coEvery { fetchTemporaryExposureKeys(keySharingInfo) } returns
            TemporaryExposureKeysFetchResult.ResolutionRequired(status)

        testSubject.fetchKeys()

        val slot = slot<(Activity) -> Unit>()
        verify { callback.onPermissionRequired(capture(slot)) }

        confirmVerified(callback)

        val activity = mockk<Activity>()
        slot.captured.invoke(activity)

        verify { status.startResolutionForResult(activity, FetchKeysHelper.REQUEST_CODE_SUBMIT_KEYS_PERMISSION) }
    }

    @Test
    fun `invokes callback's onPermissionDenied if Activity resultCode is not OK`() = coroutineScope.runBlockingTest {
        testSubject.onActivityResult(FetchKeysHelper.REQUEST_CODE_SUBMIT_KEYS_PERMISSION, resultCode = Activity.RESULT_CANCELED)

        verify { callback.onPermissionDenied() }

        confirmVerified(callback)
    }

    @Test
    fun `invokes fetchKeys if Activity resultCode is OK`() = coroutineScope.runBlockingTest {
        coEvery { fetchTemporaryExposureKeys(keySharingInfo) } returns
            TemporaryExposureKeysFetchResult.Success(emptyList())

        testSubject.onActivityResult(FetchKeysHelper.REQUEST_CODE_SUBMIT_KEYS_PERMISSION, resultCode = Activity.RESULT_OK)

        verify { testSubject.fetchKeys() }
    }
}
