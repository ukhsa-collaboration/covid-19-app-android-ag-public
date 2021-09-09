package uk.nhs.nhsx.covid19.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.BaseQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeParser
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage

@Module
class QrScannerViewModelModule {
    @Provides
    fun provideQrScannerViewModel(
        qrScannerViewModel: QrScannerViewModel, qrCodeParser: QrCodeParser,
        visitedVenuesStorage: VisitedVenuesStorage,
        analyticsEventProcessor: AnalyticsEventProcessor
    ): BaseQrScannerViewModel =
        if (MockQrScannerViewModel.currentOptions.useMock)
            MockQrScannerViewModel(qrCodeParser, visitedVenuesStorage, analyticsEventProcessor)
        else qrScannerViewModel
}
