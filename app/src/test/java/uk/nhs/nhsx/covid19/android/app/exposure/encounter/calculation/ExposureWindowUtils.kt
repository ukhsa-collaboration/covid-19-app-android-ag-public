package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.ExposureWindow.Builder
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import java.time.Instant

class ExposureWindowUtils {
    companion object {
        private val baseDate: Instant = Instant.parse("2020-07-20T00:00:00Z")
        private val someScanInstance = getGoogleScanInstance(50)

        fun getExposureWindow(
            scanInstances: List<ScanInstance> = listOf(
                someScanInstance
            ),
            millisSinceEpoch: Long = baseDate.toEpochMilli(),
            infectiousness: Int = Infectiousness.HIGH
        ): ExposureWindow {
            return Builder().apply {
                setDateMillisSinceEpoch(millisSinceEpoch)
                setReportType(ReportType.CONFIRMED_TEST)
                setScanInstances(scanInstances)
                setInfectiousness(infectiousness)
            }.build()
        }

        fun getExposureWindowWithRisk(
            calculatedRisk: Double,
            scanInstances: List<ScanInstance> = listOf(
                someScanInstance
            ),
            millisSinceEpoch: Long = baseDate.toEpochMilli(),
            infectiousness: Int = Infectiousness.HIGH
        ): ExposureWindowWithRisk {
            return ExposureWindowWithRisk(
                getExposureWindow(scanInstances, millisSinceEpoch, infectiousness),
                calculatedRisk,
                riskCalculationVersion = 2,
                matchedKeyCount = 1
            )
        }

        fun getGoogleScanInstance(
            minAttenuation: Int,
            secondsSinceLastScan: Int = 180,
            typicalAttenuation: Int = 68
        ): ScanInstance = ScanInstance.Builder().apply {
            setMinAttenuationDb(minAttenuation)
            setSecondsSinceLastScan(secondsSinceLastScan)
            setTypicalAttenuationDb(typicalAttenuation)
        }.build()
    }
}
