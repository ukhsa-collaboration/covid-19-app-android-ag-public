package uk.nhs.nhsx.covid19.android.app.qrcode

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import javax.inject.Inject

class QrScannerViewModel(
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
}
