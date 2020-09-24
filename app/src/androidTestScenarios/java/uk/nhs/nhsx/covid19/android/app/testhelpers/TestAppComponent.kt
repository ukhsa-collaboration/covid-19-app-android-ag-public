/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import dagger.Component
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        ManagedApiModule::class
    ]
)
interface TestAppComponent : ApplicationComponent {
    @Singleton
    fun getPostCodeProvider(): PostCodeProvider

    @Singleton
    fun getTestResultsProvider(): TestResultsProvider

    fun provideIsolationStateMachine(): IsolationStateMachine

    fun provideVisitedVenuesStorage(): VisitedVenuesStorage

    @Singleton
    fun getUserInbox(): UserInbox

    fun providePeriodicTasks(): PeriodicTasks

    fun getIsolationConfigurationProvider(): IsolationConfigurationProvider

    fun getDownloadAndProcessRiskyVenues(): DownloadAndProcessRiskyVenues

    fun getDownloadVirologyTestResultWork(): DownloadVirologyTestResultWork
}
