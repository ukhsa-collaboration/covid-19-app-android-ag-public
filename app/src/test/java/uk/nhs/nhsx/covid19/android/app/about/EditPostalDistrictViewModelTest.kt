package uk.nhs.nhsx.covid19.android.app.about

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
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid

class EditPostalDistrictViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val localAuthorityPostCodeValidator = mockk<LocalAuthorityPostCodeValidator>()

    private val testSubject = EditPostalDistrictViewModel(localAuthorityPostCodeValidator)

    private val postCodeValidationViewState =
        mockk<Observer<LocalAuthorityPostCodeValidationResult>>(relaxUnitFun = true)

    private val postCode = "CM1"

    @Test
    fun `validate post code success`() = runBlocking {
        testSubject.postCodeValidationResult().observeForever(postCodeValidationViewState)

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Valid(postCode, emptyList())

        testSubject.validatePostCode(postCode)

        verify { postCodeValidationViewState.onChanged(Valid(postCode, emptyList())) }
    }

    @Test
    fun `validate unsupported post code`() = runBlocking {
        testSubject.postCodeValidationResult().observeForever(postCodeValidationViewState)

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Unsupported

        testSubject.validatePostCode(postCode)

        verify { postCodeValidationViewState.onChanged(Unsupported) }
    }
}
