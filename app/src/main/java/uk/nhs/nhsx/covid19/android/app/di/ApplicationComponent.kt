package uk.nhs.nhsx.covid19.android.app.di

import dagger.Component
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.about.UserDataActivity
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsAggregatorReceiver
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsAlarm
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityActivity
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityListener
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityWorker
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ClearOutdatedDataWorker
import uk.nhs.nhsx.covid19.android.app.common.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.di.module.ApiModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitKeysWorker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInitialWorker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposurePollingCircuitBreakerWorker
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysWorker
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenuesWorker
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenuesCircuitBreakerInitialWorker
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenuesCircuitBreakerPollingWorker
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.receiver.AlarmRestarter
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationReminderReceiver
import uk.nhs.nhsx.covid19.android.app.receiver.UpdateReceiver
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.status.DebugFragment
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWorker
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusBaseActivity
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWorker
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingProgressActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.widgets.LinkTextView
import uk.nhs.nhsx.covid19.android.app.widgets.LogoView

import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        ApiModule::class
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
    fun inject(downloadKeysWorker: DownloadKeysWorker)
    fun inject(downloadRiskyPostCodesWorker: DownloadRiskyPostCodesWorker)
    fun inject(downloadAndProcessRiskyVenues: DownloadAndProcessRiskyVenuesWorker)
    fun inject(downloadVirologyTestResultWorker: DownloadVirologyTestResultWorker)
    fun inject(exposureCircuitBreakerInitialWorker: ExposureCircuitBreakerInitialWorker)
    fun inject(exposurePollingCircuitBreakerWorker: ExposurePollingCircuitBreakerWorker)
    fun inject(riskyVenuesCircuitBreakerInitialWorker: RiskyVenuesCircuitBreakerInitialWorker)
    fun inject(riskyVenuesCircuitBreakerPollingWorker: RiskyVenuesCircuitBreakerPollingWorker)
    fun inject(qrScannerActivity: QrScannerActivity)
    fun inject(questionnaireActivity: QuestionnaireActivity)
    fun inject(testOrderingActivity: TestOrderingActivity)
    fun inject(symptomsAdviceIsolateActivity: SymptomsAdviceIsolateActivity)
    fun inject(isolationExpirationActivity: IsolationExpirationActivity)
    fun inject(reviewSymptomsActivity: ReviewSymptomsActivity)
    fun inject(expirationCheckReceiver: ExpirationCheckReceiver)
    fun inject(exposureNotificationReminderReceiver: ExposureNotificationReminderReceiver)
    fun inject(alarmRestarter: AlarmRestarter)
    fun inject(qrCodeScanResultActivity: QrCodeScanResultActivity)
    fun inject(encounterDetectionActivity: EncounterDetectionActivity)
    fun inject(testResultActivity: TestResultActivity)
    fun inject(shareKeysInformationActivity: ShareKeysInformationActivity)
    fun inject(submitKeysProgressActivity: SubmitKeysProgressActivity)
    fun inject(exposureNotificationBroadcastReceiver: ExposureNotificationBroadcastReceiver)
    fun inject(userDataActivity: UserDataActivity)
    fun inject(editPostalCodeActivity: EditPostalDistrictActivity)
    fun inject(submitKeysWorker: SubmitKeysWorker)
    fun inject(clearOutdatedDataWorker: ClearOutdatedDataWorker)
    fun inject(appAvailabilityWorker: AppAvailabilityWorker)
    fun inject(testOrderingProgressActivity: TestOrderingProgressActivity)
    fun inject(appAvailabilityActivity: AppAvailabilityActivity)
    fun inject(updateReceiver: UpdateReceiver)
    fun inject(venueAlertActivity: VenueAlertActivity)
    fun inject(submitAnalyticsWorker: SubmitAnalyticsWorker)
    fun inject(debugFragment: DebugFragment)
    fun inject(linkTestResultActivity: LinkTestResultActivity)
    fun inject(logoView: LogoView)
    fun inject(linkTextView: LinkTextView)
    fun inject(analyticsAggregatorReceiver: AnalyticsAggregatorReceiver)

    fun provideAppAvailabilityListener(): AppAvailabilityListener
    fun provideAnalyticsReminder(): AnalyticsAlarm
}
