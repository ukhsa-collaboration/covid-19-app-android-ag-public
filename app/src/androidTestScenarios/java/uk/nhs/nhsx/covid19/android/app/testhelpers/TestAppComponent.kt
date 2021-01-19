/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import dagger.Component
import javax.inject.Singleton
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfoProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateStorage
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.DisplayStateExpirationNotification
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider

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
    fun getLocalAuthorityProvider(): LocalAuthorityProvider

    @Singleton
    fun getTestResultsProvider(): TestResultsProvider

    fun provideIsolationStateMachine(): IsolationStateMachine

    fun provideVisitedVenuesStorage(): VisitedVenuesStorage

    @Singleton
    fun getUserInbox(): UserInbox

    @Singleton
    fun getAppAvailabilityProvider(): AppAvailabilityProvider

    fun provideDisplayStateExpirationNotification(): DisplayStateExpirationNotification

    fun getIsolationConfigurationProvider(): IsolationConfigurationProvider

    fun getDownloadAndProcessRiskyVenues(): DownloadAndProcessRiskyVenues

    fun getDownloadVirologyTestResultWork(): DownloadVirologyTestResultWork

    fun getPolicyUpdateStorage(): PolicyUpdateStorage

    @Singleton
    fun getIsolationPaymentTokenStateProvider(): IsolationPaymentTokenStateProvider

    fun getExposureCircuitBreakerInfoProvider(): ExposureCircuitBreakerInfoProvider
}
