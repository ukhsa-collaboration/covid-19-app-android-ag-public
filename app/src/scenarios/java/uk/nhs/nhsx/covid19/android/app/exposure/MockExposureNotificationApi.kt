package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File
import java.util.Date

class MockExposureNotificationApi : ExposureNotificationApi {

    private var isEnabled = false
    private var canBeChanged = true
    private var temporaryExposureKeyHistoryWasCalled = false

    override suspend fun isEnabled(): Boolean {
        return isEnabled
    }

    override suspend fun start() {
        if (canBeChanged) {
            isEnabled = true
        }
    }

    override suspend fun stop() {
        if (canBeChanged) {
            this.isEnabled = false
        }
    }

    fun setEnabled(isEnabled: Boolean, canBeChanged: Boolean = true) {
        this.isEnabled = isEnabled
        this.canBeChanged = canBeChanged
    }

    override suspend fun temporaryExposureKeyHistory(): List<NHSTemporaryExposureKey> {
        temporaryExposureKeyHistoryWasCalled = true
        return listOf(
            NHSTemporaryExposureKey(
                key = "key",
                rollingPeriod = 2,
                rollingStartNumber = 144
            )
        )
    }

    fun temporaryExposureKeyHistoryWasCalled() = temporaryExposureKeyHistoryWasCalled

    override suspend fun provideDiagnosisKeys(
        files: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    ) = Unit

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

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return ExposureSummary.ExposureSummaryBuilder().build()
    }
}
