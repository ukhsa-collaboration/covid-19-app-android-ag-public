package uk.nhs.nhsx.covid19.android.app.settings.animations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.util.viewutils.AreSystemLevelAnimationsEnabled
import javax.inject.Inject

class AnimationsViewModel @Inject constructor(
    private val animationsProvider: AnimationsProvider,
    private val areSystemLevelAnimationsEnabled: AreSystemLevelAnimationsEnabled
) : ViewModel() {

    private val viewState = MutableLiveData(
        ViewState(
            animationsEnabled = animationsProvider.inAppAnimationEnabled && areSystemLevelAnimationsEnabled(),
            showDialog = false
        )
    )
    fun viewState(): LiveData<ViewState> = distinctUntilChanged(viewState)

    fun onAnimationToggleClicked() {
        if (areSystemLevelAnimationsEnabled()) {
            animationsProvider.inAppAnimationEnabled = !animationsProvider.inAppAnimationEnabled
        }
        updateViewState(showDialog = !areSystemLevelAnimationsEnabled())
    }

    fun onDialogDismissed() {
        updateViewState(showDialog = false)
    }

    private fun updateViewState(showDialog: Boolean? = null) {
        viewModelScope.launch {
            viewState.postValue(
                ViewState(
                    animationsEnabled = animationsProvider.inAppAnimationEnabled && areSystemLevelAnimationsEnabled(),
                    showDialog = showDialog ?: viewState.value!!.showDialog
                )
            )
        }
    }

    fun onResume() {
        updateViewState()
    }

    data class ViewState(
        val animationsEnabled: Boolean,
        val showDialog: Boolean
    )
}
