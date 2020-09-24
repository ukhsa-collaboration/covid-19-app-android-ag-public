package uk.nhs.nhsx.covid19.android.app.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey

class ShareKeysInformationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fetchTemporaryExposureKeys = mockk<FetchTemporaryExposureKeys>(relaxed = true)

    private val testSubject = ShareKeysInformationViewModel(fetchTemporaryExposureKeys)

    private val fetchExposureKeysObserver = mockk<Observer<TemporaryExposureKeysFetchResult>>(relaxed = true)

    private val exposureKeys = listOf(
        NHSTemporaryExposureKey("key1", 1),
        NHSTemporaryExposureKey("key2", 2)
    )

    @Test
    fun `fetching keys returns list of exposure keys on success`() = runBlocking {
        coEvery { fetchTemporaryExposureKeys.invoke() } returns Success(exposureKeys)

        testSubject.fetchKeysResult().observeForever(fetchExposureKeysObserver)

        testSubject.fetchKeys()

        coVerify { fetchTemporaryExposureKeys.invoke() }
        verify { fetchExposureKeysObserver.onChanged(Success(exposureKeys)) }
    }
}
