package uk.nhs.nhsx.covid19.android.app.common.postcode

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.about.UpdateAreaInfo
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NO_ERROR
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.status.RiskyPostCodeIndicatorProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class LocalAuthorityViewModel @Inject constructor(
    private val localAuthorityPostCodeValidator: LocalAuthorityPostCodeValidator,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val postCodeProvider: PostCodeProvider,
    private val riskyPostCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val updateAreaInfo: UpdateAreaInfo
) : ViewModel() {

    @VisibleForTesting
    internal var localAuthorities = MutableLiveData<List<LocalAuthorityWithId>>()
    fun localAuthorities(): LiveData<List<LocalAuthorityWithId>> = localAuthorities

    private var viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private var finishActivity = SingleLiveEvent<Void>()
    fun finishActivity(): LiveData<Void> = finishActivity

    lateinit var postCode: String

    var selectedLocalAuthorityId: String? = null

    fun initializePostCode(postCode: String?): Boolean {
        postCode?.let {
            this.postCode = it
            return true
        }

        postCodeProvider.value?.let {
            this.postCode = it
            return true
        }

        return false
    }

    fun loadLocalAuthorities() {
        viewModelScope.launch {
            val result = localAuthorityPostCodeValidator.validate(postCode)
            if (result is Valid) {
                if (result.localAuthorities.size == 1) {
                    selectedLocalAuthorityId = result.localAuthorities[0].id
                }
                val sortedLocalAuthorities = result.localAuthorities.sortedBy { it.localAuthority.name.toLowerCase() }
                localAuthorities.postValue(sortedLocalAuthorities)
            }
            viewState.postValue(ViewState(localAuthorityId = selectedLocalAuthorityId, errorState = NO_ERROR))
        }
    }

    fun selectLocalAuthority(localAuthorityId: String) {
        selectedLocalAuthorityId = localAuthorityId
        viewState.postValue(validate())
    }

    fun confirmLocalAuthority() {
        val nextViewState = validate()
        viewState.postValue(nextViewState)

        if (nextViewState.errorState == NO_ERROR) {
            postCodeProvider.value = postCode
            localAuthorityProvider.value = selectedLocalAuthorityId
            riskyPostCodeIndicatorProvider.clear()

            if (onboardingCompletedProvider.value == true) {
                updateAreaInfo.schedule()
            }
            finishActivity.postCall()
        }
    }

    fun validate(): ViewState {
        val selectedLocalAuthorityWithId =
            localAuthorities.value?.firstOrNull { it.id == selectedLocalAuthorityId }
        val errorState = when {
            selectedLocalAuthorityWithId == null -> ErrorState.NOT_SELECTED
            selectedLocalAuthorityWithId.localAuthority.supported() -> NO_ERROR
            else -> ErrorState.NOT_SUPPORTED
        }

        return ViewState(
            selectedLocalAuthorityId,
            errorState
        )
    }

    data class ViewState(
        val localAuthorityId: String?,
        val errorState: ErrorState
    )

    enum class ErrorState {
        NO_ERROR,
        NOT_SELECTED,
        NOT_SUPPORTED
    }
}
