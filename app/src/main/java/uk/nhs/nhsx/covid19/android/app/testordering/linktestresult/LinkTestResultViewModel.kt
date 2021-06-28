package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.UnparsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType.UNEXPECTED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.BOTH_PROVIDED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.NEITHER_PROVIDED
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import javax.inject.Inject

class LinkTestResultViewModel @Inject constructor(
    private val ctaTokenValidator: CtaTokenValidator,
    private val isolationStateMachine: IsolationStateMachine,
    private val linkTestResultOnsetDateNeededChecker: LinkTestResultOnsetDateNeededChecker,
    private val receivedUnknownTestResultProvider: ReceivedUnknownTestResultProvider,
    private val clock: Clock
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<LinkTestResultState>()
    fun viewState(): LiveData<LinkTestResultState> = viewStateLiveData

    private val validationOnsetDateNeededLiveData = SingleLiveEvent<ReceivedTestResult>()
    fun validationOnsetDateNeeded(): LiveData<ReceivedTestResult> = validationOnsetDateNeededLiveData

    private val validationCompletedLiveData = SingleLiveEvent<Unit>()
    fun validationCompleted(): LiveData<Unit> = validationCompletedLiveData

    private val confirmedDailyContactTestingNegativeLiveData = SingleLiveEvent<Unit>()
    fun confirmedDailyContactTestingNegative(): LiveData<Unit> =
        confirmedDailyContactTestingNegativeLiveData

    var ctaToken: String? = null

    fun fetchInitialViewState() {
        val state = isolationStateMachine.readLogicalState()
        val showDailyContactTesting = state is PossiblyIsolating &&
            state.isActiveContactCaseOnly(clock) &&
            RuntimeBehavior.isFeatureEnabled(DAILY_CONTACT_TESTING)

        val initialViewState =
            viewStateLiveData.value?.copy(showDailyContactTesting = showDailyContactTesting) ?: LinkTestResultState(
                showDailyContactTesting = showDailyContactTesting
            )

        viewStateLiveData.postValue(initialViewState)
    }

    fun onDailyContactTestingOptInChecked() {
        updateViewState(
            viewStateLiveData.value!!.copy(
                confirmedNegativeDailyContactTestingResult = !confirmedNegativeDailyContactTestingResult()
            )
        )
    }

    fun onContinueButtonClicked() {
        if (viewStateLiveData.value?.showDailyContactTesting == true) {
            evaluateViewState()
        } else {
            validateToken(ctaToken ?: "")
        }
    }

    private fun evaluateViewState() {
        when {
            !ctaToken.isNullOrEmpty() && confirmedNegativeDailyContactTestingResult() ->
                handleError(BOTH_PROVIDED)
            !ctaToken.isNullOrEmpty() && !confirmedNegativeDailyContactTestingResult() ->
                validateToken(ctaToken!!)
            ctaToken.isNullOrEmpty() && !confirmedNegativeDailyContactTestingResult() ->
                handleError(NEITHER_PROVIDED)
            ctaToken.isNullOrEmpty() && confirmedNegativeDailyContactTestingResult() -> {
                updateViewState(viewStateLiveData.value!!.copy(showValidationProgress = false, errorState = null))
                confirmedDailyContactTestingNegativeLiveData.postCall()
            }
        }
    }

    private fun confirmedNegativeDailyContactTestingResult() =
        viewStateLiveData.value?.confirmedNegativeDailyContactTestingResult == true

    private fun validateToken(ctaToken: String) {
        updateViewState(viewStateLiveData.value!!.copy(showValidationProgress = true, errorState = null))
        viewModelScope.launch {
            when (val testResultResponse = ctaTokenValidator.validate(ctaToken)) {
                is Success -> handleTestResultResponse(testResultResponse.virologyCtaExchangeResponse)
                is UnparsableTestResult -> handleUnparsableTestResult()
                is Failure -> handleError(testResultResponse.type.toLinkTestResultError())
            }
        }
    }

    private fun handleUnparsableTestResult() {
        receivedUnknownTestResultProvider.value = true
        validationCompletedLiveData.postCall()
    }

    private fun handleError(error: LinkTestResultError) {
        updateViewState(viewStateLiveData.value!!.copy(showValidationProgress = false, errorState = ErrorState(error)))
    }

    private suspend fun handleTestResultResponse(testResultResponse: VirologyCtaExchangeResponse) {
        val testResult = with(testResultResponse) {
            ReceivedTestResult(
                diagnosisKeySubmissionToken,
                testEndDate,
                testResult,
                testKit,
                diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest,
                confirmatoryDayLimit = confirmatoryDayLimit
            )
        }

        isolationStateMachine.processEvent(
            OnTestResult(
                testResult = testResult,
                showNotification = false,
                testOrderType = OUTSIDE_APP
            )
        )
        if (linkTestResultOnsetDateNeededChecker.isInterestedInAskingForSymptomsOnsetDay(testResult)) {
            validationOnsetDateNeededLiveData.postValue(testResult)
        } else {
            validationCompletedLiveData.postCall()
        }
    }

    private fun updateViewState(updatedViewState: LinkTestResultState) {
        viewStateLiveData.postValue(
            updatedViewState.copy(
                errorState = updatedViewState.errorState?.copy(
                    updated = viewStateLiveData.value!!.errorState?.error != updatedViewState.errorState.error
                )
            )
        )
    }

    data class LinkTestResultState(
        val showDailyContactTesting: Boolean = false,
        val showValidationProgress: Boolean = false,
        val confirmedNegativeDailyContactTestingResult: Boolean = false,
        val errorState: ErrorState? = null
    )

    data class ErrorState(val error: LinkTestResultError, val updated: Boolean = true)

    enum class LinkTestResultError {
        INVALID, NO_CONNECTION, UNEXPECTED, NEITHER_PROVIDED, BOTH_PROVIDED
    }

    private fun ValidationErrorType.toLinkTestResultError() = when (this) {
        INVALID -> LinkTestResultError.INVALID
        NO_CONNECTION -> LinkTestResultError.NO_CONNECTION
        UNEXPECTED -> LinkTestResultError.UNEXPECTED
    }
}
