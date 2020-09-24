package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.INVALID_POST_DISTRICT
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.SUCCESS

class PostCodeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeViewState =
        mockk<Observer<PostCodeUpdateState>>(relaxed = true)

    private val postCodeValidation = mockk<PostCodeUpdater>(relaxed = true)

    private val testSubject = PostCodeViewModel(postCodeValidation)

    @Test
    fun `valid post code`() = runBlocking {
        testSubject.viewState().observeForever(postCodeViewState)

        coEvery { postCodeValidation.update(any()) } returns SUCCESS

        val postCode = "CM1"
        testSubject.updateMainPostCode(postCode)

        verify { postCodeViewState.onChanged(SUCCESS) }
    }

    @Test
    fun `invalid post code`() = runBlocking {
        testSubject.viewState().observeForever(postCodeViewState)

        coEvery { postCodeValidation.update(any()) } returns INVALID_POST_DISTRICT

        val postCode = "CM1"
        testSubject.updateMainPostCode(postCode)

        verify { postCodeViewState.onChanged(INVALID_POST_DISTRICT) }
    }
}
