package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File
import java.util.Date

class MockExposureNotificationApi : ExposureNotificationApi {

    private var supportsLocationlessScanning = false
    private var isEnabled = false
    private var temporaryExposureKeyHistoryWasCalled = false

    override suspend fun isEnabled(): Boolean {
        return isEnabled
    }

    override suspend fun start() {
        isEnabled = true
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
        return listOf(
            NHSTemporaryExposureKey(
                key = "key",
                rollingPeriod = 2,
                rollingStartNumber = 144,
                daysSinceOnsetOfSymptoms = 5
            )
        )
    }

    fun temporaryExposureKeyHistoryWasCalled() = temporaryExposureKeyHistoryWasCalled

    override suspend fun provideDiagnosisKeys(
        files: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    ) = Unit

    override suspend fun provideDiagnosisKeys(files: List<File>) = Unit

    override suspend fun getExposureInformation(token: String): List<ExposureInformation> {
        return listOf(
            ExposureInformation
                .ExposureInformationBuilder()
                .setAttenuationDurations(intArrayOf(1000))
                .setTotalRiskScore(1000)
                .setDateMillisSinceEpoch(Date().time)
                .build()
        )
    }

    override suspend fun getExposureWindows(): List<ExposureWindow> {
        return listOf(
            ExposureWindow.Builder()
                .setDateMillisSinceEpoch(Date().time)
                .setInfectiousness(Infectiousness.HIGH)
                .setScanInstances(
                    listOf(
                        ScanInstance.Builder().setMinAttenuationDb(40).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                        ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build()
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

    fun setDeviceSupportsLocationlessScanning(supportsLocationlessScanning: Boolean) {
        this.supportsLocationlessScanning = supportsLocationlessScanning
    }

    override fun deviceSupportsLocationlessScanning(): Boolean {
        return supportsLocationlessScanning
    }
}
