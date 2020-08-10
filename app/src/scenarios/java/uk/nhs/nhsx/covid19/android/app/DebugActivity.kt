package uk.nhs.nhsx.covid19.android.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import kotlinx.android.synthetic.scenarios.activity_debug.buttonFeatureFlags
import kotlinx.android.synthetic.scenarios.activity_debug.enableBluetooth
import kotlinx.android.synthetic.scenarios.activity_debug.enableExposureNotifications
import kotlinx.android.synthetic.scenarios.activity_debug.enableLocationService
import kotlinx.android.synthetic.scenarios.activity_debug.environmentSpinner
import kotlinx.android.synthetic.scenarios.activity_debug.exposureNotificationMocks
import kotlinx.android.synthetic.scenarios.activity_debug.fieldTests
import kotlinx.android.synthetic.scenarios.activity_debug.qrScanner
import kotlinx.android.synthetic.scenarios.activity_debug.qrScannerFailure
import kotlinx.android.synthetic.scenarios.activity_debug.qrScannerMoreInfo
import kotlinx.android.synthetic.scenarios.activity_debug.qrScannerNotSupported
import kotlinx.android.synthetic.scenarios.activity_debug.qrScannerPermissionNotGranted
import kotlinx.android.synthetic.scenarios.activity_debug.qrScannerSuccess
import kotlinx.android.synthetic.scenarios.activity_debug.questionnaireScreen
import kotlinx.android.synthetic.scenarios.activity_debug.scenarioOnboarding
import kotlinx.android.synthetic.scenarios.activity_debug.scenario_main
import kotlinx.android.synthetic.scenarios.activity_debug.statusScreen
import kotlinx.android.synthetic.scenarios.activity_debug.testResultActivity
import kotlinx.android.synthetic.scenarios.activity_debug.testingInformationScreen
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.EnvironmentConfiguration
import uk.nhs.covid19.config.Remote
import uk.nhs.covid19.config.production
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.common.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.di.DaggerMockApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.GoogleExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.featureflag.testsettings.TestSettingsActivity
import uk.nhs.nhsx.covid19.android.app.fieldtests.FieldTestsActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeMoreInfoActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidLocationStateProvider
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity

class DebugActivity : AppCompatActivity(R.layout.activity_debug) {

    private lateinit var selectedEnvironment: EnvironmentConfiguration
    private lateinit var debugSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)

        debugSharedPreferences = getSharedPreferences(DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

        val environments = mutableListOf<EnvironmentConfiguration>().apply {
            add(
                EnvironmentConfiguration(
                    name = "Mock",
                    distributedRemote = Remote("localhost", path = null),
                    apiRemote = Remote("localhost", path = null)
                )
            )
            addAll(Configurations.allConfigs)
            add(production)
        }
        val environmentsAdapter = EnvironmentAdapter(this, environments)

        environmentSpinner.adapter = environmentsAdapter
        environmentSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                debugSharedPreferences.edit().putInt(SELECTED_ENVIRONMENT, position).apply()
                selectedEnvironment = environments[position]
                val isMockNetwork = position == 0

                setApplicationComponent(isMockNetwork, exposureNotificationMocks.isChecked)
            }
        }

        val selectedEnvironment = debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0)
            .coerceIn(0, environments.size - 1)
        environmentSpinner.setSelection(selectedEnvironment)

        buttonFeatureFlags.setOnClickListener {
            startActivity(Intent(this, TestSettingsActivity::class.java))
        }

        scenario_main.setOnClickListener {
            finish()
            MainActivity.start(this)
        }

        scenarioOnboarding.setOnClickListener {
            finish()
            AuthenticationCodeActivity.start(this)
        }

        fieldTests.setOnClickListener {
            startActivity<FieldTestsActivity>()
        }

        statusScreen.setOnClickListener {
            startActivity<StatusActivity>()
        }

        qrScanner.setOnClickListener {
            startActivity<QrScannerActivity>()
        }

        enableBluetooth.setOnClickListener {
            EnableBluetoothActivity.start(this)
        }

        enableLocationService.setOnClickListener {
            EnableLocationActivity.start(this)
        }

        enableExposureNotifications.setOnClickListener {
            EnableExposureNotificationsActivity.start(this)
        }

        qrScannerSuccess.setOnClickListener {
            QrCodeScanResultActivity.start(this, QrCodeScanResult.Success("Sample Venue"))
        }

        qrScannerFailure.setOnClickListener {
            QrCodeScanResultActivity.start(this, QrCodeScanResult.InvalidContent)
        }

        qrScannerPermissionNotGranted.setOnClickListener {
            QrCodeScanResultActivity.start(this, QrCodeScanResult.CameraPermissionNotGranted)
        }

        qrScannerNotSupported.setOnClickListener {
            QrCodeScanResultActivity.start(this, QrCodeScanResult.ScanningNotSupported)
        }

        qrScannerMoreInfo.setOnClickListener {
            startActivity<QrCodeMoreInfoActivity>()
        }

        questionnaireScreen.setOnClickListener {
            startActivity<QuestionnaireActivity>()
        }

        testingInformationScreen.setOnClickListener {
            startActivity<TestOrderingActivity>()
        }

        testResultActivity.setOnClickListener {
            startActivity<TestResultActivity>()
        }

        exposureNotificationMocks.setOnCheckedChangeListener { _, isChecked ->
            setApplicationComponent(useMockNetwork = environmentSpinner.selectedItemPosition == 0, useMockExposureApi = isChecked)
        }
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

    private fun getConfiguration(): EnvironmentConfiguration = selectedEnvironment

    private fun setApplicationComponent(
        useMockNetwork: Boolean,
        useMockExposureApi: Boolean
    ) =
        if (useMockNetwork) useMockApplicationComponent(useMockExposureApi) else useRegularApplicationComponent(useMockExposureApi)

    private fun useRegularApplicationComponent(useMockExposureApi: Boolean) {
        app.buildAndUseAppComponent(NetworkModule(getConfiguration()), getExposureNotificationApi(useMockExposureApi))
    }

    private fun useMockApplicationComponent(useMockExposureApi: Boolean) {
        app.appComponent =
            DaggerMockApplicationComponent.builder()
                .appModule(
                    AppModule(
                        applicationContext,
                        getExposureNotificationApi(useMockExposureApi),
                        AndroidBluetoothStateProvider(),
                        AndroidLocationStateProvider(),
                        (application as ExposureApplication)
                            .createEncryptedSharedPreferences(
                                "mockedEncryptedSharedPreferences"
                            ),
                        qrCodesSignatureKey
                    )
                )
                .mockApiModule(MockApiModule())
                .networkModule(NetworkModule(getConfiguration()))
                .build()
        app.updateLifecycleListener()
    }

    private fun getExposureNotificationApi(useMockExposureApi: Boolean) =
        if (useMockExposureApi) MockExposureNotificationApi() else GoogleExposureNotificationApi(
            this
        )

    companion object {
        const val DEBUG_PREFERENCES_NAME = "debugPreferences"
        const val SELECTED_ENVIRONMENT = "SELECTED_ENVIRONMENT"

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DebugActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
