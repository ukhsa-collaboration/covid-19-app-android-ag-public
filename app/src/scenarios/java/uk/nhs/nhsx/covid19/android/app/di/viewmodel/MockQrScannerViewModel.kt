package uk.nhs.nhsx.covid19.android.app.di.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.BaseQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeParser
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import javax.inject.Inject

class MockQrScannerViewModel(
    qrCodeParser: QrCodeParser,
    visitedVenuesStorage: VisitedVenuesStorage,
    delay: Long,
    analyticsEventProcessor: AnalyticsEventProcessor
) : BaseQrScannerViewModel(qrCodeParser, visitedVenuesStorage, delay, analyticsEventProcessor) {

    @Inject
    constructor(
        qrCodeParser: QrCodeParser,
        visitedVenuesStorage: VisitedVenuesStorage,
        analyticsEventProcessor: AnalyticsEventProcessor
    ) : this(qrCodeParser, visitedVenuesStorage, DELAY_TO_SHOW_SCANNING_TEXT_IN_MS, analyticsEventProcessor)

    companion object {
        var currentOptions = Options()
    }

    data class Options(
        val useMock: Boolean = false,
        val loop: Boolean = false,
        val venueList: MutableList<Venue> = mutableListOf()
    )

    override fun getQrCodeScanResult(): LiveData<QrCodeScanResult> {
        runDebugScenario()
        return qrCodeScanResult
    }

    private fun runDebugScenario() {
        if (currentOptions.useMock) {
            if (currentOptions.venueList.size > 0) {
                viewModelScope.launch {
                    delay(2000)
                    val element = currentOptions.venueList.removeAt(0)
                    venueScanned(element)
                    if (currentOptions.loop) {
                        currentOptions.venueList.add(element)
                    }
                }
            }
            currentOptions = currentOptions.copy(
                useMock = currentOptions.venueList.size >= 0
            )
        }
    }
}
