package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.IOException
import java.time.Instant

internal class SubmitKeysProgressViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val submitTemporaryExposureKeys = mockk<SubmitTemporaryExposureKeys>()
    private val exposureKeys = listOf<NHSTemporaryExposureKey>()
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>()
    private val diagnoseKeySubmissionToken = ""

    private val testSubject =
        SubmitKeysProgressViewModel(
            submitTemporaryExposureKeys,
            keySharingInfoProvider,
            exposureKeys,
            diagnoseKeySubmissionToken
        )

    private val observer = mockk<Observer<Lce<Unit>>>(relaxUnitFun = true)

    val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "",
        acknowledgedDate = Instant.now()
    )

    @Before
    fun setUp() {
        testSubject.submitKeysResult().observeForever(observer)
    }

    @Test
    fun `error case when keySharingInfo isSelfReporting returns false`() = runBlocking {
        coEvery { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken) } returns
                Result.Failure(throwable)

        testSubject.submitKeys()

        coVerifyOrder {
            observer.onChanged(Lce.Loading)
            submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken)
            observer.onChanged(Lce.Error(throwable))
        }
    }

    @Test
    fun `success case keySharingInfo isSelfReporting returns false`() = runBlocking {
        coEvery { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken) } returns
                Result.Success(Unit)

        testSubject.submitKeys()

        coVerifyOrder {
            observer.onChanged(Lce.Loading)
            submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken)
            observer.onChanged(Lce.Success(Unit))
        }
    }

    @Test
    fun `error case when keySharingInfo is null`() = runBlocking {
        coEvery { keySharingInfoProvider.keySharingInfo } returns null
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken) } returns
                Result.Failure(throwable)

        testSubject.submitKeys()

        coVerifyOrder {
            observer.onChanged(Lce.Loading)
            submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken)
            observer.onChanged(Lce.Error(throwable))
        }
    }

    @Test
    fun `success case when keySharingInfo is null`() = runBlocking {
        coEvery { keySharingInfoProvider.keySharingInfo } returns null
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken) } returns
                Result.Success(Unit)

        testSubject.submitKeys()

        coVerifyOrder {
            observer.onChanged(Lce.Loading)
            submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken)
            observer.onChanged(Lce.Success(Unit))
        }
    }

    @Test
    fun `error case when keySharingInfo isSelfReporting returns true`() = runBlocking {
        coEvery { keySharingInfoProvider.keySharingInfo } returns keySharingInfo.copy(isSelfReporting = true)
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken, isPrivateJourney = true, testKit = "LAB_RESULT") } returns
                Result.Failure(throwable)

        testSubject.submitKeys()

        coVerifyOrder {
            observer.onChanged(Lce.Loading)
            submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken, isPrivateJourney = true, testKit = "LAB_RESULT")
            observer.onChanged(Lce.Error(throwable))
        }
    }

    @Test
    fun `success case keySharingInfo isSelfReporting returns true`() = runBlocking {
        coEvery { keySharingInfoProvider.keySharingInfo } returns keySharingInfo.copy(isSelfReporting = true)
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken, isPrivateJourney = true, testKit = "LAB_RESULT") } returns
                Result.Success(Unit)

        testSubject.submitKeys()

        coVerifyOrder {
            observer.onChanged(Lce.Loading)
            submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken, isPrivateJourney = true, testKit = "LAB_RESULT")
            observer.onChanged(Lce.Success(Unit))
        }
    }
}
