package uk.nhs.nhsx.covid19.android.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import co.lokalise.android.sdk.LokaliseSDK
import co.lokalise.android.sdk.core.LokalisePreferences
import com.google.android.material.snackbar.Snackbar
import com.jeroenmols.featureflag.framework.RuntimeFeatureFlagProvider
import com.jeroenmols.featureflag.framework.TestSetting
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.covid19.config.LokaliseSettings
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryActivity
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataActivity
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityActivity
import uk.nhs.nhsx.covid19.android.app.availability.UpdateRecommendedActivity
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationActivity
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.common.bluetooth.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityInformationActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityDebugBinding
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockQrScannerViewModel.Options
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationOptOutActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.AgeLimitQuestionType.IsAdult
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ReviewData
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.BookFollowUpTestActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysReminderActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.featureflag.testsettings.TestSettingsActivity
import uk.nhs.nhsx.covid19.android.app.localdata.LocalDataAndStatisticsActivity
import uk.nhs.nhsx.covid19.android.app.localstats.FetchLocalDataProgressActivity
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
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.SymptomsAfterRiskyVenueActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.PositiveSymptomsNoIsolationActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CardinalSymptom
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersActivity.Companion.SYMPTOMS_DATA_KEY
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelSymptom
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.NonCardinalSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomsCheckerQuestions
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.YourSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.MEETING_PEOPLE
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlData
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlsWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.scenariodialog.MockApiDialogFragment
import uk.nhs.nhsx.covid19.android.app.scenariodialog.MyDataDialogFragment
import uk.nhs.nhsx.covid19.android.app.scenariodialog.QrScannerDialogFragment
import uk.nhs.nhsx.covid19.android.app.scenariodialog.ScenarioDialogFragment
import uk.nhs.nhsx.covid19.android.app.scenariodialog.TestResultDialogFragment
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsActivity
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesActivity
import uk.nhs.nhsx.covid19.android.app.settings.myarea.MyAreaActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubActivity
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubActivity
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubActivity
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessageActivity
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubActivity
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.OrderLfdTestActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultOnsetDateActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.UnknownTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.FAKE_LOCALE_NAME_FOR_STRING_IDS
import uk.nhs.nhsx.covid19.android.app.util.crashreporting.CrashReport
import uk.nhs.nhsx.covid19.android.app.util.isEmulator
import uk.nhs.nhsx.covid19.android.app.util.viewutils.purgeLokalise
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DebugActivity : AppCompatActivity() {

    private lateinit var debugSharedPreferences: SharedPreferences

    private lateinit var appLocaleProvider: ApplicationLocaleProvider

    private lateinit var binding: ActivityDebugBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appLocaleProvider = applicationContext.appComponent.provideApplicationLocaleProvider()

        setSupportActionBar(binding.primaryToolbar.toolbar)

        debugSharedPreferences = getSharedPreferences(DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

        setupEnvironmentSpinner()

        setupFeatureFlagButton()

        setupExposureNotificationCheckbox()

        setupLanguageSpinner()

        setupScenariosButtons()

        setupScreenButtons()

        setupScreenFilter()

        setupQrScannerSettings()

        setupUpdateTranslationsFromLokalise()

        setupAnalyticsData()
    }

    private fun setupAnalyticsData() {
        binding.analyticsData.setOnSingleClickListener {
            startActivity<AnalyticsReportActivity>()
        }
    }

    private fun setupQrScannerSettings() = with(binding) {
        MockQrScannerViewModel.currentOptions = Options(useMock = false)
        qrScannerFirstVenueAutomated.isChecked = false
        qrScannerFirstVenueAutomated.setOnCheckedChangeListener(qrScannerCheckChangeListener)
    }

    private val qrScannerCheckChangeListener =
        OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showQrScannerDialog()
            } else {
                MockQrScannerViewModel.currentOptions = MockQrScannerViewModel.currentOptions.copy(useMock = false)
            }
        }

    private fun showQrScannerDialog() = with(binding) {
        dialog = QrScannerDialogFragment(
            positiveAction = {
                if (MockQrScannerViewModel.currentOptions.venueList.size == 0) {
                    MockQrScannerViewModel.currentOptions =
                        MockQrScannerViewModel.currentOptions.copy(useMock = false)
                    qrScannerFirstVenueAutomated.isChecked = false
                } else {
                    MockQrScannerViewModel.currentOptions =
                        MockQrScannerViewModel.currentOptions.copy(useMock = true)
                }
            },
            dismissAction = {
                qrScannerFirstVenueAutomated.isChecked = false
            }
        )
        dialog?.show(supportFragmentManager, "QrScannerDialogFragment")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FILE_SELECT_CODE -> if (resultCode == RESULT_OK && data != null) {
                val uri: Uri? = data.data
                if (dialog != null && dialog is QrScannerDialogFragment && uri != null) {
                    (dialog as QrScannerDialogFragment).processCsv(uri)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupUpdateTranslationsFromLokalise() = with(binding) {
        lokaliseContent.apiIdError.isVisible = LokaliseSettings.apiKey == ""
        lokaliseContent.projectIdError.isVisible = LokaliseSettings.projectId == ""

        localiseHeadingContainer.setOnSingleClickListener {
            if (lokaliseContent.root.visibility == View.GONE) {
                lokaliseHeadingIcon.setImageDrawable(getDrawable(android.R.drawable.arrow_up_float))
                lokaliseContent.root.visibility = View.VISIBLE
            } else {
                lokaliseHeadingIcon.setImageDrawable(getDrawable(android.R.drawable.arrow_down_float))
                lokaliseContent.root.visibility = View.GONE
            }
        }
        updateLokaliseVersion()
        lokaliseContent.updateFromLokalise.setOnSingleClickListener {
            LokaliseSDK.clearCallbacks()
            LokaliseSDK.addCallback { previousVersion, newVersion ->
                Timber.d("LokaliseSDK On translations updated from $previousVersion to $newVersion")
                Snackbar.make(scenarios, "Updated to version $newVersion", Snackbar.LENGTH_LONG).show()
                updateLokaliseVersion()
            }
            lokaliseContent.updateFromLokalise.visibility = View.GONE
            lokaliseContent.lokaliseProgress.visibility = View.VISIBLE
            lokaliseContent.lokaliseProgress.animate()
            LokaliseSDK.updateTranslations()
        }

        lokaliseContent.purgeLokalise.setOnSingleClickListener {
            LokaliseSDK.clearCallbacks()
            purgeLokalise(this@DebugActivity)
            updateLokaliseVersion()
        }
    }

    private fun updateLokaliseVersion() = runOnUiThread {
        with(binding) {
            lokaliseContent.updateFromLokalise.visibility = View.VISIBLE
            lokaliseContent.lokaliseProgress.visibility = View.GONE
            lokaliseVersion.text = LokalisePreferences(this@DebugActivity).BUNDLE_ID.get().toString()
        }
    }

    private fun setupEnvironmentSpinner() = with(binding) {
        val environments = scenariosApp.environments
        val environmentsAdapter = EnvironmentAdapter(this@DebugActivity, environments)

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
        binding.mockSettings.text = "$responseType after ${delayMillis}ms"
    }

    private fun setupMockBehaviour() {
        binding.mockSettings.setOnSingleClickListener {
            MockApiDialogFragment {
                refreshMockSettingsLabel()
            }.show(supportFragmentManager, "MockApiDialogFragment")
        }
    }

    private fun setupExposureNotificationCheckbox() = with(binding) {
        val useMockedExposureNotifications =
            debugSharedPreferences.getBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, isEmulator())
        exposureNotificationMocks.isChecked = useMockedExposureNotifications

        exposureNotificationMocks.setOnCheckedChangeListener { _, isChecked ->
            debugSharedPreferences.edit().putBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, isChecked)
                .apply()
            scenariosApp.updateDependencyGraph()
        }
    }

    private fun setupLanguageSpinner() = with(binding) {
        val supportedLanguageItems = getSupportedLanguageItems()
        languageSpinner.adapter = LanguageAdapter(this@DebugActivity, supportedLanguageItems)

        val previouslySelectedLanguageItem = appLocaleProvider.getUserSelectedLanguage().toSupportedLanguageItem()
        val indexOfPreviouslySelectedLanguageItem = supportedLanguageItems.indexOf(previouslySelectedLanguageItem)
        languageSpinner.setSelection(indexOfPreviouslySelectedLanguageItem)

        languageSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = supportedLanguageItems[position]
                appLocaleProvider.languageCode = selectedLanguage.code
                if (selectedLanguage == stringIdsSupportedLanguageItem) {
                    val runtimeFeatureFlagProvider = RuntimeFeatureFlagProvider(this@DebugActivity)
                    runtimeFeatureFlagProvider.setFeatureEnabled(TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER, true)
                    runtimeFeatureFlagProvider.setFeatureEnabled(TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER, true)
                }
            }
        }
    }

    private fun getSupportedLanguageItems(): List<SupportedLanguageItem> {
        return listOf(
            SupportedLanguageItem(nameResId = R.string.default_language, code = null),
            stringIdsSupportedLanguageItem
        ) +
                SupportedLanguage.values().toList().map { SupportedLanguageItem(it.languageName, it.code) }
    }

    private fun setupFeatureFlagButton() {
        binding.buttonFeatureFlags.setOnSingleClickListener {
            startActivity(Intent(this, TestSettingsActivity::class.java))
        }
    }

    private fun setupScenariosButtons(hidden: Boolean = false) = with(binding) {
        titleScenarios.setOnSingleClickListener {
            if (scenariosGroup.visibility == View.VISIBLE) {
                scenariosGroup.visibility = View.GONE
            } else {
                scenariosGroup.visibility = View.VISIBLE
            }
            titleScenarios.text = SCENARIOS
        }

        if (hidden) {
            scenariosGroup.visibility = View.GONE
            titleScenarios.text = SCENARIOS
        }

        scenarioMain.setOnSingleClickListener {
            MainActivity.start(this@DebugActivity)
        }

        scenarioOnboarding.setOnSingleClickListener {
            WelcomeActivity.start(this@DebugActivity)
        }

        statusScreen.setOnSingleClickListener {
            startActivity<StatusActivity>()
        }

        shareFlow.setOnSingleClickListener {
            lifecycleScope.launch {
                appComponent.provideVisitedVenuesStorage().setVisits(venueVisits)
                startActivity(getTestResultIntent<ShareKeysInformationActivity>())
            }
        }
    }

    private fun setupScreenFilter(withText: String? = null) = with(binding) {
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

        if (withText != null) {
            screenFilter.setText(withText)
        }
    }

    private fun setupScreenButtons() {
        binding.screenButtonContainer.removeAllViews()

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
            startActivity<PermissionActivity>()
        }

        addScreenButton("Enable bluetooth") {
            EnableBluetoothActivity.start(this)
        }

        addScreenButton("Enable location services") {
            EnableLocationActivity.start(this)
        }

        addScreenButton("Enable exposure notifications") {
            startActivity<EnableExposureNotificationsActivity>()
        }

        addScreenButton("QR Code Scanner") {
            startActivity<QrScannerActivity>()
        }

        addScreenButton("QR Code Scan Success") {
            QrCodeScanResultActivity.start(this, Success("Sample Venue"))
        }

        addScreenButton("QR Success Custom Sound") {
            QrCodeScanResultActivity.start(this, Success("Sample Venue"))
            playCustomSoundAndVibrate()
        }

        addScreenButton("QR Success Default Sound") {
            QrCodeScanResultActivity.start(this, Success("Sample Venue"))
            playDefaultSoundAndVibrate()
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

        addScreenButton("Risky Venue Alert M1/INFORM") {
            VenueAlertInformActivity.start(this, "ABCD1234")
        }

        addScreenButton("Risky Venue Alert M2/BOOK TEST") {
            VenueAlertBookTestActivity.start(this, "ABCD1234")
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

        addScreenButton("Positive Symptoms No Isolation") {
            startActivity<PositiveSymptomsNoIsolationActivity>()
        }

        addScreenButton("Questionnaire Isolation Advice - NoIndexCaseThenIsolationDueToSelfAssessment") {
            SymptomsAdviceIsolateActivity.start(this, NoIndexCaseThenIsolationDueToSelfAssessment(7))
        }

        addScreenButton("Questionnaire Isolation Advice - NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales") {
            SymptomsAdviceIsolateActivity.start(this, NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales(7))
        }

        addScreenButton("Questionnaire Isolation Advice - NoIndexCaseThenSelfAssessmentNoImpactOnIsolation") {
            SymptomsAdviceIsolateActivity.start(this, NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(7))
        }

        addScreenButton("Questionnaire Isolation Advice - IndexCaseThenHasSymptomsDidUpdateIsolation") {
            SymptomsAdviceIsolateActivity.start(this, IndexCaseThenHasSymptomsDidUpdateIsolation(7))
        }

        addScreenButton("Questionnaire Isolation Advice - IndexCaseThenHasSymptomsNoEffectOnIsolation") {
            SymptomsAdviceIsolateActivity.start(this, IndexCaseThenHasSymptomsNoEffectOnIsolation)
        }

        addScreenButton("Questionnaire Isolation Advice - IndexCaseThenNoSymptoms") {
            SymptomsAdviceIsolateActivity.start(this, IndexCaseThenNoSymptoms)
        }

        addScreenButton("Testing information") {
            startActivity<TestOrderingActivity>()
        }

        addScreenButton("Test result") {
            TestResultDialogFragment {
                startActivity<TestResultActivity>()
            }.show(supportFragmentManager, "TestResultDialogFragment")
        }

        addScreenButton("Book follow-up test") {
            startActivity<BookFollowUpTestActivity>()
        }

        addScreenButton("Exposure Notification Screen") {
            createExposureNotification()
            ExposureNotificationActivity.start(this)
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
            MyDataDialogFragment {
                MyDataActivity.start(this)
            }.show(supportFragmentManager, "UserDataDialogFragment")
        }

        addScreenButton("Test Ordering Progress") {
            startActivity<TestOrderingProgressActivity>()
        }

        addScreenButton("Order LFD test") {
            startActivity<OrderLfdTestActivity>()
        }

        addScreenButton("App Availability") {
            startActivity<AppAvailabilityActivity>()
        }

        addScreenButton("Device not supported") {
            startActivity<DeviceNotSupportedActivity>()
        }

        addScreenButton("Share keys information") {
            appComponent.provideKeySharingInfoProvider().keySharingInfo = keySharingInfo
            startActivity<ShareKeysInformationActivity>()
        }

        addScreenButton("Share keys reminder") {
            appComponent.provideKeySharingInfoProvider().keySharingInfo = keySharingInfo
            startActivity<ShareKeysReminderActivity>()
        }

        addScreenButton("Animations") {
            startActivity<AnimationsActivity>()
        }

        val riskIndicatorWithEmptyPolicyData = RiskIndicator(
            colorScheme = GREEN,
            colorSchemeV2 = GREEN,
            name = TranslatableString(mapOf("en" to "Tier1 from post code")),
            heading = TranslatableString(mapOf("en" to "Data from the NHS shows that the spread of coronavirus in your area is low.")),
            content = TranslatableString(
                mapOf(
                    "en" to "Your local authority has normal measures for coronavirus in place. It’s important that you continue to follow the latest official government guidance to help control the virus.\n" +
                            "\n" +
                            "Find out the restrictions for your local area to help reduce the spread of coronavirus."
                )
            ),
            linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
            linkUrl = TranslatableString(mapOf("en" to "https://faq.covid19.nhs.uk/article/KA-01270/en-us")),
            policyData = null,
            externalUrls = null
        )

        val riskIndicatorExternalUrls = ExternalUrlsWrapper(
            title = TranslatableString(mapOf("en" to "Keep your app updated:")),
            urls = listOf(
                ExternalUrlData(
                    title = TranslatableString(mapOf("en" to "Check the App Store")),
                    url = TranslatableString(mapOf(
                        "en" to "https://apps.apple.com/gb/app/nhs-covid-19/id1520427663"))
                ),
                ExternalUrlData(
                    title = TranslatableString(mapOf("en" to "Check the Google Play Store")),
                    url = TranslatableString(mapOf(
                        "en" to "https://play.google.com/store/apps/details?id=uk.nhs.covid19.production&hl=en_US&gl=UK"))
                ),
                ExternalUrlData(
                    title = TranslatableString(mapOf("en" to "Check the app website")),
                    url = TranslatableString(mapOf(
                        "en" to "https://www.gov.uk/government/collections/nhs-covid-19-app"))
                ),
            )
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

        addScreenButton("Risk level from post code with links") {
            RiskLevelActivity.start(
                this,
                RiskyPostCodeViewState.Risk(
                    "CM2",
                    riskIndicatorWithEmptyPolicyData.copy(externalUrls = riskIndicatorExternalUrls),
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

        addScreenButton("Accordion Test") {
            startActivity<ComponentsActivity>()
        }

        addScreenButton("Local Authority Information") {
            startActivity<LocalAuthorityInformationActivity>()
        }

        val riskIndicatorPolicyData = PolicyData(
                heading = TranslatableString(mapOf("en" to "Coronavirus cases are very high in your area")),
                content = TranslatableString(mapOf("en" to "Local Authority content high")),
                footer = TranslatableString(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                policies = listOf(
                    Policy(
                        policyIcon = MEETING_PEOPLE,
                        policyHeading = TranslatableString(mapOf("en" to "Meeting people")),
                        policyContent = TranslatableString(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
                    )
                ),
                localAuthorityRiskTitle = TranslatableString(mapOf("en" to "Local Authority is in local COVID alert level: high"))
            )

        addScreenButton("Risk level from local authority") {
            RiskLevelActivity.start(
                this,
                RiskyPostCodeViewState.Risk(
                    "CM2",
                    riskIndicatorWithEmptyPolicyData.copy(
                        policyData = riskIndicatorPolicyData
                    ),
                    riskLevelFromLocalAuthority = true
                )
            )
        }

        addScreenButton("Risk level from local authority with links") {
            RiskLevelActivity.start(
                this,
                RiskyPostCodeViewState.Risk(
                    "CM2",
                    riskIndicatorWithEmptyPolicyData.copy(
                        policyData = riskIndicatorPolicyData,
                        externalUrls = riskIndicatorExternalUrls
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
            notifications.showRiskyVenueVisitNotification(BOOK_TEST)
            notifications.showStateExpirationNotification()
            notifications.showTestResultsReceivedNotification()
            notifications.showRecommendedAppUpdateIsAvailable()
            GlobalScope.launch {
                val message = appComponent.provideGetLocalMessageFromStorage().invoke()
                if (message != null) {
                    notifications.showLocalMessageNotification(title = message.head, message = message.body)
                } else {
                    Timber.d("Local information notification not shown because no message stored")
                }
            }
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

        addScreenButton("Venue History") {
            startActivity<VenueHistoryActivity>()
        }

        addScreenButton("Languages") {
            startActivity<LanguagesActivity>()
        }

        addScreenButton("Settings - My area") {
            startActivity<MyAreaActivity>()
        }

        addScreenButton("Share Result") {
            startActivity<ShareKeysResultActivity>()
        }

        addScreenButton("Contact Tracing Hub") {
            startActivity<ContactTracingHubActivity>()
        }

        addScreenButton("Unknown test result") {
            startActivity<UnknownTestResultActivity>()
        }

        addScreenButton("Add RemoteServiceException to storage") {
            appComponent.provideCrashReportProvider().crashReport =
                CrashReport("android.app.RemoteServiceException", Thread.currentThread().name, "Test Stack trace...")
        }

        addScreenButton("Testing Hub") {
            startActivity<TestingHubActivity>()
        }

        addScreenButton("Local message") {
            startActivity<LocalMessageActivity>()
        }

        addScreenButton("Risky Venue (M2): Do you have symptoms") {
            startActivity<SymptomsAfterRiskyVenueActivity>()
        }

        addScreenButton("Exposure Notification Age Limit") {
            createExposureNotification()
            ExposureNotificationAgeLimitActivity.start(this)
        }

        addScreenButton("Exposure Notification Vaccination Status") {
            createExposureNotification()
            ExposureNotificationVaccinationStatusActivity.start(this)
        }

        addScreenButton("Self-isolation hub") {
            startActivity<IsolationHubActivity>()
        }

        addScreenButton("Risky Contact Advice - Opt out") {
            startActivity<RiskyContactIsolationOptOutActivity>()
        }

        addScreenButton("Risky Contact Advice - Under 18 (resets state)") {
            val isolationStateMachine = appComponent.provideIsolationStateMachine()
            isolationStateMachine.reset()
            RiskyContactIsolationAdviceActivity.startAsMinor(this)
        }

        addScreenButton("Risky Contact Advice - Fully vaccinated (resets state)") {
            val isolationStateMachine = appComponent.provideIsolationStateMachine()
            isolationStateMachine.reset()
            RiskyContactIsolationAdviceActivity.startAsFullyVaccinated(this)
        }

        addScreenButton("Risky Contact Advice - Medically exempt (resets state)") {
            val isolationStateMachine = appComponent.provideIsolationStateMachine()
            isolationStateMachine.reset()
            RiskyContactIsolationAdviceActivity.startAsMedicallyExempt(this)
        }

        addScreenButton("Risky Contact Advice - New isolation (resets state)") {
            resetIsolationStateAndEnterContactIsolation()
            RiskyContactIsolationAdviceActivity.start(this)
        }

        addScreenButton("Risky Contact Advice - Continue isolation (resets state)") {
            appComponent.provideIsolationStateMachine().run {
                reset()
                processEvent(OnPositiveSelfAssessment(SelectedDate.CannotRememberDate))
                processEvent(OnExposedNotification(Instant.now()))
            }
            RiskyContactIsolationAdviceActivity.start(this)
        }

        addScreenButton("Risky contact review") {
            resetIsolationStateAndEnterContactIsolation()
            ExposureNotificationReviewActivity.start(
                this,
                reviewData = ReviewData(
                    questionnaireOutcome = MedicallyExempt,
                    ageResponse = OptOutResponseEntry(questionType = IsAdult, true),
                    vaccinationStatusResponses = listOf(
                        OptOutResponseEntry(questionType = VaccinationStatusQuestionType.FullyVaccinated, true),
                        OptOutResponseEntry(questionType = DoseDate, false),
                        OptOutResponseEntry(questionType = VaccinationStatusQuestionType.MedicallyExempt, true),
                    )
                )
            )
        }

        addScreenButton("Local Data and Statistics Screen") {
            startActivity<LocalDataAndStatisticsActivity>()
        }

        addScreenButton("Local Stats - Fetch Local Data") {
            startActivity<FetchLocalDataProgressActivity>()
        }

        addScreenButton("Local Stats - Show Local Data (random data)") {
            val randomLocalStats = RandomLocalStatsGenerator().generate()
            startActivity(LocalDataAndStatisticsActivity.getIntent(this, randomLocalStats))
        }

        addScreenButton("Guidance Hub") {
            startActivity<GuidanceHubActivity>()
        }

        addScreenButton("Check Your Answers") {
            startActivity<CheckYourAnswersActivity>() {
                putExtra(
                    SYMPTOMS_DATA_KEY,
                    SymptomsCheckerQuestions(
                        nonCardinalSymptoms = NonCardinalSymptoms(
                            title = TranslatableString(mapOf("en-GB" to "Do you have any of these symptoms?")),
                            isChecked = true,
                            nonCardinalSymptomsText = TranslatableString(mapOf("en-GB" to "Shivering or chills\n\nA new, continuous cough\n\nA loss or change to your sense of smell or taste\n\nShortness of breath\n\nFeeling tired or exhausted\n\nAn aching body\n\nA headache\n\nA sore throat\n\nA blocked or runny nose\n\nLoss of appetite\n\nDiarrhoea\n\nFeeling sick or being sick"))
                        ),
                        cardinalSymptom = CardinalSymptom(
                            title = TranslatableString(mapOf("en-GB" to "Do you have a high temperature?")),
                            isChecked = true
                        ),
                        howDoYouFeelSymptom = HowDoYouFeelSymptom(true)
                    )
                )
            }
        }

        addScreenButton("Your Symptoms Activity") {
            startActivity<YourSymptomsActivity>()
        }

        addScreenButton("Symptoms Checker Advice - Continue normal activities") {
            openSymptomsCheckerAdviceScreen(CONTINUE_NORMAL_ACTIVITIES)
        }

        addScreenButton("Symptoms Checker Advice - Try to stay at home") {
            openSymptomsCheckerAdviceScreen(TRY_TO_STAY_AT_HOME)
        }
    }

    private fun openSymptomsCheckerAdviceScreen(result: SymptomCheckerAdviceResult) {
        startActivity<SymptomCheckerAdviceActivity>() {
            putExtra(
                SymptomCheckerAdviceActivity.VALUE_KEY_QUESTIONS,
                SymptomsCheckerQuestions(
                    null,
                    null,
                    null
                )
            )
            putExtra(
                SymptomCheckerAdviceActivity.VALUE_KEY_RESULT,
                result
            )
        }
    }

    private fun resetIsolationStateAndEnterContactIsolation() {
        val isolationStateMachine = appComponent.provideIsolationStateMachine()
        isolationStateMachine.reset()
        isolationStateMachine.processEvent(OnExposedNotification(Instant.now()))
    }

    private fun createExposureNotification() =
        appComponent.provideIsolationStateMachine().apply {
            reset()
            processEvent(OnExposedNotification(Instant.now()))
        }

    private fun playDefaultSoundAndVibrate() {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val sound = RingtoneManager.getRingtone(applicationContext, notification)

        sound.play()
        triggerVibration()
    }

    private fun playCustomSoundAndVibrate() {
        val player = MediaPlayer.create(this, R.raw.success)

        player.start()
        triggerVibration()
    }

    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    private fun addScreenButton(
        title: String,
        action: () -> Unit
    ) = with(binding) {
        if (!title.lowercase().contains(screenFilter.text.toString().lowercase())) return
        val button = Button(ContextThemeWrapper(this@DebugActivity, R.style.PrimaryButton))
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

    private inline fun <reified T : Activity> getTestResultIntent() =
        Intent(this, T::class.java).apply {
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
        val strings = TranslatableString(mapOf("en" to "Test"))
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

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token",
        acknowledgedDate = Instant.now()
    )

    private val venueVisits = listOf(
        VenueVisit(
            venue = Venue("1", "Venue A"),
            from = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(18, ChronoUnit.HOURS),
            to = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(19, ChronoUnit.HOURS)
        ),
        VenueVisit(
            venue = Venue("1", "Venue B"),
            from = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(16, ChronoUnit.HOURS),
            to = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(19, ChronoUnit.HOURS)
        ),
        VenueVisit(
            venue = Venue("1", "Venue C"),
            from = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(15, ChronoUnit.HOURS),
            to = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(16, ChronoUnit.HOURS)
        ),
        VenueVisit(
            venue = Venue("1", "Venue D"),
            from = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS),
            to = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS)
        ),
        VenueVisit(
            venue = Venue("1", "Venue E"),
            from = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS),
            to = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS)
        ),
        VenueVisit(
            venue = Venue("1", "Venue F"),
            from = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(7, ChronoUnit.HOURS),
            to = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS)
        ),
        VenueVisit(
            venue = Venue("1", "Venue G"),
            from = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(16, ChronoUnit.HOURS),
            to = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(19, ChronoUnit.HOURS)
        )
    )

    private var dialog: ScenarioDialogFragment<*>? = null

    companion object {
        private const val SCENARIOS = "Scenarios ..."
        const val DEBUG_PREFERENCES_NAME = "debugPreferences"
        const val SELECTED_ENVIRONMENT = "SELECTED_ENVIRONMENT"
        const val USE_MOCKED_EXPOSURE_NOTIFICATION = "USE_MOCKED_EXPOSURE_NOTIFICATION"
        const val OFFSET_DAYS = "OFFSET_DAYS"
        const val FILE_SELECT_CODE = 10011
        private val stringIdsSupportedLanguageItem =
            SupportedLanguageItem(nameResId = R.string.show_string_ids, code = FAKE_LOCALE_NAME_FOR_STRING_IDS)

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DebugActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
