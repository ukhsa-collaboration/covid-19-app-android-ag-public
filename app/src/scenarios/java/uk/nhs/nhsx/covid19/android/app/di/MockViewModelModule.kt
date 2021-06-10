package uk.nhs.nhsx.covid19.android.app.di

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataViewModel
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockTestResultViewModel
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
}
