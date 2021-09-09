package uk.nhs.nhsx.covid19.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.qrcode.BaseQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerViewModel

@Module
class QrScannerViewModelModule {
    @Provides
    fun provideQrScannerViewModel(qrScannerViewModel: QrScannerViewModel): BaseQrScannerViewModel = qrScannerViewModel
}
