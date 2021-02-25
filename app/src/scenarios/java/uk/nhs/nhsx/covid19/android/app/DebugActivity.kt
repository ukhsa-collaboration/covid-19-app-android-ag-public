package uk.nhs.nhsx.covid19.android.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Button
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import java.time.Instant
import java.time.LocalDate
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import kotlinx.android.synthetic.scenarios.activity_debug.buttonFeatureFlags
import kotlinx.android.synthetic.scenarios.activity_debug.environmentSpinner
import kotlinx.android.synthetic.scenarios.activity_debug.exposureNotificationMocks
import kotlinx.android.synthetic.scenarios.activity_debug.languageSpinner
import kotlinx.android.synthetic.scenarios.activity_debug.mockSettings
import kotlinx.android.synthetic.scenarios.activity_debug.scenarioOnboarding
import kotlinx.android.synthetic.scenarios.activity_debug.scenarioMain
import kotlinx.android.synthetic.scenarios.activity_debug.scenarios
import kotlinx.android.synthetic.scenarios.activity_debug.scenariosGroup
import kotlinx.android.synthetic.scenarios.activity_debug.screenButtonContainer
import kotlinx.android.synthetic.scenarios.activity_debug.screenFilter
import kotlinx.android.synthetic.scenarios.activity_debug.statusScreen
import kotlinx.android.synthetic.scenarios.activity_debug.titleScreens
import kotlinx.android.synthetic.scenarios.activity_debug.titleScenarios
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.DEFAULT
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.about.UserDataActivity
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityActivity
import uk.nhs.nhsx.covid19.android.app.availability.UpdateRecommendedActivity
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationActivity
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityInformationActivity
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.edgecases.TabletNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.featureflag.testsettings.TestSettingsActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.DataAndPrivacyActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.WelcomeActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentActivity
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeHelpActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.CameraPermissionNotGranted
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.InvalidContent
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.ScanningNotSupported
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Success
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.MEETING_PEOPLE
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.scenariodialog.MockApiDialogFragment
import uk.nhs.nhsx.covid19.android.app.scenariodialog.TestResultDialogFragment
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultOnsetDateActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.temporal.ChronoUnit

class DebugActivity : AppCompatActivity(R.layout.activity_debug) {

    private lateinit var debugSharedPreferences: SharedPreferences

    private lateinit var appLocaleProvider: ApplicationLocaleProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        appLocaleProvider = applicationContext.appComponent.provideApplicationLocaleProvider()

        setSupportActionBar(toolbar)

        debugSharedPreferences = getSharedPreferences(DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

        setupEnvironmentSpinner()

        setupFeatureFlagButton()

        setupExposureNotificationCheckbox()

        setupLanguageSpinner()

        setupScenariosButtons()

        setupScreenButtons()

        setupScreenFilter()
    }

    private fun setupEnvironmentSpinner() {
        val environments = scenariosApp.environments
        val environmentsAdapter = EnvironmentAdapter(this, environments)

        environmentSpinner.adapter = environmentsAdapter
        val selectedEnvironment = debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0)
            .coerceIn(0, environments.size - 1)
        environmentSpinner.setSelection(selectedEnvironment)

        environmentSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                debugSharedPreferences.edit().putInt(SELECTED_ENVIRONMENT, position).apply()
                scenariosApp.updateDependencyGraph()
                mockSettings.apply {
                    visibility =
                        if (position == scenariosApp.mockEnvironmentIndex) View.VISIBLE
                        else View.GONE
                    refreshMockSettingsLabel()
                }
            }
        }
        setupMockBehaviour()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshMockSettingsLabel() = with(MockApiModule.behaviour) {
        mockSettings.text = "$responseType after ${delayMillis}ms"
    }

    private fun setupMockBehaviour() {
        mockSettings.setOnSingleClickListener {
            MockApiDialogFragment {
                refreshMockSettingsLabel()
            }.show(supportFragmentManager, "MockApiDialogFragment")
        }
    }

    private fun setupExposureNotificationCheckbox() {
        val useMockedExposureNotifications =
            debugSharedPreferences.getBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, false)
        exposureNotificationMocks.isChecked = useMockedExposureNotifications

        exposureNotificationMocks.setOnCheckedChangeListener { _, isChecked ->
            debugSharedPreferences.edit().putBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, isChecked)
                .apply()
            scenariosApp.updateDependencyGraph()
        }
    }

    private fun setupLanguageSpinner() {
        val supportedLanguages = SupportedLanguage.values().toList()
        val languageAdapter = LanguageAdapter(this, supportedLanguages)

        languageSpinner.adapter = languageAdapter

        val previouslySelectedLanguage = appLocaleProvider.getUserSelectedLanguage() ?: DEFAULT
        val indexOfPreviouslySelectedLanguage =
            supportedLanguages.indexOf(previouslySelectedLanguage)
        languageSpinner.setSelection(indexOfPreviouslySelectedLanguage)

        languageSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLanguage = supportedLanguages[position]
                appLocaleProvider.languageCode = selectedLanguage.code
            }
        }
    }

    private fun setupFeatureFlagButton() {
        buttonFeatureFlags.setOnSingleClickListener {
            startActivity(Intent(this, TestSettingsActivity::class.java))
        }
    }

    private fun setupScenariosButtons() {
        titleScenarios.setOnSingleClickListener {
            if (scenariosGroup.visibility == View.VISIBLE) {
                scenariosGroup.visibility = View.GONE
                titleScenarios.text = "Scenarios ..."
            } else {
                scenariosGroup.visibility = View.VISIBLE
                titleScenarios.text = "Scenarios"
            }
        }

        scenarioMain.setOnSingleClickListener {
            MainActivity.start(this)
        }

        scenarioOnboarding.setOnSingleClickListener {
            WelcomeActivity.start(this)
        }

        statusScreen.setOnSingleClickListener {
            startActivity<StatusActivity>()
        }
    }

    private fun setupScreenFilter() {
        screenFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setupScreenButtons()
                scenarios.scrollToChild(titleScreens)
            }
        })

        screenFilter.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) scenarios.scrollToChild(titleScreens)
        }

        screenFilter.setOnEditorActionListener { _, actionId, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (actionId == KeyEvent.KEYCODE_ENTER)) {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(screenFilter.windowToken, 0)
                true
            } else false
        }
    }

    private fun setupScreenButtons() {
        screenButtonContainer.removeAllViews()

        addScreenButton("Isolation Payment") {
            startActivity<IsolationPaymentActivity>()
        }
        addScreenButton("Post code") {
            PostCodeActivity.start(this)
        }

        addScreenButton("Data and privacy") {
            DataAndPrivacyActivity.start(this)
        }

        addScreenButton("Permission") {
            PermissionActivity.start(this)
        }

        addScreenButton("Enable bluetooth") {
            EnableBluetoothActivity.start(this)
        }

        addScreenButton("Enable location services") {
            EnableLocationActivity.start(this)
        }

        addScreenButton("Enable exposure notifications") {
            EnableExposureNotificationsActivity.start(this)
        }

        addScreenButton("QR Code Scanner") {
            startActivity<QrScannerActivity>()
        }

        addScreenButton("QR Code Scan Success") {
            QrCodeScanResultActivity.start(this, Success("Sample Venue"))
        }

        addScreenButton("QR Code Scan Failure") {
            QrCodeScanResultActivity.start(this, InvalidContent)
        }

        addScreenButton("QR Code Permission Not Granted") {
            QrCodeScanResultActivity.start(this, CameraPermissionNotGranted)
        }

        addScreenButton("QR Code Scanning Not Supported") {
            QrCodeScanResultActivity.start(this, ScanningNotSupported)
        }

        addScreenButton("QR Code Help") {
            startActivity<QrCodeHelpActivity>()
        }

        addScreenButton("Risky Venue Alert") {
            VenueAlertActivity.start(this, "ABCD1234")
        }

        addScreenButton("Questionnaire screen") {
            startActivity<QuestionnaireActivity>()
        }

        addScreenButton("Questionnaire No Symptoms") {
            startActivity<NoSymptomsActivity>()
        }

        addScreenButton("Questionnaire Review Symptoms") {
            startActivity(reviewSymptomsIntent)
        }

        addScreenButton("Questionnaire Isolation Advice") {
            SymptomsAdviceIsolateActivity.start(this, true, 14)
        }

        addScreenButton("Testing information") {
            startActivity<TestOrderingActivity>()
        }

        addScreenButton("Test result") {
            TestResultDialogFragment {
                startActivity<TestResultActivity>()
            }.show(supportFragmentManager, "TestResultDialogFragment")
        }

        addScreenButton("Encounter detection") {
            EncounterDetectionActivity.start(this)
        }

        addScreenButton("Isolation Expiration") {
            IsolationExpirationActivity.start(this, LocalDate.now().minusDays(1).toString())
        }

        addScreenButton("More about the app") {
            MoreAboutAppActivity.start(this)
        }

        addScreenButton("Submit Keys Progress") {
            startActivity(getSubmitKeysIntent())
        }

        addScreenButton("User Data") {
            UserDataActivity.start(this)
        }

        addScreenButton("Test Ordering Progress") {
            startActivity<TestOrderingProgressActivity>()
        }

        addScreenButton("App Availability") {
            startActivity<AppAvailabilityActivity>()
        }

        addScreenButton("Device not supported") {
            startActivity<DeviceNotSupportedActivity>()
        }

        addScreenButton("Tablet not supported") {
            startActivity<TabletNotSupportedActivity>()
        }

        addScreenButton("Share keys information") {
            startActivity(shareKeysIntent)
        }

        val riskIndicatorWithEmptyPolicyData = RiskIndicator(
            colorScheme = GREEN,
            colorSchemeV2 = GREEN,
            name = Translatable(mapOf("en" to "Tier1 from post code")),
            heading = Translatable(mapOf("en" to "Data from the NHS shows that the spread of coronavirus in your area is low.")),
            content = Translatable(
                mapOf(
                    "en" to "Your local authority has normal measures for coronavirus in place. It’s important that you continue to follow the latest official government guidance to help control the virus.\n" +
                        "\n" +
                        "Find out the restrictions for your local area to help reduce the spread of coronavirus."
                )
            ),
            linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
            linkUrl = Translatable(mapOf("en" to "https://faq.covid19.nhs.uk/article/KA-01270/en-us")),
            policyData = null
        )

        addScreenButton("Risk level from post code") {
            RiskLevelActivity.start(
                this,
                RiskyPostCodeViewState.Risk(
                    "CM2",
                    riskIndicatorWithEmptyPolicyData,
                    riskLevelFromLocalAuthority = false
                )
            )
        }

        addScreenButton("Update Recommended") {
            startActivity<UpdateRecommendedActivity>()
        }

        addScreenButton("Policy Update") {
            startActivity<PolicyUpdateActivity>()
        }

        addScreenButton("Local Authority") {
            startActivity<LocalAuthorityActivity> {
                putExtra(
                    LocalAuthorityActivity.EXTRA_POST_CODE,
                    "TD12"
                )
                putExtra(
                    LocalAuthorityActivity.EXTRA_BACK_ALLOWED,
                    false
                )
            }
        }

        addScreenButton("Local Authority Information") {
            startActivity<LocalAuthorityInformationActivity>()
        }

        addScreenButton("Risk level from local authority") {
            RiskLevelActivity.start(
                this,
                RiskyPostCodeViewState.Risk(
                    "CM2",
                    riskIndicatorWithEmptyPolicyData.copy(
                        policyData = PolicyData(
                            heading = Translatable(mapOf("en" to "Coronavirus cases are very high in your area")),
                            content = Translatable(mapOf("en" to "Local Authority content high")),
                            footer = Translatable(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                            policies = listOf(
                                Policy(
                                    policyIcon = MEETING_PEOPLE,
                                    policyHeading = Translatable(mapOf("en" to "Meeting people")),
                                    policyContent = Translatable(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
                                )
                            ),
                            localAuthorityRiskTitle = Translatable(mapOf("en" to "Local Authority is in local COVID alert level: high"))
                        )
                    ),
                    riskLevelFromLocalAuthority = true
                )
            )
        }

        addScreenButton("Edit post code") {
            EditPostalDistrictActivity.start(this)
        }

        addScreenButton("Link test result") {
            startActivity<LinkTestResultActivity>()
        }

        addScreenButton("Link test result symptoms") {
            startActivity(testResultSymptomsIntent)
        }

        addScreenButton("Link test result onset date") {
            startActivity(testResultOnsetDateIntent)
        }

        addScreenButton("Show all notifications") {
            val notifications = app.appComponent.provideNotificationProvider()
            notifications.showAppIsAvailable()
            notifications.showAppIsNotAvailable()
            notifications.showAreaRiskChangedNotification()
            notifications.showExposureNotification()
            notifications.showExposureNotificationReminder()
            notifications.showRiskyVenueVisitNotification()
            notifications.showStateExpirationNotification()
            notifications.showTestResultsReceivedNotification()
            notifications.showRecommendedAppUpdateIsAvailable()
        }

        addScreenButton("Trigger background tasks") {
            val periodicTasks = app.appComponent.providePeriodicTasks()
            periodicTasks.schedule()
        }

        addScreenButton("Open market") {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=uk.nhs.covid19.production")
            )
            val chooser = Intent.createChooser(marketIntent, "Select market app")
            if (marketIntent.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Timber.d("Can't start market app")
            }
        }

        addScreenButton("Battery optimization") {
            startActivity<BatteryOptimizationActivity>()
        }

        addScreenButton("Redirect To Isolation Payment Web") {
            startActivity<RedirectToIsolationPaymentWebsiteActivity>()
        }

        addScreenButton("Settings") {
            startActivity<SettingsActivity>()
        }

        addScreenButton("Languages") {
            startActivity<LanguagesActivity>()
        }
    }

    private fun addScreenButton(
        title: String,
        action: () -> Unit
    ) {
        if (!title.toLowerCase().contains(screenFilter.text.toString().toLowerCase())) return
        val button = Button(ContextThemeWrapper(this, R.style.PrimaryButton))
        button.text = title
        button.setOnSingleClickListener { action() }
        screenButtonContainer.addView(button)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debug, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionClearAppData -> {
                (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val shareKeysIntent: Intent by lazy {
        Intent(this, ShareKeysInformationActivity::class.java).apply {
            putExtra(
                "EXTRA_TEST_RESULT",
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(),
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
        }
    }

    private val testResultSymptomsIntent: Intent by lazy {
        Intent(this, LinkTestResultSymptomsActivity::class.java).apply {
            putExtra(
                "EXTRA_TEST_RESULT",
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now().minus(1, ChronoUnit.DAYS),
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
        }
    }

    private val testResultOnsetDateIntent: Intent by lazy {
        Intent(this, LinkTestResultOnsetDateActivity::class.java).apply {
            putExtra(
                "EXTRA_TEST_RESULT",
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now().minus(1, ChronoUnit.DAYS),
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
        }
    }

    private fun getSubmitKeysIntent() =
        Intent(this, SubmitKeysProgressActivity::class.java).apply {
            putParcelableArrayListExtra(
                "EXPOSURE_KEYS_TO_SUBMIT",
                ArrayList<NHSTemporaryExposureKey>()
            )
            putExtra("SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN", "test")
        }

    private val reviewSymptomsIntent: Intent by lazy {
        val strings = Translatable(mapOf("en" to "Test"))
        Intent(this, ReviewSymptomsActivity::class.java).apply {
            putParcelableArrayListExtra(
                ReviewSymptomsActivity.EXTRA_QUESTIONS,
                ArrayList<Question>().apply {
                    add(
                        Question(
                            symptom = Symptom(strings, strings, 0.0),
                            isChecked = true
                        )
                    )
                }
            )
        }
    }

    private fun ScrollView.scrollToChild(view: View) {
        val childOffset = Point()
        getDeepChildOffset(view.parent, view, childOffset)
        smoothScrollTo(0, childOffset.y)
    }

    private fun ViewGroup.getDeepChildOffset(
        parent: ViewParent,
        child: View,
        accumulatedOffset: Point
    ) {
        val parentGroup = parent as ViewGroup
        accumulatedOffset.x += child.left
        accumulatedOffset.y += child.top
        if (parentGroup == this) {
            return
        }
        getDeepChildOffset(parentGroup.parent, parentGroup, accumulatedOffset)
    }

    companion object {
        const val DEBUG_PREFERENCES_NAME = "debugPreferences"
        const val SELECTED_ENVIRONMENT = "SELECTED_ENVIRONMENT"
        const val USE_MOCKED_EXPOSURE_NOTIFICATION = "USE_MOCKED_EXPOSURE_NOTIFICATION"

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DebugActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
