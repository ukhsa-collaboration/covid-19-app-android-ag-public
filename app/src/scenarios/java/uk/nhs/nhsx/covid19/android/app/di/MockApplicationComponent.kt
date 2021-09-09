package uk.nhs.nhsx.covid19.android.app.di

import dagger.Component
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.di.module.QrScannerViewModelModule
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, NetworkModule::class, MockApiModule::class, MockViewModelModule::class, QrScannerViewModelModule::class])
interface MockApplicationComponent : ApplicationComponent
