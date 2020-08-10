package uk.nhs.nhsx.covid19.android.app.onboarding.authentication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Invalid
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Valid

class AuthenticationCodeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val authenticationCodeValidator = mockk<AuthenticationCodeValidator>()
    private val authenticationCodeProvider = mockk<AuthenticationProvider>(relaxed = true)

    private val testSubject = AuthenticationCodeViewModel(authenticationCodeProvider, authenticationCodeValidator)

    private val authenticationLiveDataObserver = mockk<Observer<AuthCodeViewState>>(relaxed = true)

    @Test
    fun `validate returns valid`() = runBlocking {
        testSubject.viewState().observeForever(authenticationLiveDataObserver)

        val authCode = "12345678"
        coEvery { authenticationCodeValidator.validate(authCode) } returns true

        testSubject.validate(authCode)

        verify { authenticationLiveDataObserver.onChanged(Valid) }
    }

    @Test
    fun `validate returns invalid`() = runBlocking {
        testSubject.viewState().observeForever(authenticationLiveDataObserver)

        val authCode = "12"
        coEvery { authenticationCodeValidator.validate(authCode) } returns false

        testSubject.validate(authCode)

        verify { authenticationLiveDataObserver.onChanged(Invalid) }
    }
}
