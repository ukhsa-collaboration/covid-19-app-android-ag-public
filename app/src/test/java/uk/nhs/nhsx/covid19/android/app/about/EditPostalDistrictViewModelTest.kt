package uk.nhs.nhsx.covid19.android.app.about

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.InvalidPostDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.Success

class EditPostalDistrictViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val updateAreaRisk = mockk<UpdateAreaRisk>(relaxed = true)
    private val postCodeValidation = mockk<PostCodeUpdater>(relaxed = true)
    private val localAuthorityPostCodeValidator = mockk<LocalAuthorityPostCodeValidator>(relaxed = true)

    private val testSubject =
        EditPostalDistrictViewModel(postCodeValidation, updateAreaRisk, localAuthorityPostCodeValidator)

    private val postCodeUpdateViewState = mockk<Observer<PostCodeUpdateState>>(relaxed = true)
    private val postCodeValidationViewState = mockk<Observer<LocalAuthorityPostCodeValidationResult>>(relaxed = true)

    private val postCode = "CM1"

    @Test
    fun `update post code if valid`() = runBlocking {
        testSubject.postCodeUpdateState().observeForever(postCodeUpdateViewState)

        coEvery { postCodeValidation.update(any()) } returns Success("SE1")

        testSubject.updatePostCode(postCode)

        coVerify { updateAreaRisk.schedule() }
        verify { postCodeUpdateViewState.onChanged(Success("SE1")) }
    }

    @Test
    fun `do not update post code if invalid post code`() = runBlocking {
        testSubject.postCodeUpdateState().observeForever(postCodeUpdateViewState)

        coEvery { postCodeValidation.update(any()) } returns InvalidPostDistrict

        testSubject.updatePostCode(postCode)

        coVerify(exactly = 0) { updateAreaRisk.schedule() }
        verify { postCodeUpdateViewState.onChanged(InvalidPostDistrict) }
    }

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
