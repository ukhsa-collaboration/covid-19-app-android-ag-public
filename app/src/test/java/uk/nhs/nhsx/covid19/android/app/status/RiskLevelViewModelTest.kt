package uk.nhs.nhsx.covid19.android.app.status

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES

class RiskLevelViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private val testSubject = RiskLevelViewModel(localAuthorityPostCodeProvider)

    private val showMassTestingObserver = mockk<Observer<Boolean>>(relaxed = true)

    @Before
    fun setUp() {
        testSubject.showMassTesting().observeForever(showMassTestingObserver)
    }

    @Test
    fun `return true when post district is in England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onHandleRiskLevel()

        verify { showMassTestingObserver.onChanged(true) }
    }

    @Test
    fun `return false when post district is not in England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onHandleRiskLevel()

        verify { showMassTestingObserver.onChanged(false) }
    }
}
