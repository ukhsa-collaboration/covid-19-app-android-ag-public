package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaBanner
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.NAVIGATE_AND_TURN_ON
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.ONLY_NAVIGATE
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.StorageBasedUserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowUnknownTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.payment.CanClaimIsolationPayment
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.LocalMessage
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.TestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.UnknownTestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.NotIsolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.status.localmessage.GetLocalMessageFromStorage
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.viewutils.AreSystemLevelAnimationsEnabled
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StatusViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val postCodeIndicatorProvider = mockk<RiskyPostCodeIndicatorProvider>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val storageBasedUserInbox = mockk<StorageBasedUserInbox>(relaxUnitFun = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val districtAreaUrlProvider = mockk<DistrictAreaStringProvider>(relaxed = true)
    private val startAppReviewFlowConstraint = mockk<ShouldShowInAppReview>(relaxed = true)
    private val lastReviewFlowStartedDateProvider =
        mockk<LastAppRatingStartedDateProvider>(relaxed = true)
    private val canClaimIsolationPayment = mockk<CanClaimIsolationPayment>(relaxed = true)
    private val isolationPaymentTokenStateProvider = mockk<IsolationPaymentTokenStateProvider>(relaxed = true)
    private val animationsProvider = mockk<AnimationsProvider>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider =
        mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val getLocalMessageFromStorage = mockk<GetLocalMessageFromStorage>()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val navigateTo = mockk<Observer<NavigationTarget>>(relaxed = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val exposureNotificationManager = mockk<ExposureNotificationManager>()
    private val exposureNotificationPermissionHelperFactory = mockk<ExposureNotificationPermissionHelper.Factory>()
    private val exposureNotificationPermissionHelper = mockk<ExposureNotificationPermissionHelper>(relaxUnitFun = true)
    private val areSystemLevelAnimationsEnabled = mockk<AreSystemLevelAnimationsEnabled>(relaxUnitFun = true)

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-22T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private lateinit var testSubject: StatusViewModel

    private val lowRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.GREEN,
        colorSchemeV2 = ColorScheme.GREEN,
        name = TranslatableString(mapOf("en" to "low")),
        heading = TranslatableString(mapOf("en" to "Heading low")),
        content = TranslatableString(
            mapOf(
                "en" to "Content low"
            )
        ),
        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val mediumRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.YELLOW,
        colorSchemeV2 = ColorScheme.YELLOW,
        name = TranslatableString(mapOf("en" to "medium")),
        heading = TranslatableString(mapOf("en" to "Heading medium")),
        content = TranslatableString(
            mapOf(
                "en" to "Content medium"
            )
        ),
        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val highRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.RED,
        colorSchemeV2 = ColorScheme.RED,
        name = TranslatableString(mapOf("en" to "high")),
        heading = TranslatableString(mapOf("en" to "Heading high")),
        content = TranslatableString(
            mapOf(
                "en" to "Content high"
            )
        ),
        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
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
        currentDate = LocalDate.now(fixedClock),
        areaRiskState = mediumRisk,
        isolationState = DEFAULT_ISOLATION_VIEW_STATE,
        showIsolationPaymentButton = false,
        showReportSymptomsButton = true,
        exposureNotificationsEnabled = false,
        animationsEnabled = false,
        localMessage = null
    )

    @Before
    fun setUp() {
        every { postCodeProvider.value } returns DEFAULT_POST_CODE
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { postCodeIndicatorProvider.riskyPostCodeIndicator } returns RiskIndicatorWrapper(
            "medium",
            mediumRiskyPostCodeIndicator
        )
        every {
            exposureNotificationPermissionHelperFactory.create(any(), any())
        } returns exposureNotificationPermissionHelper
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false
        every { userInbox.fetchInbox() } returns DEFAULT_INFORMATION_SCREEN_STATE
        every { isolationStateMachine.readLogicalState() } returns DEFAULT_ISOLATION_STATE
        coEvery { districtAreaUrlProvider.provide(any()) } returns DEFAULT_LATEST_ADVICE_URL_RES_ID
        coEvery { exposureNotificationManager.isEnabled() } returns false
        coEvery { getLocalMessageFromStorage() } returns null

        setupTestSubject(contactTracingHubAction = null)
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
    fun `get isolation advice when not in default state`() {
        val contactCase = isolationHelper.contactCase()
        val isolationState = contactCase.asIsolation().asLogical()

        every { isolationStateMachine.readLogicalState() } returns isolationState
        coEvery { districtAreaUrlProvider.provide(R.string.url_latest_advice_in_isolation) } returns 0

        testSubject.updateViewState()

        coVerify { districtAreaUrlProvider.provide(R.string.url_latest_advice_in_isolation) }

        verify {
            viewStateObserver.onChanged(
                defaultViewState.copy(
                    showReportSymptomsButton = true,
                    isolationState = Isolating(
                        isolationStart = contactCase.startDate,
                        expiryDate = contactCase.expiryDate,
                        isolationAdvice = 0
                    )
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
        verify { isolationPaymentTokenStateProvider.addTokenStateListener(any()) }
        verify { storageBasedUserInbox.setStorageChangeListener(any()) }
    }

    @Test
    fun `onPause unregisters user inbox listener`() {
        testSubject.onPause()

        verify { sharedPreferences.unregisterOnSharedPreferenceChangeListener(any()) }
        verify { isolationPaymentTokenStateProvider.removeTokenStateListener(any()) }
        verify { storageBasedUserInbox.removeStorageChangeListener() }
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
    fun `animation provider value is true and system level animations enabled should update state to true`() {
        every { animationsProvider.inAppAnimationEnabled } returns true
        every { areSystemLevelAnimationsEnabled.invoke() } returns true

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(animationsEnabled = true)) }
    }

    @Test
    fun `animation provider value is false and system level animations enabled should update state to false`() {
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { areSystemLevelAnimationsEnabled.invoke() } returns true

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(animationsEnabled = false)) }
    }

    @Test
    fun `animation provider value is true and system level animations disabled should update state to false`() {
        every { animationsProvider.inAppAnimationEnabled } returns true
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(animationsEnabled = false)) }
    }

    @Test
    fun `animation provider value is false and system level animations disabled should update state to false`() {
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(animationsEnabled = false)) }
    }

    @Test
    fun `on update view state should not show isolation payment button if cannot claim isolation payment and token is unresolved`() {
        every { canClaimIsolationPayment() } returns false
        every { isolationPaymentTokenStateProvider.tokenState } returns Unresolved

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = false)) }
    }

    @Test
    fun `on update view state should not show isolation payment button if cannot claim isolation payment and token is disabled`() {
        every { canClaimIsolationPayment() } returns false
        every { isolationPaymentTokenStateProvider.tokenState } returns Disabled

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = false)) }
    }

    @Test
    fun `on update view state should not show isolation payment button if cannot claim isolation payment and there is a token`() {
        every { canClaimIsolationPayment() } returns false
        every { isolationPaymentTokenStateProvider.tokenState } returns Token("token")

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = false)) }
    }

    @Test
    fun `on update view state should not show isolation payment button if can claim isolation payment and token is unresolved`() {
        every { canClaimIsolationPayment() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Unresolved

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = false)) }
    }

    @Test
    fun `on update view state should not show isolation payment button if can claim isolation payment and token is disabled`() {
        every { canClaimIsolationPayment() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Disabled

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = false)) }
    }

    @Test
    fun `on update view state should show isolation payment button if can claim isolation payment and there is a token`() {
        every { canClaimIsolationPayment() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Token("token")

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = true)) }
    }

    @Test
    fun `visibility of isolation payment button should update when isolation payment token status changes`() {
        testSubject.onResume()

        val tokenStateListenerSlot = slot<(IsolationPaymentTokenState) -> Unit>()
        verify { isolationPaymentTokenStateProvider.addTokenStateListener(capture(tokenStateListenerSlot)) }

        every { canClaimIsolationPayment() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Unresolved

        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = false)) }

        val newState = Token("token")
        every { isolationPaymentTokenStateProvider.tokenState } returns newState
        tokenStateListenerSlot.captured(newState)

        verify { viewStateObserver.onChanged(defaultViewState.copy(showIsolationPaymentButton = true)) }
    }

    @Test
    fun `update view state on date change`() {
        val today = LocalDate.now(fixedClock)
        val tomorrow = today.plusDays(1)

        testSubject.updateViewState(today)
        testSubject.updateViewState(today)
        testSubject.updateViewState(tomorrow)

        verify(exactly = 1) { viewStateObserver.onChanged(defaultViewState.copy(currentDate = today)) }
        verify(exactly = 1) { viewStateObserver.onChanged(defaultViewState.copy(currentDate = tomorrow)) }
    }

    @Test
    fun `update view state on null local message`() {
        testSubject.updateViewState()

        verify { viewStateObserver.onChanged(defaultViewState.copy(localMessage = null)) }
    }

    @Test
    fun `update view state on new local message`() {
        val expected = mockk<LocalMessageTranslation>()
        coEvery { getLocalMessageFromStorage() } returns expected

        testSubject.updateViewState()

        verify(exactly = 1) { viewStateObserver.onChanged(defaultViewState.copy(localMessage = expected)) }
    }

    @Test
    fun `update view state with isolation expiration`() {
        val now = LocalDate.now(fixedClock)
        val inboxItem = ShowIsolationExpiration(now)

        every { userInbox.fetchInbox() } returns inboxItem

        testSubject.userInboxStorageChangeListener.notifyChanged()

        verify { navigateTo.onChanged(IsolationExpiration(now)) }
    }

    @Test
    fun `update view state with show test result`() {
        every { userInbox.fetchInbox() } returns ShowTestResult

        testSubject.userInboxStorageChangeListener.notifyChanged()

        verify { notificationProvider.cancelTestResult() }
        verify { navigateTo.onChanged(TestResult) }
    }

    @Test
    fun `update view state with show unknown test result`() {
        every { userInbox.fetchInbox() } returns ShowUnknownTestResult

        testSubject.userInboxStorageChangeListener.notifyChanged()

        verify { navigateTo.onChanged(UnknownTestResult) }
    }

    @Test
    fun `update view state with show venue alert`() {
        val inboxItem = ShowVenueAlert("venue1", INFORM)

        every { userInbox.fetchInbox() } returns inboxItem

        testSubject.userInboxStorageChangeListener.notifyChanged()

        verify { navigateTo.onChanged(VenueAlert("venue1", INFORM)) }
    }

    @Test
    fun `update view state with show encounter detection`() {
        every { userInbox.fetchInbox() } returns ShowEncounterDetection

        testSubject.userInboxStorageChangeListener.notifyChanged()

        verify { navigateTo.onChanged(ExposureConsent) }
    }

    @Test
    fun `when isolation payment button tapped selectedIsolationPaymentsButton analytics event added`() {
        testSubject.optionIsolationPaymentClicked()
        coVerify { analyticsEventProcessorMock.track(SelectedIsolationPaymentsButton) }
    }

    @Test
    fun `updateViewStateAndCheckUserInbox should update view state and fetch from user inbox`() {
        every { userInbox.fetchInbox() } returns ShowEncounterDetection

        testSubject.updateViewStateAndCheckUserInbox()

        verify { viewStateObserver.onChanged(defaultViewState) }
        verify { userInbox.fetchInbox() }
        verify { navigateTo.onChanged(ExposureConsent) }
    }

    @Test
    fun `on activate contact tracing button clicked enables contact tracing`() {
        testSubject.onActivateContactTracingButtonClicked()
        verify { exposureNotificationPermissionHelper.startExposureNotifications() }
    }

    @Test
    fun `when contactTracingHubAction is set to NAVIGATE_AND_TURN_ON navigates to ContactTracingHub`() {
        setupTestSubject(contactTracingHubAction = NAVIGATE_AND_TURN_ON)

        testSubject.updateViewStateAndCheckUserInbox()

        verify { navigateTo.onChanged(ContactTracingHub(shouldTurnOnContactTracing = true)) }
    }

    @Test
    fun `when contactTracingHubAction is set to ONLY_NAVIGATE navigates to ContactTracingHub`() {
        setupTestSubject(contactTracingHubAction = ONLY_NAVIGATE)

        testSubject.updateViewStateAndCheckUserInbox()

        verify { navigateTo.onChanged(ContactTracingHub(shouldTurnOnContactTracing = false)) }
    }

    @Test
    fun `contactTracingHubAction consumed after first call`() {
        setupTestSubject(contactTracingHubAction = ONLY_NAVIGATE)

        testSubject.updateViewStateAndCheckUserInbox()

        verify { navigateTo.onChanged(ContactTracingHub(shouldTurnOnContactTracing = false)) }

        testSubject.updateViewStateAndCheckUserInbox()

        confirmVerified(navigateTo)
    }

    @Test
    fun `when showLocalMessageScreen is set to true navigates to LocalMessages`() {
        setupTestSubject(contactTracingHubAction = null, showLocalMessageScreen = true)

        testSubject.updateViewStateAndCheckUserInbox()

        verify { navigateTo.onChanged(LocalMessage) }
        coVerify { analyticsEventProcessorMock.track(DidAccessLocalInfoScreenViaNotification) }
    }

    @Test
    fun `showLocalMessageScreen consumed after first call`() {
        setupTestSubject(contactTracingHubAction = null, showLocalMessageScreen = true)

        testSubject.updateViewStateAndCheckUserInbox()

        verify { navigateTo.onChanged(LocalMessage) }

        testSubject.updateViewStateAndCheckUserInbox()

        confirmVerified(navigateTo)
    }

    @Test
    fun `when local message banner is clicked track analytics event DidAccessLocalInfoScreenViaBanner`() {
        setupTestSubject(contactTracingHubAction = null)

        testSubject.localMessageBannerClicked()

        coVerify { analyticsEventProcessorMock.track(DidAccessLocalInfoScreenViaBanner) }
    }

    private fun setupTestSubject(
        contactTracingHubAction: ContactTracingHubAction?,
        showLocalMessageScreen: Boolean = false
    ) {
        testSubject = StatusViewModel(
            postCodeProvider,
            postCodeIndicatorProvider,
            sharedPreferences,
            isolationStateMachine,
            userInbox,
            storageBasedUserInbox,
            notificationProvider,
            districtAreaUrlProvider,
            startAppReviewFlowConstraint,
            lastReviewFlowStartedDateProvider,
            canClaimIsolationPayment,
            isolationPaymentTokenStateProvider,
            analyticsEventProcessorMock,
            animationsProvider,
            fixedClock,
            exposureNotificationManager,
            areSystemLevelAnimationsEnabled,
            getLocalMessageFromStorage,
            exposureNotificationPermissionHelperFactory,
            contactTracingHubAction = contactTracingHubAction,
            showLocalMessageScreen = showLocalMessageScreen
        )

        testSubject.viewState.observeForever(viewStateObserver)
        testSubject.navigationTarget().observeForever(navigateTo)
    }

    companion object {
        private const val DEFAULT_POST_CODE = "A1"
        private val DEFAULT_INFORMATION_SCREEN_STATE = null
        private val DEFAULT_ISOLATION_VIEW_STATE = NotIsolating
        private val DEFAULT_ISOLATION_STATE = IsolationState(isolationConfiguration = DurationDays()).asLogical()
        private const val DEFAULT_LATEST_ADVICE_URL_RES_ID = 0
    }
}
