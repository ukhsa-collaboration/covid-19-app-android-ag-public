package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag.NEW_ENGLAND_CONTACT_CASE_JOURNEY
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewEnglandJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.QuestionnaireJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ContinueIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ExposureNotificationAgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.NewEnglandContactAdvice
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.utils.CoroutineTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExposureNotificationViewModelTest : CoroutineTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val getRiskyContactEncounterDate = mockk<GetRiskyContactEncounterDate>()
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val expectedEncounterDate: LocalDate = mockk(relaxUnitFun = true)

    private val contactJourneyObserver = mockk<Observer<ContactJourney>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val finishActivityObserver = mockk<Observer<Void>>(relaxUnitFun = true)
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
    private val optOutOfContactIsolation = mockk<OptOutOfContactIsolation>(relaxUnitFun = true)
    private val acknowledgeRiskyContact = mockk<AcknowledgeRiskyContact>(relaxUnitFun = true)
    private val testSubject = ExposureNotificationViewModel(
        getRiskyContactEncounterDate,
        isolationStateMachine,
        fixedClock,
        localAuthorityPostCodeProvider,
        optOutOfContactIsolation,
        acknowledgeRiskyContact
    )

    @Before
    fun setUp() {
        testSubject.contactJourney.observeForever(contactJourneyObserver)
        testSubject.finishActivity.observeForever(finishActivityObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        every { getRiskyContactEncounterDate() } returns expectedEncounterDate
    }

    @Test
    fun `when encounter date is present and not in active index case isolation then update view state to show isolation and testing advice for Wales`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        checkViewStateOfIsolationAndTestingAdvice(
            isActiveIndexCase = false,
            contactJourney = QuestionnaireJourney(
                encounterDate = expectedEncounterDate,
                shouldShowTestingAndIsolationAdvice = true
            )
        )
    }

    @Test
    fun `when encounter date is present and not in active index case isolation then update view state to show isolation and testing advice for England`() {
        checkViewStateOfIsolationAndTestingAdvice(
            isActiveIndexCase = false,
            contactJourney = NewEnglandJourney(encounterDate = expectedEncounterDate)
        )
    }

    @Test
    fun `when encounter date is present and in active index case isolation then update view state to hide isolation and testing advice for Wales`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        checkViewStateOfIsolationAndTestingAdvice(
            isActiveIndexCase = true,
            contactJourney = QuestionnaireJourney(
                encounterDate = expectedEncounterDate,
                shouldShowTestingAndIsolationAdvice = false
            )
        )
    }

    @Test
    fun `when encounter date is present and in active index case isolation then update view state to hide isolation and testing advice for England`() {
        checkViewStateOfIsolationAndTestingAdvice(
            isActiveIndexCase = true,
            contactJourney = NewEnglandJourney(encounterDate = expectedEncounterDate)
        )
    }

    @Test
    fun `finishes activity when encounter date not present`() {
        every { getRiskyContactEncounterDate() } returns null

        testSubject.updateViewState()

        verify { finishActivityObserver.onChanged(null) }
    }

    @Test
    fun `opts-out of contact isolation and acknowledges exposure notification on primary button click when user is in England and is already isolating as index case`() {
        runWithFeatureEnabled(NEW_ENGLAND_CONTACT_CASE_JOURNEY) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(ContinueIsolation) }
        }
    }

    @Test
    fun `opens new England advice on primary button click when user is in England`() {
        runWithFeatureEnabled(NEW_ENGLAND_CONTACT_CASE_JOURNEY) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns false

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(NewEnglandContactAdvice) }
        }
    }

    @Test
    fun `opens age question screen on primary button click when user is in Wales`() {
        runWithFeatureEnabled(NEW_ENGLAND_CONTACT_CASE_JOURNEY) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(ExposureNotificationAgeLimit) }
        }
    }

    @Test
    fun `opens age question screen on primary button click when user is in England and new contact case journey is disabled`() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(ExposureNotificationAgeLimit) }
        }
    }

    private fun checkViewStateOfIsolationAndTestingAdvice(
        isActiveIndexCase: Boolean,
        contactJourney: ContactJourney
    ) = runBlockingTest {
        every { isolationStateMachine.readLogicalState().isActiveIndexCase(fixedClock) } returns isActiveIndexCase

        testSubject.updateViewState()
        verify { contactJourneyObserver.onChanged(contactJourney) }
    }
}
