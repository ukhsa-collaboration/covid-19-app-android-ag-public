package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Error
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Progress
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Valid
import javax.inject.Inject

class LinkTestResultViewModel @Inject constructor(
    private val ctaTokenValidator: CtaTokenValidator,
    private val isolationStateMachine: IsolationStateMachine
) : ViewModel() {

    private val linkTestResultLiveData = MutableLiveData<LinkTestResultViewState>()
    fun viewState(): LiveData<LinkTestResultViewState> = linkTestResultLiveData

    fun validate(ctaToken: String) {
        linkTestResultLiveData.postValue(Progress)
        val cleanedCtaToken = ctaToken.replace(CROCKFORD_BASE32_REGEX.toRegex(), "")
        viewModelScope.launch {
            when (val testResultResponse = ctaTokenValidator.validate(cleanedCtaToken)) {
                is Success -> handleTestResultResponse(testResultResponse.virologyCtaExchangeResponse)
                is Failure -> linkTestResultLiveData.postValue(Error(testResultResponse.type))
            }
        }
    }

    private fun handleTestResultResponse(testResultResponse: VirologyCtaExchangeResponse) {
        isolationStateMachine.processEvent(
            OnTestResult(
                testResult = ReceivedTestResult(
                    testResultResponse.diagnosisKeySubmissionToken,
                    testResultResponse.testEndDate,
                    testResultResponse.testResult
                ),
                showNotification = false
            )
        )
        linkTestResultLiveData.postValue(Valid)
    }

    companion object {
        private const val CROCKFORD_BASE32_REGEX = "[^0123456789abcdefghjkmnpqrstvwxyz]"
    }

    sealed class LinkTestResultViewState {
        object Progress : LinkTestResultViewState()
        object Valid : LinkTestResultViewState()
        data class Error(val type: LinkTestResultErrorType) : LinkTestResultViewState()
    }

    enum class LinkTestResultErrorType {
        INVALID, NO_CONNECTION, UNEXPECTED
    }
}
