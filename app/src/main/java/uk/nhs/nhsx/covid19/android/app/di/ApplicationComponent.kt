package uk.nhs.nhsx.covid19.android.app.di

import dagger.Component
import uk.nhs.nhsx.covid19.android.app.AnalyticsReportActivity
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryActivity
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataActivity
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsAlarmController
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitOnboardingAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityActivity
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityListener
import uk.nhs.nhsx.covid19.android.app.availability.ApplicationStartAreaRiskUpdater
import uk.nhs.nhsx.covid19.android.app.availability.UpdateRecommendedActivity
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationActivity
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ClearOutdatedData
import uk.nhs.nhsx.covid19.android.app.common.DownloadTasksWorker
import uk.nhs.nhsx.covid19.android.app.common.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.di.module.ApiModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.di.module.ViewModelModule
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWorker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysReminderActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.DataAndPrivacyActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.WelcomeActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.SymptomsAfterRiskyVenueActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.receiver.AlarmRestarter
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationReminderReceiver
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationRetryReceiver
import uk.nhs.nhsx.covid19.android.app.receiver.SubmitAnalyticsAlarmReceiver
import uk.nhs.nhsx.covid19.android.app.receiver.UpdateReceiver
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsActivity
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsProvider
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesActivity
import uk.nhs.nhsx.covid19.android.app.settings.myarea.MyAreaActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.MigrateIsolationState
import uk.nhs.nhsx.covid19.android.app.status.DebugFragment
import uk.nhs.nhsx.covid19.android.app.status.DownloadAreaInfoWorker
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusBaseActivity
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubActivity
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubActivity
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubReminderReceiver
import uk.nhs.nhsx.covid19.android.app.status.localmessage.GetLocalMessageFromStorage
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessageActivity
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubActivity
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.OrderLfdTestActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultOnsetDateActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.UnknownTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.crashreporting.CrashReportProvider
import uk.nhs.nhsx.covid19.android.app.util.crashreporting.RemoteServiceExceptionHandler
import uk.nhs.nhsx.covid19.android.app.widgets.LinkTextView
import uk.nhs.nhsx.covid19.android.app.widgets.LogoView
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        ApiModule::class,
        ViewModelModule::class
    ]
)
interface ApplicationComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(permissionActivity: PermissionActivity)
    fun inject(postCodeActivity: PostCodeActivity)
    fun inject(statusActivity: StatusActivity)
    fun inject(statusBaseActivity: StatusBaseActivity)
    fun inject(enableBluetoothActivity: EnableBluetoothActivity)
    fun inject(enableLocationActivity: EnableLocationActivity)
    fun inject(enableExposureNotificationsActivity: EnableExposureNotificationsActivity)
    fun inject(riskLevelActivity: RiskLevelActivity)
    fun inject(downloadAreaInfoWorker: DownloadAreaInfoWorker)
    fun inject(qrScannerActivity: QrScannerActivity)
    fun inject(questionnaireActivity: QuestionnaireActivity)
    fun inject(testOrderingActivity: TestOrderingActivity)
    fun inject(symptomsAdviceIsolateActivity: SymptomsAdviceIsolateActivity)
    fun inject(isolationExpirationActivity: IsolationExpirationActivity)
    fun inject(reviewSymptomsActivity: ReviewSymptomsActivity)
    fun inject(expirationCheckReceiver: ExpirationCheckReceiver)
    fun inject(exposureNotificationReminderReceiver: ExposureNotificationReminderReceiver)
    fun inject(exposureNotificationRetryReceiver: ExposureNotificationRetryReceiver)
    fun inject(submitAnalyticsAlarmReceiver: SubmitAnalyticsAlarmReceiver)
    fun inject(alarmRestarter: AlarmRestarter)
    fun inject(qrCodeScanResultActivity: QrCodeScanResultActivity)
    fun inject(testResultActivity: TestResultActivity)
    fun inject(submitKeysProgressActivity: SubmitKeysProgressActivity)
    fun inject(exposureNotificationBroadcastReceiver: ExposureNotificationBroadcastReceiver)
    fun inject(myDataActivity: MyDataActivity)
    fun inject(editPostalCodeActivity: EditPostalDistrictActivity)
    fun inject(testOrderingProgressActivity: TestOrderingProgressActivity)
    fun inject(appAvailabilityActivity: AppAvailabilityActivity)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(animationsActivity: AnimationsActivity)
    fun inject(venueHistoryActivity: VenueHistoryActivity)
    fun inject(languagesActivity: LanguagesActivity)
    fun inject(updateReceiver: UpdateReceiver)
    fun inject(venueAlertInformActivity: VenueAlertInformActivity)
    fun inject(venueAlertBookTestActivity: VenueAlertBookTestActivity)
    fun inject(debugFragment: DebugFragment)
    fun inject(linkTestResultActivity: LinkTestResultActivity)
    fun inject(linkTestResultActivity: LinkTestResultSymptomsActivity)
    fun inject(linkTestResultActivity: LinkTestResultOnsetDateActivity)
    fun inject(updateRecommendedActivity: UpdateRecommendedActivity)
    fun inject(logoView: LogoView)
    fun inject(linkTextView: LinkTextView)
    fun inject(downloadTasksWorker: DownloadTasksWorker)
    fun inject(submitOnboardingAnalyticsWorker: SubmitOnboardingAnalyticsWorker)
    fun inject(exposureNotificationWorker: ExposureNotificationWorker)
    fun inject(dataAndPrivacyActivity: DataAndPrivacyActivity)
    fun inject(policyUpdateActivity: PolicyUpdateActivity)
    fun inject(localAuthorityActivity: LocalAuthorityActivity)
    fun inject(batteryOptimizationActivity: BatteryOptimizationActivity)
    fun inject(welcomeActivity: WelcomeActivity)
    fun inject(redirectToIsolationPaymentWebsiteActivity: RedirectToIsolationPaymentWebsiteActivity)
    fun inject(myAreaActivity: MyAreaActivity)
    fun inject(shareKeysInformationActivity: ShareKeysInformationActivity)
    fun inject(shareKeysReminderActivity: ShareKeysReminderActivity)
    fun inject(shareKeysResultActivity: ShareKeysResultActivity)
    fun inject(contactTracingHubActivity: ContactTracingHubActivity)
    fun inject(testingHubActivity: TestingHubActivity)
    fun inject(unknownTestResultActivity: UnknownTestResultActivity)
    fun inject(localMessageActivity: LocalMessageActivity)
    fun inject(symptomsAfterRiskyVenueActivity: SymptomsAfterRiskyVenueActivity)
    fun inject(orderLfdTestActivity: OrderLfdTestActivity)
    fun inject(isolationHubActivity: IsolationHubActivity)
    fun inject(exposureNotificationAgeLimitActivity: ExposureNotificationAgeLimitActivity)
    fun inject(exposureNotificationVaccinationStatusActivity: ExposureNotificationVaccinationStatusActivity)
    fun inject(isolationHubReminderReceiver: IsolationHubReminderReceiver)
    fun inject(riskyContactIsolationAdviceActivity: RiskyContactIsolationAdviceActivity)

    fun inject(testResultViewModel: BaseTestResultViewModel)

    fun inject(analyticsReportActivity: AnalyticsReportActivity)

    fun provideAppAvailabilityListener(): AppAvailabilityListener
    fun providePeriodicTasks(): PeriodicTasks
    fun provideOnboardingCompleted(): OnboardingCompletedProvider
    fun provideApplicationStartAreaRiskUpdater(): ApplicationStartAreaRiskUpdater
    fun provideNotificationProvider(): NotificationProvider
    fun provideBatteryOptimizationChecker(): BatteryOptimizationChecker
    fun provideApplicationLocaleProvider(): ApplicationLocaleProvider
    fun provideExposureNotificationRetryAlarmController(): ExposureNotificationRetryAlarmController
    fun provideSubmitAnalyticsAlarmController(): SubmitAnalyticsAlarmController
    fun provideVisitedVenuesStorage(): VisitedVenuesStorage
    fun provideKeySharingInfoProvider(): KeySharingInfoProvider
    fun provideMigrateIsolationState(): MigrateIsolationState
    fun provideIsolationStateMachine(): IsolationStateMachine
    fun provideRemoteServiceExceptionHandler(): RemoteServiceExceptionHandler
    fun provideCrashReportProvider(): CrashReportProvider
    fun provideAnimationsProvider(): AnimationsProvider
    fun provideGetLocalMessageFromStorage(): GetLocalMessageFromStorage
    fun provideClearOutdatedData(): ClearOutdatedData
}
