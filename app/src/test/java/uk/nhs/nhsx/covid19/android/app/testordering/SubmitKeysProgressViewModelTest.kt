package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.IOException

internal class SubmitKeysProgressViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val submitTemporaryExposureKeys = mockk<SubmitTemporaryExposureKeys>()
    private val exposureKeys = listOf<NHSTemporaryExposureKey>()
    private val diagnoseKeySubmissionToken = ""

    private val testSubject =
        SubmitKeysProgressViewModel(
            submitTemporaryExposureKeys,
            exposureKeys,
            diagnoseKeySubmissionToken
        )

    private val observer = mockk<Observer<Lce<Unit>>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        testSubject.submitKeysResult().observeForever(observer)
    }

    @Test
    fun `error case`() = runBlocking {
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken) } returns
                Result.Failure(throwable)

        testSubject.submitKeys()

        verifyOrder {
            observer.onChanged(Lce.Loading)
            observer.onChanged(Lce.Error(throwable))
        }
    }

    @Test
    fun `success case`() = runBlocking {
        coEvery { submitTemporaryExposureKeys(exposureKeys, diagnoseKeySubmissionToken) } returns
                Result.Success(Unit)

        testSubject.submitKeys()

        verifyOrder {
            observer.onChanged(Lce.Loading)
            observer.onChanged(Lce.Success(Unit))
        }
    }
}
