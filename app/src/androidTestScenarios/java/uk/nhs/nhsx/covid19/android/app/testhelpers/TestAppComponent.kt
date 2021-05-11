/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.app.AlarmManager
import dagger.Component
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsAlarmController
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.MockViewModelModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfoProvider
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateStorage
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.DisplayStateExpirationNotification
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        ManagedApiModule::class,
        MockViewModelModule::class
    ]
)
interface TestAppComponent : ApplicationComponent {
    @Singleton
    fun getPostCodeProvider(): PostCodeProvider

    @Singleton
    fun getLocalAuthorityProvider(): LocalAuthorityProvider

    @Singleton
    fun getUnacknowledgedTestResultsProvider(): UnacknowledgedTestResultsProvider

    @Singleton
    fun getTestOrderingTokensProvider(): TestOrderingTokensProvider

    @Singleton
    fun getKeySharingInfoProvider(): KeySharingInfoProvider

    fun provideIsolationStateMachine(): IsolationStateMachine

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

    @Singleton
    fun getLastVisitedBookTestTypeVenueDateProvider(): LastVisitedBookTestTypeVenueDateProvider

    @Singleton
    fun getSubmitAnalyticsAlarmController(): SubmitAnalyticsAlarmController

    @Singleton
    fun getAlarmManager(): AlarmManager
}
