package uk.nhs.nhsx.covid19.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel

@Module
class ViewModelModule {
    @Provides
    fun provideTestResultViewModel(testResultViewModel: TestResultViewModel): BaseTestResultViewModel =
        testResultViewModel

    @Provides
    fun provideUserDataViewModel(userDataViewModel: MyDataViewModel): BaseMyDataViewModel =
        userDataViewModel
}
