package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance.Builder
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.SimulateGoogleEN
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File
import java.time.Clock
import java.time.Instant

class MockExposureNotificationApi(
    private val context: Context,
    private val clock: Clock,
    simulateGoogleEN: Boolean,
    private val sharedPreferences: SharedPreferences? = null
) : ExposureNotificationApi {

    private var supportsLocationlessScanning = false
    private var _isEnabled: Boolean = false

    private var temporaryExposureKeyHistoryWasCalled = false
    var activationResult: Result = if (simulateGoogleEN) SimulateGoogleEN() else Success()
    var temporaryExposureKeyHistoryResult: Result = if (simulateGoogleEN) SimulateGoogleEN() else Success()

    override suspend fun isEnabled(): Boolean {
        return sharedPreferences?.getBoolean(EN_ENABLED, false) ?: _isEnabled
    }

    fun setEnabled(isEnabled: Boolean) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean(EN_ENABLED, isEnabled).apply()
        } else {
            _isEnabled = isEnabled
        }
    }

    override suspend fun start() {
        val result = this.activationResult
        result.nextResult?.let {
            this.activationResult = it
        }

        when (result) {
            is Success -> setEnabled(true)
            is ResolutionRequired -> {
                setEnabled(result.nextResult is Success)
                throw ApiException(
                    Status(
                        ConnectionResult.RESOLUTION_REQUIRED,
                        "ResolutionRequired",
                        result.pendingIntent
                    )
                )
            }
            is Error -> throw ApiException(result.status)
            is SimulateGoogleEN -> {
                val intent = Intent(context, EnableExposureNotificationActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    ExposureNotificationPermissionHelper.REQUEST_CODE_START_EXPOSURE_NOTIFICATION,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                throw ApiException(Status(ConnectionResult.RESOLUTION_REQUIRED, "ResolutionRequired", pendingIntent))
            }
        }
    }

    override suspend fun stop() {
        setEnabled(false)
        activationResult = SimulateGoogleEN()
    }

    override suspend fun version(): Long = 2

    override suspend fun temporaryExposureKeyHistory(): List<NHSTemporaryExposureKey> {
        temporaryExposureKeyHistoryWasCalled = true

        val result = this.temporaryExposureKeyHistoryResult
        result.nextResult?.let {
            this.temporaryExposureKeyHistoryResult = it
        }

        when (result) {
            is Success -> return listOf(
                NHSTemporaryExposureKey(
                    key = "key",
                    rollingPeriod = 2,
                    rollingStartNumber = 144,
                    daysSinceOnsetOfSymptoms = 5
                )
            )

            is ResolutionRequired -> {
                throw ApiException(
                    Status(
                        ConnectionResult.RESOLUTION_REQUIRED,
                        "ResolutionRequired",
                        result.pendingIntent
                    )
                )
            }

            is Error -> throw ApiException(result.status)
            is SimulateGoogleEN -> {
                val intent = Intent(context, AllowShareKeysActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    ExposureNotificationPermissionHelper.REQUEST_CODE_START_EXPOSURE_NOTIFICATION,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                throw ApiException(Status(ConnectionResult.RESOLUTION_REQUIRED, "ResolutionRequired", pendingIntent))
            }
        }
    }

    fun temporaryExposureKeyHistoryWasCalled() = temporaryExposureKeyHistoryWasCalled

    override suspend fun provideDiagnosisKeys(files: List<File>) = Unit

    var mockExposureWindows: List<ExposureWindow>? = null

    override suspend fun getExposureWindows(): List<ExposureWindow> =
        mockExposureWindows ?: createDefaultExposureWindows()

    private fun createDefaultExposureWindows(): List<ExposureWindow> = listOf(
        ExposureWindow.Builder()
            .setDateMillisSinceEpoch(Instant.now(clock).toEpochMilli())
            .setInfectiousness(Infectiousness.HIGH)
            .setScanInstances(
                listOf(
                    Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                    Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                    Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                    Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                    Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                    Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build()
                )
            )
            .build(),
        ExposureWindow.Builder()
            .setDateMillisSinceEpoch(Instant.now(clock).toEpochMilli())
            .setInfectiousness(Infectiousness.HIGH)
            .setScanInstances(
                listOf(
                    Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                    Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                    Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                    Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                    Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                    Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build()
                )
            )
            .build()
    )

    override suspend fun getDiagnosisKeysDataMapping(): DiagnosisKeysDataMapping =
        DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
            .setDaysSinceOnsetToInfectiousness(mapOf())
            .setReportTypeWhenMissing(ReportType.SELF_REPORT)
            .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.NONE)
            .build()

    override fun setDiagnosisKeysDataMapping(dataMapping: DiagnosisKeysDataMapping) {}

    override suspend fun isAvailable(): Boolean {
        return true
    }

    override suspend fun isRunningNormally(): Boolean {
        return isEnabled()
    }

    fun setDeviceSupportsLocationlessScanning(supportsLocationlessScanning: Boolean) {
        this.supportsLocationlessScanning = supportsLocationlessScanning
    }

    override fun deviceSupportsLocationlessScanning(): Boolean {
        return supportsLocationlessScanning
    }

    sealed class Result {
        abstract val nextResult: Result?

        data class Success(
            override val nextResult: Result? = null
        ) : Result()

        data class ResolutionRequired(
            val pendingIntent: PendingIntent,
            override val nextResult: Result
        ) : Result()

        data class Error(
            val status: Status = Status(ConnectionResult.SERVICE_MISSING),
            override val nextResult: Result? = null
        ) : Result()

        data class SimulateGoogleEN(override val nextResult: Result? = null) : Result()
    }

    companion object {
        const val EN_ENABLED = "EN_ENABLED"
    }
}
