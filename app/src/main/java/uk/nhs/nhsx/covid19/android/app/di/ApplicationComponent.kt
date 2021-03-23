package uk.nhs.nhsx.covid19.android.app.di

import dagger.Component
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.about.MyDataActivity
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryActivity
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
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWorker
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
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformActivity
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
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesActivity
import uk.nhs.nhsx.covid19.android.app.settings.myarea.MyAreaActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.status.DebugFragment
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWorker
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusBaseActivity
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.DailyContactTestingConfirmationActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultOnsetDateActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.widgets.IsolationStatusView
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
    fun inject(activity: MainActivity)
    fun inject(activity: PermissionActivity)
    fun inject(activity: PostCodeActivity)
    fun inject(activity: StatusActivity)
    fun inject(activity: StatusBaseActivity)
    fun inject(activity: EnableBluetoothActivity)
    fun inject(activity: EnableLocationActivity)
    fun inject(activity: EnableExposureNotificationsActivity)
    fun inject(riskLevelActivity: RiskLevelActivity)
    fun inject(downloadRiskyPostCodesWorker: DownloadRiskyPostCodesWorker)
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
    fun inject(encounterDetectionActivity: EncounterDetectionActivity)
    fun inject(testResultActivity: TestResultActivity)
    fun inject(shareKeysInformationActivity: ShareKeysInformationActivity)
    fun inject(submitKeysProgressActivity: SubmitKeysProgressActivity)
    fun inject(exposureNotificationBroadcastReceiver: ExposureNotificationBroadcastReceiver)
    fun inject(myDataActivity: MyDataActivity)
    fun inject(editPostalCodeActivity: EditPostalDistrictActivity)
    fun inject(testOrderingProgressActivity: TestOrderingProgressActivity)
    fun inject(appAvailabilityActivity: AppAvailabilityActivity)
    fun inject(settingsActivity: SettingsActivity)
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
    fun inject(isolationStatusView: IsolationStatusView)
    fun inject(downloadTasksWorker: DownloadTasksWorker)
    fun inject(submitOnboardingAnalyticsWorker: SubmitOnboardingAnalyticsWorker)
    fun inject(exposureNotificationWorker: ExposureNotificationWorker)
    fun inject(dataAndPrivacyActivity: DataAndPrivacyActivity)
    fun inject(policyUpdateActivity: PolicyUpdateActivity)
    fun inject(localAuthorityActivity: LocalAuthorityActivity)
    fun inject(batteryOptimizationActivity: BatteryOptimizationActivity)
    fun inject(welcomeActivity: WelcomeActivity)
    fun inject(redirectToIsolationPaymentWebsiteActivity: RedirectToIsolationPaymentWebsiteActivity)
    fun inject(dailyContactTestingConfirmationActivity: DailyContactTestingConfirmationActivity)
    fun inject(myAreaActivity: MyAreaActivity)

    fun inject(testResultViewModel: BaseTestResultViewModel)

    fun provideAppAvailabilityListener(): AppAvailabilityListener
    fun providePeriodicTasks(): PeriodicTasks
    fun provideOnboardingCompleted(): OnboardingCompletedProvider
    fun provideApplicationStartAreaRiskUpdater(): ApplicationStartAreaRiskUpdater
    fun provideNotificationProvider(): NotificationProvider
    fun provideBatteryOptimizationChecker(): BatteryOptimizationChecker
    fun provideApplicationLocaleProvider(): ApplicationLocaleProvider
    fun provideExposureNotificationRetryAlarmController(): ExposureNotificationRetryAlarmController
    fun provideSubmitAnalyticsAlarmController(): SubmitAnalyticsAlarmController
}
