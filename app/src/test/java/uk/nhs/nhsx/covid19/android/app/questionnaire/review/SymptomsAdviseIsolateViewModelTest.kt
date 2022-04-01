package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIndexCaseThenIsolationDueToSelfAssessmentAdvice.AdviceForEngland
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIndexCaseThenIsolationDueToSelfAssessmentAdvice.AdviceForWales
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviseIsolateViewModel.ViewState

class SymptomsAdviseIsolateViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private lateinit var viewModel: SymptomsAdviseIsolateViewModel

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        viewModel = SymptomsAdviseIsolateViewModel(localAuthorityPostCodeProvider)
        viewModel.viewState().observeForever(viewStateObserver)
    }

    @Test
    fun `verify view state is updated when local authority is England`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        viewModel.handleLocalAuthorityAdvice()
        verify { viewStateObserver.onChanged(ViewState(AdviceForEngland)) }
    }

    @Test
    fun `verify view state is updated when local authority is Wales`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        viewModel.handleLocalAuthorityAdvice()
        verify { viewStateObserver.onChanged(ViewState(AdviceForWales)) }
    }
}
