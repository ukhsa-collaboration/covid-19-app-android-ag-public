package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.PendingIntent
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File
import java.time.Clock
import java.time.Instant

class MockExposureNotificationApi(private val clock: Clock) : ExposureNotificationApi {

    private var supportsLocationlessScanning = false
    private var isEnabled = false
    private var temporaryExposureKeyHistoryWasCalled = false
    var activationResult: Result = Success()
    var temporaryExposureKeyHistoryResult: Result = Success()

    override suspend fun isEnabled(): Boolean {
        return isEnabled
    }

    override suspend fun start() {
        val result = this.activationResult
        result.nextResult?.let {
            this.activationResult = it
        }

        when (result) {
            is Success -> isEnabled = true
            is ResolutionRequired -> {
                isEnabled = result.nextResult is Success
                throw ApiException(Status(ConnectionResult.RESOLUTION_REQUIRED, "ResolutionRequired", result.pendingIntent))
            }
            is Error -> throw ApiException(result.status)
        }
    }

    override suspend fun stop() {
        isEnabled = false
    }

    override suspend fun version(): Long? = 2

    fun setEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
    }

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
                throw ApiException(Status(ConnectionResult.RESOLUTION_REQUIRED, "ResolutionRequired", result.pendingIntent))
            }

            is Error -> throw ApiException(result.status)
        }
    }

    fun temporaryExposureKeyHistoryWasCalled() = temporaryExposureKeyHistoryWasCalled

    override suspend fun provideDiagnosisKeys(files: List<File>) = Unit

    override suspend fun getExposureWindows(): List<ExposureWindow> {
        return listOf(
            ExposureWindow.Builder()
                .setDateMillisSinceEpoch(Instant.now(clock).toEpochMilli())
                .setInfectiousness(Infectiousness.HIGH)
                .setScanInstances(
                    listOf(
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build()
                    )
                )
                .build(),
            ExposureWindow.Builder()
                .setDateMillisSinceEpoch(Instant.now(clock).toEpochMilli())
                .setInfectiousness(Infectiousness.HIGH)
                .setScanInstances(
                    listOf(
                        ScanInstance.Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                        ScanInstance.Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                        ScanInstance.Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                        ScanInstance.Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                        ScanInstance.Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build(),
                        ScanInstance.Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(1).build()
                    )
                )
                .build()
        )
    }

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
    }
}
