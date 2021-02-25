package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.IsolationState
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.UserDataState
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.VenueVisitsUiState
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthority
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodes
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Instant
import java.time.LocalDate

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

    private val userDataStateObserver = mockk<Observer<UserDataState>>(relaxed = true)
    private val venueVisitsEditModeChangedObserver = mockk<Observer<Boolean>>(relaxed = true)
    private val allUserDataDeletedObserver = mockk<Observer<Unit>>(relaxed = true)

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(DAILY_CONTACT_TESTING)

        testSubject.userDataState().observeForever(userDataStateObserver)
        testSubject.venueVisitsEditModeChanged().observeForever(venueVisitsEditModeChangedObserver)
        testSubject.getAllUserDataDeleted().observeForever(allUserDataDeletedObserver)

        every { postCodeProvider.value } returns postCode
        every { relevantTestResultProvider.testResult } returns acknowledgedTestResult
        coEvery { venuesStorage.getVisits() } returns listOf()
        every { localAuthorityProvider.value } returns localAuthorityId
        coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `onResume triggers view state emission`() = runBlocking {
        every { stateMachine.readState() } returns contactCaseOnlyIsolation

        testSubject.onResume()

        verify { userDataStateObserver.onChanged(expectedInitialUserDataState) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `onResume with no changes to view state does not trigger view state emission`() = runBlocking {
        every { stateMachine.readState() } returns contactCaseOnlyIsolation

        testSubject.onResume()
        testSubject.onResume()

        verify(exactly = 1) { userDataStateObserver.onChanged(any()) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `delete removes data from storage`() {
        every { stateMachine.readState() } returns contactCaseOnlyIsolation

        testSubject.onResume()

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
        every { stateMachine.readState() } returns contactCaseOnlyIsolation

        testSubject.onResume()

        testSubject.deleteVenueVisit(0)

        coVerifyOrder {
            userDataStateObserver.onChanged(expectedInitialUserDataState)
            venuesStorage.removeVenueVisit(0)
            userDataStateObserver.onChanged(
                expectedInitialUserDataState.copy(
                    venueVisitsUiState = VenueVisitsUiState(listOf(), isInEditMode = true),
                    showDialog = null
                )
            )
        }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `clicking edit and done changes delete state`() {
        every { stateMachine.readState() } returns contactCaseOnlyIsolation

        testSubject.onResume()
        testSubject.onEditVenueVisitClicked()
        testSubject.onEditVenueVisitClicked()

        coVerifyOrder {
            userDataStateObserver.onChanged(expectedInitialUserDataState)
            userDataStateObserver.onChanged(
                expectedInitialUserDataState.copy(
                    venueVisitsUiState = VenueVisitsUiState(listOf(), isInEditMode = true),
                    showDialog = null
                )
            )
            venueVisitsEditModeChangedObserver.onChanged(true)
            userDataStateObserver.onChanged(
                expectedInitialUserDataState.copy(
                    venueVisitsUiState = VenueVisitsUiState(listOf(), isInEditMode = false),
                    showDialog = null
                )
            )
            venueVisitsEditModeChangedObserver.onChanged(false)
        }
    }

    @Test
    fun `loading user data only returns main post code when local authority is not stored`() {
        every { stateMachine.readState() } returns contactCaseOnlyIsolation
        every { localAuthorityProvider.value } returns null
        every { postCodeProvider.value } returns postCode

        testSubject.onResume()

        verify {
            userDataStateObserver.onChanged(expectedInitialUserDataState.copy(localAuthority = postCode))
        }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `loading user data returns exposure notification details and dailyContactTestingOptInDate when previously in contact case`() {
        every { stateMachine.readState() } returns Default(previousIsolation = contactCaseOnlyIsolation)

        testSubject.onResume()

        verify {
            userDataStateObserver.onChanged(
                expectedInitialUserDataState.copy(
                    isolationState = IsolationState(
                        contactCaseEncounterDate = contactCaseEncounterDate,
                        contactCaseNotificationDate = contactCaseNotificationDate,
                        dailyContactTestingOptInDate = dailyContactTestingOptInDate
                    )
                )
            )
        }
    }

    @Test
    fun `loading user data doesn't return exposure notification details and dailyContactTestingOptInDate when previously in contact case`() {
        every { stateMachine.readState() } returns Default()

        testSubject.onResume()

        verify {
            userDataStateObserver.onChanged(expectedInitialUserDataState.copy(isolationState = null))
        }
    }

    private val postCode = "CM1"
    private val localAuthorityId = "SE00001"
    private val postCodeLocalAuthorities = listOf(localAuthorityId)
    private val localAuthority = LocalAuthority(name = "Something", country = "Somewhere")

    private val localAuthorityPostCodes = LocalAuthorityPostCodes(
        postcodes = mapOf(postCode to postCodeLocalAuthorities),
        localAuthorities = mapOf(postCodeLocalAuthorities[0] to localAuthority)
    )

    private val acknowledgedTestResult = AcknowledgedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = Instant.now(),
        testResult = POSITIVE,
        acknowledgedDate = Instant.now(),
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false,
        confirmedDate = null
    )

    private val contactCaseEncounterDate = Instant.parse("2020-05-19T12:00:00Z")

    private val contactCaseNotificationDate = Instant.parse("2020-05-20T12:00:00Z")

    private val dailyContactTestingOptInDate = LocalDate.now().plusDays(5)

    private val contactCaseOnlyIsolation = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = contactCaseEncounterDate,
            notificationDate = contactCaseNotificationDate,
            expiryDate = LocalDate.now().plusDays(5),
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        )
    )

    private val expectedInitialUserDataState = UserDataState(
        localAuthority = "${localAuthority.name}\n$postCode",
        isolationState = IsolationState(
            lastDayOfIsolation = contactCaseOnlyIsolation.lastDayOfIsolation,
            contactCaseEncounterDate = contactCaseEncounterDate,
            contactCaseNotificationDate = contactCaseNotificationDate,
            indexCaseSymptomOnsetDate = contactCaseOnlyIsolation.indexCase?.symptomsOnsetDate,
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        ),
        venueVisitsUiState = VenueVisitsUiState(listOf(), isInEditMode = false),
        acknowledgedTestResult = acknowledgedTestResult,
        showDialog = null
    )
}
