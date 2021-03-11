package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.ParseJsonError
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid

class PostCodeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val localAuthorityPostCodeValidator = mockk<LocalAuthorityPostCodeValidator>()

    private val testSubject = PostCodeViewModel(localAuthorityPostCodeValidator)

    private val postCodeValidationViewState =
        mockk<Observer<LocalAuthorityPostCodeValidationResult>>(relaxUnitFun = true)

    private val postCode = "CM1"

    @Test
    fun `valid post code in validate`() = runBlocking {
        testSubject.postCodeValidationResult().observeForever(postCodeValidationViewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Valid(postCode, emptyList())

        testSubject.validateMainPostCode(postCode)

        verify { postCodeValidationViewState.onChanged(Valid(postCode, emptyList())) }
    }

    @Test
    fun `invalid post code in validate`() = runBlocking {
        testSubject.postCodeValidationResult().observeForever(postCodeValidationViewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Invalid

        testSubject.validateMainPostCode(postCode)

        verify { postCodeValidationViewState.onChanged(Invalid) }
    }

    @Test
    fun `invalid post code in validate due to json error`() = runBlocking {
        testSubject.postCodeValidationResult().observeForever(postCodeValidationViewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns ParseJsonError

        testSubject.validateMainPostCode(postCode)

        verify { postCodeValidationViewState.onChanged(ParseJsonError) }
    }

    @Test
    fun `unsupported post code in validate`() = runBlocking {
        testSubject.postCodeValidationResult().observeForever(postCodeValidationViewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Unsupported

        testSubject.validateMainPostCode(postCode)

        verify { postCodeValidationViewState.onChanged(Unsupported) }
    }
}
