package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SuccessfullySharedExposureKeys
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.DEFAULT_DIAGNOSIS_KEY_SUBMISSION_TOKEN
import uk.nhs.nhsx.covid19.android.app.exposure.MIN_TRANSMISSION_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.TransmissionRiskLevelApplier
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget.ThankYou
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgeTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.lang.IllegalStateException
import java.time.Clock
import java.time.Instant

class SelfReportSubmitTestResultAndKeysProgressViewModel @AssistedInject constructor(
    private val submitTemporaryExposureKeys: SubmitTemporaryExposureKeys,
    private val transmissionRiskLevelApplier: TransmissionRiskLevelApplier,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val clock: Clock,
    private val isolationStateMachine: IsolationStateMachine,
    private val acknowledgeTestResult: AcknowledgeTestResult,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private val submitKeysLiveData: MutableLiveData<Lce<Unit>> = SingleLiveEvent()
    fun submitKeysResult(): LiveData<Lce<Unit>> = submitKeysLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var wasReceivedAndAcknowledged = false
    private var initialKeySharingInfoAcknowledgeDate: Instant? = null
    private var job: Job? = null

    fun submitTestResultAndKeys() {
        runCatching {
            submitKeysLiveData.value = Lce.Loading
            if (questions.testType != null && questions.testKitType != null && questions.testEndDate != null) {
                receiveAndAcknowledgeTestResultIfNeeded(questions.testType, questions.testKitType, questions.testEndDate)
                if (questions.temporaryExposureKeys != null) {
                    filterAndShareKeys(questions.temporaryExposureKeys)
                } else {
                    onKeySharingDeclined()
                }
            } else {
                submitKeysLiveData.postValue(Lce.Error(IllegalStateException("test type, kit or date not set")))
            }
        }.onFailure {
            submitKeysLiveData.postValue(Lce.Error(it))
        }
    }

    fun onBackPressed() {
        job?.cancel()
        if (questions.testType != null && questions.testKitType != null && questions.testEndDate != null) {
            val keysSharingInfo = keySharingInfoProvider.keySharingInfo
            if (wasReceivedAndAcknowledged && keysSharingInfo != null && initialKeySharingInfoAcknowledgeDate != keysSharingInfo.acknowledgedDate) {
                keySharingInfoProvider.setHasDeclinedSharingKeys()
                navigateLiveData.postValue(ThankYou(questions.hasReportedResult ?: true, hasSharedSuccessfully = false))
            } else {
                navigateLiveData.postValue(Finish)
            }
        } else {
            navigateLiveData.postValue(Finish)
        }
    }

    fun onSuccess() {
        keySharingInfoProvider.reset()
        analyticsEventProcessor.track(SuccessfullySharedExposureKeys)
        navigateLiveData.postValue(ThankYou(questions.hasReportedResult ?: true, hasSharedSuccessfully = true))
    }

    private fun receiveAndAcknowledgeTestResultIfNeeded(
        testType: VirologyTestResult,
        testKitType: VirologyTestKitType,
        testEndDate: ChosenDate
    ) {
        if (!wasReceivedAndAcknowledged) {
            initialKeySharingInfoAcknowledgeDate = keySharingInfoProvider.keySharingInfo?.acknowledgedDate
        } else {
            val keysSharingInfo = keySharingInfoProvider.keySharingInfo
            if (keysSharingInfo != null && initialKeySharingInfoAcknowledgeDate != keysSharingInfo.acknowledgedDate) {
                return
            }
        }

        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = DEFAULT_DIAGNOSIS_KEY_SUBMISSION_TOKEN,
            testEndDate = testEndDate.date.atStartOfDay(clock.zone).toInstant(),
            testResult = testType,
            testKitType = testKitType,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false,
            symptomsOnsetDate = SymptomsDate(questions.symptomsOnsetDate?.date),
            isSelfReporting = true
        )

        if (!wasReceivedAndAcknowledged) {
            wasReceivedAndAcknowledged = true
            acknowledgeTestResult(testResult)
        } else {
            isolationStateMachine.processEvent(OnTestResultAcknowledge(testResult))
        }
    }

    private fun filterAndShareKeys(temporaryExposureKeys: List<NHSTemporaryExposureKey>) {
        val keysSharingInfo = keySharingInfoProvider.keySharingInfo
        if (keysSharingInfo != null && initialKeySharingInfoAcknowledgeDate != keysSharingInfo.acknowledgedDate) {
            val filteredTemporaryExposureKeys =
                applyTransmissionRiskLevelsAndFilter(temporaryExposureKeys, keysSharingInfo)
           job = viewModelScope.launch {
                val viewState = when (val result =
                    submitTemporaryExposureKeys(
                        exposureKeys = filteredTemporaryExposureKeys, isPrivateJourney = true,
                        testKit = questions.testKitType.toString()
                    )) {
                    is Failure -> Lce.Error(result.throwable)
                    is Success -> Lce.Success(Unit)
                }
               submitKeysLiveData.postValue(viewState)
            }
        } else {
            submitKeysLiveData.postValue(Lce.Error(IllegalStateException("keysSharingInfo not set")))
        }
    }

    private fun applyTransmissionRiskLevelsAndFilter(
        temporaryExposureKeys: List<NHSTemporaryExposureKey>,
        keysSharingInfo: KeySharingInfo
    ): List<NHSTemporaryExposureKey> {
        return transmissionRiskLevelApplier.applyTransmissionRiskLevels(
            temporaryExposureKeys,
            keysSharingInfo
        ).filter {
            it.transmissionRiskLevel != null &&
                    it.transmissionRiskLevel > MIN_TRANSMISSION_RISK_LEVEL &&
                    it.rollingPeriod == 144
        }
    }

    private fun onKeySharingDeclined() {
        val keysSharingInfo = keySharingInfoProvider.keySharingInfo
        if (keysSharingInfo != null && keysSharingInfo.wasAcknowledgedMoreThan24HoursAgo(clock)) {
            keySharingInfoProvider.reset()
        } else {
            keySharingInfoProvider.setHasDeclinedSharingKeys()
        }
        navigateLiveData.postValue(ThankYou(questions.hasReportedResult ?: true, hasSharedSuccessfully = false))
    }

    sealed class NavigationTarget {
        data class ThankYou(val hasReported: Boolean, val hasSharedSuccessfully: Boolean) : NavigationTarget()
        object Finish : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelfReportSubmitTestResultAndKeysProgressViewModel
    }
}
