package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.VenueVisitsUiState
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthority
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodes
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE

class UserDataViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val localAuthorityProvider = mockk<LocalAuthorityProvider>(relaxed = true)
    private val venuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val localAuthorityPostCodesLoader = mockk<LocalAuthorityPostCodesLoader>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>()
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>()
    private val sharedPreferencesDeletedDataEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val submittedOnboardingAnalyticsProvider = mockk<SubmittedOnboardingAnalyticsProvider>(relaxed = true)

    private val testSubject = UserDataViewModel(
        postCodeProvider,
        localAuthorityProvider,
        venuesStorage,
        stateMachine,
        relevantTestResultProvider,
        sharedPreferences,
        localAuthorityPostCodesLoader,
        submittedOnboardingAnalyticsProvider
    )

    private val localAuthorityTextObserver = mockk<Observer<String>>(relaxed = true)
    private val venueVisitsObserver = mockk<Observer<VenueVisitsUiState>>(relaxed = true)
    private val venueVisitsEditModeChangedObserver = mockk<Observer<Boolean>>(relaxed = true)
    private val stateMachineStateObserver = mockk<Observer<State>>(relaxed = true)
    private val latestTestResultObserver = mockk<Observer<AcknowledgedTestResult>>(relaxed = true)
    private val allUserDataDeletedObserver = mockk<Observer<Unit>>(relaxed = true)

    private val postCode = "CM1"
    private val localAuthorityId = "SE00001"
    private val postCodeLocalAuthorities = listOf(localAuthorityId)
    private val localAuthority = LocalAuthority(name = "Something", country = "Somewhere")

    private val localAuthorityPostCodes = LocalAuthorityPostCodes(
        postcodes = mapOf(postCode to postCodeLocalAuthorities),
        localAuthorities = mapOf(postCodeLocalAuthorities[0] to localAuthority)
    )

    @Before
    fun setUp() {
        coEvery { venuesStorage.getVisits() } returns listOf()
        coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes
    }

    @Test
    fun `venue visits updated`() = runBlocking {
        testSubject.getVenueVisitsUiState().observeForever(venueVisitsObserver)
        testSubject.venueVisitsEditModeChanged().observeForever(venueVisitsEditModeChangedObserver)

        testSubject.loadUserData()

        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = false)) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
    }

    @Test
    fun `status machine state updated`() = runBlocking {
        coEvery { stateMachine.readState() } returns State.Default()

        testSubject.getLastStatusMachineState().observeForever(stateMachineStateObserver)

        testSubject.loadUserData()

        verify { stateMachineStateObserver.onChanged(State.Default()) }
    }

    @Test
    fun `latest test result state updated`() = runBlocking {
        val latestTestResult = AcknowledgedTestResult(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(),
            testResult = POSITIVE,
            acknowledgedDate = Instant.now(),
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )

        every { relevantTestResultProvider.testResult } returns latestTestResult

        testSubject.getAcknowledgedTestResult().observeForever(latestTestResultObserver)

        testSubject.loadUserData()

        verify { latestTestResultObserver.onChanged(latestTestResult) }
    }

    @Test
    fun `delete removes data from storage`() {
        testSubject.getAllUserDataDeleted().observeForever(allUserDataDeletedObserver)

        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor
        every { submittedOnboardingAnalyticsProvider.value } returns true

        testSubject.deleteAllUserData()

        verify { venuesStorage.removeAllVenueVisits() }
        verify { sharedPreferencesEditor.clear() }
        verify { sharedPreferencesDeletedDataEditor.apply() }
        verify { submittedOnboardingAnalyticsProvider setProperty "value" value eq(true) }
        verify { allUserDataDeletedObserver.onChanged(Unit) }
    }

    @Test
    fun `delete single venue visit removes it from storage`() = runBlocking {
        testSubject.getVenueVisitsUiState().observeForever(venueVisitsObserver)
        testSubject.venueVisitsEditModeChanged().observeForever(venueVisitsEditModeChangedObserver)

        testSubject.deleteVenueVisit(0)

        coVerify { venuesStorage.removeVenueVisit(0) }
        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = false)) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
    }

    @Test
    fun `clicking edit and done changes delete state`() {
        testSubject.getVenueVisitsUiState().observeForever(venueVisitsObserver)
        testSubject.venueVisitsEditModeChanged().observeForever(venueVisitsEditModeChangedObserver)

        testSubject.onEditVenueVisitClicked()

        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = true)) }
        verify { venueVisitsEditModeChangedObserver.onChanged(true) }

        testSubject.onEditVenueVisitClicked()

        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = false)) }
        verify { venueVisitsEditModeChangedObserver.onChanged(false) }
    }

    @Test
    fun `loading user data doesn't change edit mode state`() {
        testSubject.getVenueVisitsUiState().observeForever(venueVisitsObserver)
        testSubject.venueVisitsEditModeChanged().observeForever(venueVisitsEditModeChangedObserver)

        testSubject.loadUserData()

        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = false)) }

        testSubject.onEditVenueVisitClicked()

        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = true)) }

        testSubject.loadUserData()

        verify { venueVisitsObserver.onChanged(VenueVisitsUiState(listOf(), isInEditMode = true)) }

        verify(exactly = 1) { venueVisitsEditModeChangedObserver.onChanged(true) }
    }

    @Test
    fun `loading user data returns local authority and main post code when local authority is stored`() {
        testSubject.localAuthorityText().observeForever(localAuthorityTextObserver)

        every { localAuthorityProvider.value } returns localAuthorityId
        every { postCodeProvider.value } returns postCode

        testSubject.loadUserData()

        verify { localAuthorityTextObserver.onChanged("${localAuthority.name}\n$postCode") }
    }

    @Test
    fun `loading user data only returns main post code when local authority is not stored`() {
        testSubject.localAuthorityText().observeForever(localAuthorityTextObserver)

        every { localAuthorityProvider.value } returns null
        every { postCodeProvider.value } returns postCode

        testSubject.loadUserData()

        verify { localAuthorityTextObserver.onChanged(postCode) }
    }
}
