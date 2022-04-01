package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
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
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewWalesJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.QuestionnaireJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ContinueIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ExposureNotificationAgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.NewContactJourney
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
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
            checkViewStateOfIsolationAndTestingAdvice(
                isActiveIndexCase = false,
                contactJourney = NewWalesJourney(
                    encounterDate = expectedEncounterDate
                )
            )
        }
    }

    @Test
    fun `when encounter date is present and not in active index case isolation then update view state to show isolation and testing advice for England`() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            checkViewStateOfIsolationAndTestingAdvice(
                isActiveIndexCase = false,
                contactJourney = NewEnglandJourney(encounterDate = expectedEncounterDate)
            )
        }
    }

    @Test
    fun `when encounter date is present and not in active index case isolation then update view state to show questionnaire for England`() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW, true) {
            checkViewStateOfIsolationAndTestingAdvice(
                isActiveIndexCase = false,
                contactJourney = QuestionnaireJourney(encounterDate = expectedEncounterDate, true)
            )
        }
    }

    @Test
    fun `when encounter date is present and not in active index case isolation then update view state to show questionnaire for Wales`() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW, true) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
            checkViewStateOfIsolationAndTestingAdvice(
                isActiveIndexCase = false,
                contactJourney = QuestionnaireJourney(encounterDate = expectedEncounterDate, true)
            )
        }
    }

    @Test
    fun `when encounter date is present and in active index case isolation then update view state to show isolation and testing advice for Wales`() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
            checkViewStateOfIsolationAndTestingAdvice(
                isActiveIndexCase = true,
                contactJourney = NewWalesJourney(
                    encounterDate = expectedEncounterDate
                )
            )
        }
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
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(ContinueIsolation) }
        }
    }

    @Test
    fun `opts-out of contact isolation and acknowledges exposure notification on primary button click when user is in Wales and is already isolating as index case`() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(ContinueIsolation) }
        }
    }

    @Test
    fun `opens new England advice on primary button click when user is in England`() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns false

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(NewContactJourney) }
        }
    }

    @Test
    fun `opens new England advice on primary button click when user is in Wales`() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns false

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(NewContactJourney) }
        }
    }

    @Test
    fun `opens age question screen on primary button click when feature flag is disabled`() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW, true) {
            coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

            testSubject.onPrimaryButtonClick()

            verify { navigationTargetObserver.onChanged(ExposureNotificationAgeLimit) }
        }
    }

    @Test
    fun `opens age question screen on primary button click and new contact case journey feature flag is disabled`() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
