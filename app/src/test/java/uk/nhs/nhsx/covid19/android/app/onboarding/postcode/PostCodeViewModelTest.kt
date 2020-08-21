package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor

class PostCodeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeValidator = mockk<PostCodeValidator>()
    private val analyticsManager = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val postCodeViewState =
        mockk<Observer<PostCodeViewModel.PostCodeViewState>>(relaxed = true)

    private val postCodePrefs = mockk<PostCodeProvider>(relaxed = true)

    private val testSubject = PostCodeViewModel(postCodeValidator, postCodePrefs, analyticsManager)

    @Test
    fun `valid post code`() = runBlocking {
        testSubject.viewState().observeForever(postCodeViewState)

        coEvery { postCodeValidator.validate(any()) } returns true

        val postCode = "CM1"
        testSubject.validate(postCode)

        verify { postCodeViewState.onChanged(PostCodeViewModel.PostCodeViewState.Valid) }
    }

    @Test
    fun `invalid post code`() = runBlocking {
        testSubject.viewState().observeForever(postCodeViewState)

        coEvery { postCodeValidator.validate(any()) } returns false

        val postCode = "CM1"
        testSubject.validate(postCode)

        verify { postCodeViewState.onChanged(PostCodeViewModel.PostCodeViewState.Invalid) }
    }
}
