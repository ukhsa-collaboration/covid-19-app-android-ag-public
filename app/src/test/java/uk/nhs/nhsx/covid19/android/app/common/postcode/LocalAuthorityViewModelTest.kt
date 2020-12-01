package uk.nhs.nhsx.covid19.android.app.common.postcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.UpdateAreaRisk
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NOT_SELECTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NOT_SUPPORTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NO_ERROR
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.status.RiskyPostCodeIndicatorProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAuthorityViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val localAuthorityPostCodeValidator = mockk<LocalAuthorityPostCodeValidator>(relaxed = true)
    private val localAuthorityProvider = mockk<LocalAuthorityProvider>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val riskyPostCodeIndicatorProvider = mockk<RiskyPostCodeIndicatorProvider>(relaxed = true)
    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>(relaxed = true)
    private val updateAreaRisk = mockk<UpdateAreaRisk>(relaxed = true)

    private val localAuthorities = mockk<Observer<List<LocalAuthorityWithId>>>(relaxed = true)
    private val viewState = mockk<Observer<ViewState>>(relaxed = true)
    private val finishActivity = mockk<Observer<Void>>(relaxed = true)

    private val testSubject = LocalAuthorityViewModel(
        localAuthorityPostCodeValidator,
        localAuthorityProvider,
        postCodeProvider,
        riskyPostCodeIndicatorProvider,
        onboardingCompletedProvider,
        updateAreaRisk
    )

    private val postCode = "CM1"

    private val localAuthorityWithId = LocalAuthorityWithId(
        id = "1",
        localAuthority = LocalAuthority(
            name = "Something",
            country = "Country"
        )
    )

    private val localAuthoritiesWithId = listOf(
        LocalAuthorityWithId(
            id = "2",
            localAuthority = LocalAuthority(
                name = "Some other thing",
                country = "Another country"
            )
        ),
        localAuthorityWithId
    )

    @Before
    fun setUp() {
        mockkStatic("uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoaderKt")
        testSubject.postCode = postCode
    }

    @Test
    fun `initialize postcode with non null argument`() {
        val actual = testSubject.initializePostCode(postCode)

        assertTrue { actual }
        assertEquals(postCode, testSubject.postCode)
    }

    @Test
    fun `initialize postcode with null argument`() {
        every { postCodeProvider.value } returns postCode

        val actual = testSubject.initializePostCode(null)

        assertTrue { actual }
        assertEquals(postCode, testSubject.postCode)
    }

    @Test
    fun `not possible to initialize`() {
        every { postCodeProvider.value } returns null

        val actual = testSubject.initializePostCode(null)

        assertFalse { actual }
    }

    @Test
    fun `load single local authority for valid postcode`() = runBlocking {
        testSubject.localAuthorities().observeForever(localAuthorities)
        testSubject.viewState().observeForever(viewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Valid(
            postCode,
            listOf(localAuthorityWithId)
        )

        testSubject.loadLocalAuthorities()

        assertEquals(localAuthorityWithId.id, testSubject.selectedLocalAuthorityId)
        verify {
            localAuthorities.onChanged(listOf(localAuthorityWithId))
            viewState.onChanged(
                ViewState(
                    localAuthorityId = localAuthorityWithId.id,
                    errorState = NO_ERROR
                )
            )
        }
    }

    @Test
    fun `load multiple local authorities for valid postcode`() = runBlocking {
        testSubject.localAuthorities().observeForever(localAuthorities)
        testSubject.viewState().observeForever(viewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Valid(postCode, emptyList())

        testSubject.loadLocalAuthorities()

        verify {
            localAuthorities.onChanged(emptyList())
            viewState.onChanged(
                ViewState(
                    localAuthorityId = null,
                    errorState = NO_ERROR
                )
            )
        }
    }

    @Test
    fun `load multiple local authorities for invalid postcode`() = runBlocking {
        testSubject.localAuthorities().observeForever(localAuthorities)
        testSubject.viewState().observeForever(viewState)

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Invalid

        testSubject.loadLocalAuthorities()

        verify(exactly = 0) {
            localAuthorities.onChanged(any())
            viewState.onChanged(
                ViewState(
                    localAuthorityId = null,
                    errorState = NOT_SUPPORTED
                )
            )
        }
    }

    @Test
    fun `sort local authorities by name`() = runBlocking {
        testSubject.localAuthorities().observeForever(localAuthorities)

        val first = LocalAuthorityWithId(
            id = "2",
            localAuthority = LocalAuthority(
                name = "ABC",
                country = "En"
            )
        )

        val second = LocalAuthorityWithId(
            id = "1",
            localAuthority = LocalAuthority(
                name = "XYZ",
                country = "En"
            )
        )

        coEvery { localAuthorityPostCodeValidator.validate(any()) } returns Valid(
            "AB",
            listOf(
                second,
                first
            )
        )

        testSubject.loadLocalAuthorities()

        verify {
            localAuthorities.onChanged(
                listOf(
                    first,
                    second
                )
            )
        }
    }

    @Test
    fun `select local authority in supported country`() {
        testSubject.viewState().observeForever(viewState)

        testSubject.localAuthorities.value = localAuthoritiesWithId
        every { localAuthorityWithId.localAuthority.supported() } returns true

        testSubject.selectLocalAuthority(localAuthorityWithId.id)

        assertEquals(localAuthorityWithId.id, testSubject.selectedLocalAuthorityId)
        verify {
            viewState.onChanged(
                ViewState(
                    localAuthorityId = localAuthorityWithId.id,
                    errorState = NO_ERROR
                )
            )
        }
    }

    @Test
    fun `select local authority in unsupported country`() {
        testSubject.viewState().observeForever(viewState)

        testSubject.localAuthorities.value = localAuthoritiesWithId
        every { localAuthorityWithId.localAuthority.supported() } returns false

        testSubject.selectLocalAuthority(localAuthorityWithId.id)

        assertEquals(localAuthorityWithId.id, testSubject.selectedLocalAuthorityId)
        verify {
            viewState.onChanged(
                ViewState(
                    localAuthorityId = localAuthorityWithId.id,
                    errorState = NOT_SUPPORTED
                )
            )
        }
    }

    @Test
    fun `confirm local authority when no authority is selected`() {
        testSubject.viewState().observeForever(viewState)
        testSubject.finishActivity().observeForever(finishActivity)

        testSubject.confirmLocalAuthority()

        verify {
            viewState.onChanged(
                ViewState(
                    localAuthorityId = null,
                    errorState = NOT_SELECTED
                )
            )
        }
        verify(exactly = 0) { finishActivity.onChanged(null) }
    }

    @Test
    fun `confirm local authority when unsupported authority is selected`() {
        testSubject.viewState().observeForever(viewState)
        testSubject.finishActivity().observeForever(finishActivity)

        testSubject.localAuthorities.value = localAuthoritiesWithId
        testSubject.selectedLocalAuthorityId = localAuthorityWithId.id
        every { localAuthorityWithId.localAuthority.supported() } returns false

        testSubject.confirmLocalAuthority()

        verify {
            viewState.onChanged(
                ViewState(
                    localAuthorityId = localAuthorityWithId.id,
                    errorState = NOT_SUPPORTED
                )
            )
        }
        verify(exactly = 0) { finishActivity.onChanged(null) }
    }

    @Test
    fun `confirm local authority when onboarding is completed`() {
        testSubject.finishActivity().observeForever(finishActivity)

        every { onboardingCompletedProvider.value } returns true
        every { localAuthorityWithId.localAuthority.supported() } returns true
        testSubject.localAuthorities.value = localAuthoritiesWithId
        testSubject.selectedLocalAuthorityId = localAuthorityWithId.id

        testSubject.confirmLocalAuthority()

        verifyOrder {
            postCodeProvider setProperty "value" value eq(postCode)
            localAuthorityProvider setProperty "value" value eq(localAuthorityWithId.id)
            riskyPostCodeIndicatorProvider.clear()
            updateAreaRisk.schedule()
            finishActivity.onChanged(null)
        }
    }

    @Test
    fun `confirm local authority when onboarding is not completed`() {
        testSubject.finishActivity().observeForever(finishActivity)

        every { onboardingCompletedProvider.value } returns null
        every { localAuthorityWithId.localAuthority.supported() } returns true
        testSubject.localAuthorities.value = localAuthoritiesWithId
        testSubject.selectedLocalAuthorityId = localAuthorityWithId.id

        testSubject.confirmLocalAuthority()

        verifyOrder {
            postCodeProvider setProperty "value" value eq(postCode)
            localAuthorityProvider setProperty "value" value eq(localAuthorityWithId.id)
            riskyPostCodeIndicatorProvider.clear()
            finishActivity.onChanged(null)
        }
        verify(exactly = 0) { updateAreaRisk.schedule() }
    }
}
