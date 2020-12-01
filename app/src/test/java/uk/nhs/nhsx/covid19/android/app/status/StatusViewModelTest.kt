package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import java.time.Instant
import java.time.LocalDate

class StatusViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val postCodeIndicatorProvider = mockk<RiskyPostCodeIndicatorProvider>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val districtAreaUrlProvider = mockk<DistrictAreaStringProvider>(relaxed = true)
    private val startAppReviewFlowConstraint = mockk<ShouldShowInAppReview>(relaxed = true)
    private val lastReviewFlowStartedDateProvider =
        mockk<LastAppRatingStartedDateProvider>(relaxed = true)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val showInformationScreenObserver = mockk<Observer<InformationScreen>>(relaxed = true)

    private val testSubject =
        StatusViewModel(
            postCodeProvider,
            postCodeIndicatorProvider,
            sharedPreferences,
            isolationStateMachine,
            userInbox,
            notificationProvider,
            districtAreaUrlProvider,
            startAppReviewFlowConstraint,
            lastReviewFlowStartedDateProvider
        )

    private val lowRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.GREEN,
        name = Translatable(mapOf("en" to "low")),
        heading = Translatable(mapOf("en" to "Heading low")),
        content = Translatable(
            mapOf(
                "en" to "Content low"
            )
        ),
        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val mediumRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.YELLOW,
        name = Translatable(mapOf("en" to "medium")),
        heading = Translatable(mapOf("en" to "Heading medium")),
        content = Translatable(
            mapOf(
                "en" to "Content medium"
            )
        ),
        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val highRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.RED,
        name = Translatable(mapOf("en" to "high")),
        heading = Translatable(mapOf("en" to "Heading high")),
        content = Translatable(
            mapOf(
                "en" to "Content high"
            )
        ),
        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val lowRisk = Risk(
        mainPostCode = "A1",
        riskIndicator = lowRiskyPostCodeIndicator,
        riskLevelFromLocalAuthority = false
    )

    private val mediumRisk = Risk(
        mainPostCode = "A1",
        riskIndicator = mediumRiskyPostCodeIndicator,
        riskLevelFromLocalAuthority = false
    )

    private val highRisk = Risk(
        mainPostCode = "A1",
        riskIndicator = highRiskyPostCodeIndicator,
        riskLevelFromLocalAuthority = false
    )

    private val defaultViewState = ViewState(
        currentDate = LocalDate.now(),
        areaRiskState = mediumRisk,
        isolationState = DEFAULT_ISOLATION_STATE,
        latestAdviceUrl = DEFAULT_LATEST_ADVICE_URL_RES_ID,
        showExposureNotificationReminderDialog = false
    )

    @Before
    fun setUp() {
        every { postCodeProvider.value } returns DEFAULT_POST_CODE
        every { postCodeIndicatorProvider.riskyPostCodeIndicator } returns RiskIndicatorWrapper(
            "medium",
            mediumRiskyPostCodeIndicator
        )
        every { userInbox.fetchInbox() } returns DEFAULT_INFORMATION_SCREEN_STATE
        every { isolationStateMachine.readState() } returns DEFAULT_ISOLATION_STATE
        coEvery { districtAreaUrlProvider.provide(any()) } returns DEFAULT_LATEST_ADVICE_URL_RES_ID

        testSubject.viewState.observeForever(viewStateObserver)
        testSubject.showInformationScreen().observeForever(showInformationScreenObserver)
    }

    @After
    fun tearDown() {
        testSubject.viewState.removeObserver(viewStateObserver)
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `area risk changed from high to low`() {
        every { postCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "low",
                lowRiskyPostCodeIndicator
            )
        )

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = lowRisk)) }
    }

    @Test
    fun `area risk changed from high to medium`() {
        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = mediumRisk)) }
    }

    @Test
    fun `area risk changed from low to high`() {
        every { postCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "high",
                highRiskyPostCodeIndicator
            )
        )

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = highRisk)) }
    }

    @Test
    fun `area risk changed from low to medium`() {
        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = mediumRisk)) }
    }

    @Test
    fun `area risk did not change and is low`() {
        every { postCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "low",
                lowRiskyPostCodeIndicator
            )
        )

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = lowRisk)) }
    }

    @Test
    fun `area risk did not change and is medium`() {
        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = mediumRisk)) }
    }

    @Test
    fun `get latest url when not in default state`() {
        val isolationState = Isolation(Instant.now(), DurationDays())

        every { isolationStateMachine.readState() } returns isolationState
        coEvery { districtAreaUrlProvider.provide(R.string.url_latest_advice_in_isolation) } returns 0

        testSubject.updateViewState()

        coVerify { districtAreaUrlProvider.provide(R.string.url_latest_advice_in_isolation) }

        verify {
            viewStateObserver.onChanged(
                defaultViewState.copy(
                    latestAdviceUrl = 0,
                    isolationState = isolationState
                )
            )
        }
    }

    @Test
    fun `area risk did not change and is high`() {
        every { postCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "high",
                highRiskyPostCodeIndicator
            )
        )

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = highRisk)) }
    }

    @Test
    fun `onResume updated view state and registers user inbox listener`() {
        testSubject.onResume()

        verify { viewStateObserver.onChanged(defaultViewState) }
        verify { sharedPreferences.registerOnSharedPreferenceChangeListener(any()) }
        verify { userInbox.registerListener(any()) }
    }

    @Test
    fun `onPause unregisters user inbox listener`() {
        testSubject.onPause()

        verify { sharedPreferences.unregisterOnSharedPreferenceChangeListener(any()) }
        verify { userInbox.unregisterListener(any()) }
    }

    @Test
    fun `on stop exposure notification clicked show exposure notification reminder dialog if notification is allowed`() {
        every { notificationProvider.canSendNotificationToChannel(any()) } returns true

        testSubject.updateViewState()
        testSubject.onStopExposureNotificationsClicked()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showExposureNotificationReminderDialog = true)) }
    }

    @Test
    fun `on stop exposure notification clicked do show exposure notification reminder dialog if notification is not allowed`() {
        every { notificationProvider.canSendNotificationToChannel(any()) } returns false

        testSubject.updateViewState()
        testSubject.onStopExposureNotificationsClicked()

        verify { viewStateObserver.onChanged(defaultViewState) }
    }

    @Test
    fun `risky post code indicator is null`() {
        every { postCodeIndicatorProvider.riskyPostCodeIndicator } returns null

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = Unknown)) }
    }

    @Test
    fun `risky post code indicator has no risk set`() {
        every { postCodeIndicatorProvider.riskyPostCodeIndicator } returns
            RiskIndicatorWrapper("medium", null)

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(areaRiskState = Unknown)) }
    }

    @Test
    fun `update view state on date change`() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        testSubject.updateViewState(today)
        testSubject.updateViewState(today)
        testSubject.updateViewState(tomorrow)

        verify(exactly = 1) { viewStateObserver.onChanged(defaultViewState.copy(currentDate = today)) }
        verify(exactly = 1) { viewStateObserver.onChanged(defaultViewState.copy(currentDate = tomorrow)) }
    }

    @Test
    fun `update view state with isolation expiration`() {
        val now = LocalDate.now()
        val inboxItem = ShowIsolationExpiration(now)

        every { userInbox.fetchInbox() } returns inboxItem

        testSubject.userInboxListener.invoke()

        verify { userInbox.clearItem(inboxItem) }
        verify { showInformationScreenObserver.onChanged(IsolationExpiration(now)) }
    }

    @Test
    fun `update view state with show test result`() {
        every { userInbox.fetchInbox() } returns ShowTestResult

        testSubject.userInboxListener.invoke()

        verify { notificationProvider.cancelTestResult() }
        verify { showInformationScreenObserver.onChanged(TestResult) }
    }

    @Test
    fun `update view state with show venue alert`() {
        val inboxItem = ShowVenueAlert("venue1")

        every { userInbox.fetchInbox() } returns inboxItem

        testSubject.userInboxListener.invoke()

        verify { userInbox.clearItem(inboxItem) }
        verify { showInformationScreenObserver.onChanged(VenueAlert("venue1")) }
    }

    @Test
    fun `update view state with show encounter detection`() {
        every { userInbox.fetchInbox() } returns ShowEncounterDetection

        testSubject.userInboxListener.invoke()

        verify { showInformationScreenObserver.onChanged(ExposureConsent) }
    }

    companion object {
        private const val DEFAULT_POST_CODE = "A1"
        private val DEFAULT_INFORMATION_SCREEN_STATE = null
        private val DEFAULT_ISOLATION_STATE = Default()
        private const val DEFAULT_LATEST_ADVICE_URL_RES_ID = 0
    }
}
