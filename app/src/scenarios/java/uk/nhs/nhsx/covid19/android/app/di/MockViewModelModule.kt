package uk.nhs.nhsx.covid19.android.app.di

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataViewModel
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.BaseQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeParser
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel

@Module
class MockViewModelModule {
    @Provides
    fun provideTestResultViewModel(testResultViewModel: TestResultViewModel): BaseTestResultViewModel =
        if (MockTestResultViewModel.currentOptions.useMock) MockTestResultViewModel() else testResultViewModel

    @Provides
    fun provideMyDataViewModel(myDataViewModel: MyDataViewModel): BaseMyDataViewModel =
        if (MockMyDataViewModel.currentOptions.useMock) MockMyDataViewModel() else myDataViewModel

    @Provides
    fun provideQrScannerViewModel(
        qrScannerViewModel: QrScannerViewModel,
        qrCodeParser: QrCodeParser,
        visitedVenuesStorage: VisitedVenuesStorage,
        analyticsEventProcessor: AnalyticsEventProcessor
    ): BaseQrScannerViewModel =
        if (MockQrScannerViewModel.currentOptions.useMock)
            MockQrScannerViewModel(qrCodeParser, visitedVenuesStorage, analyticsEventProcessor)
        else qrScannerViewModel
}
