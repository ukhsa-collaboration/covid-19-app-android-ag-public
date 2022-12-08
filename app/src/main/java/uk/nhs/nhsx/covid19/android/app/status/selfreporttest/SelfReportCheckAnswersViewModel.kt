package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedSelfReportingTestFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidRememberOnsetSymptomsDateBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.IsPositiveSelfLFDFree
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelfReportedPositiveSelfLFDOnGov
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.exposure.DEFAULT_DIAGNOSIS_KEY_SUBMISSION_TOKEN
import uk.nhs.nhsx.covid19.android.app.state.TrackTestResultAnalyticsOnReceive
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.SubmitAndContinue
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.SymptomsOnset
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.ThankYou
import uk.nhs.nhsx.covid19.android.app.testordering.IsKeySubmissionSupported
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock

class SelfReportCheckAnswersViewModel @AssistedInject constructor(
    private val isKeySubmissionSupported: IsKeySubmissionSupported,
    private val clock: Clock,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val trackTestResultAnalyticsOnReceive: TrackTestResultAnalyticsOnReceive,
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    init {
        viewStateLiveData.postValue(ViewState(selfReportTestQuestions = questions))
    }

    fun changeTestKitTypeClicked() {
        navigateLiveData.postValue(TestKitType(questions))
    }

    fun changeTestOriginClicked() {
        navigateLiveData.postValue(TestOrigin(questions))
    }

    fun changeTestDateClicked() {
        navigateLiveData.postValue(TestDate(questions))
    }

    fun changeSymptomsClicked() {
        navigateLiveData.postValue(Symptoms(questions))
    }

    fun changeSymptomsOnsetClicked() {
        navigateLiveData.postValue(SymptomsOnset(questions))
    }

    fun changeReportedTestClicked() {
        navigateLiveData.postValue(ReportedTest(questions))
    }

    fun onBackPressed() {
        when {
            questions.hasReportedResult != null -> {
                navigateLiveData.postValue(ReportedTest(questions))
            }
            questions.symptomsOnsetDate != null -> {
                navigateLiveData.postValue(SymptomsOnset(questions))
            }
            questions.hadSymptoms != null -> {
                navigateLiveData.postValue(Symptoms(questions))
            }
            else -> {
                navigateLiveData.postValue(TestDate(questions))
            }
        }
    }

    fun onClickSubmitAndContinue() {
        trackRelevantAnalyticsOnContinue()
        if (questions.testType != null && questions.testKitType != null && questions.testEndDate != null) {
            val testResult = ReceivedTestResult(
                diagnosisKeySubmissionToken = DEFAULT_DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                testEndDate = questions.testEndDate.date.atStartOfDay(clock.zone).toInstant(),
                testResult = questions.testType,
                testKitType = questions.testKitType,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false,
                shouldOfferFollowUpTest = false,
                symptomsOnsetDate = SymptomsDate(questions.symptomsOnsetDate?.date),
                isSelfReporting = true
            )
            trackTestResultAnalyticsOnReceive(testResult, OUTSIDE_APP)

            if (isKeySubmissionSupported(testResult, isSelfReportJourney = true)) {
                navigateLiveData.postValue(SubmitAndContinue(questions))
            } else {
                navigateLiveData.postValue(ThankYou(questions.hasReportedResult ?: true, hasSharedSuccessfully = false))
            }
        } else {
            navigateLiveData.postValue(SubmitAndContinue(questions))
        }
    }

    private fun trackRelevantAnalyticsOnContinue() {
        if (questions.hadSymptoms == true) {
            analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult)
        }
        if (questions.symptomsOnsetDate?.rememberedDate == true) {
            analyticsEventProcessor.track(DidRememberOnsetSymptomsDateBeforeReceivedTestResult)
        }
        if (questions.isNHSTest == true) {
            analyticsEventProcessor.track(IsPositiveSelfLFDFree)
        }
        if (questions.hasReportedResult == true) {
            analyticsEventProcessor.track(SelfReportedPositiveSelfLFDOnGov)
        }
        if (questions.temporaryExposureKeys != null) {
            analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow)
        }
        analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow)
        analyticsEventProcessor.track(CompletedSelfReportingTestFlow)
    }

    data class ViewState(val selfReportTestQuestions: SelfReportTestQuestions)

    sealed class NavigationTarget {
        data class TestKitType(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestOrigin(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestDate(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class Symptoms(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class SymptomsOnset(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ReportedTest(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class SubmitAndContinue(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ThankYou(val hasReported: Boolean, val hasSharedSuccessfully: Boolean) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelfReportCheckAnswersViewModel
    }
}
