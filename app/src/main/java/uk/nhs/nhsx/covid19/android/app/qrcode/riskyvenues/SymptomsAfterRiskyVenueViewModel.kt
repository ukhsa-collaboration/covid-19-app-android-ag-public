package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasNoSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.OrderLfdTest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Questionnaire
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class SymptomsAfterRiskyVenueViewModel @AssistedInject constructor(
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    @Assisted val shouldShowCancelConfirmationDialogOnCancelButtonClick: Boolean
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    fun onHasSymptomsClicked() {
        analyticsEventProcessor.track(SelectedHasSymptomsM2Journey)
        navigationTarget.postValue(Questionnaire)
    }

    fun onHasNoSymptomsClicked() {
        analyticsEventProcessor.track(SelectedHasNoSymptomsM2Journey)
        navigationTarget.postValue(OrderLfdTest)
    }

    fun onCancelButtonClicked() {
        if (shouldShowCancelConfirmationDialogOnCancelButtonClick) {
            viewState.postValue(ViewState(showCancelDialog = true))
        } else {
            navigationTarget.postValue(Finish)
        }
    }

    fun onDialogOptionLeaveClicked() {
        navigationTarget.postValue(Home)
    }

    fun onDialogOptionStayClicked() {
        viewState.postValue(ViewState(showCancelDialog = false))
    }

    @AssistedFactory
    interface Factory {
        fun create(
            shouldShowCancelConfirmationDialogOnCancelButtonClick: Boolean
        ): SymptomsAfterRiskyVenueViewModel
    }
}

data class ViewState(val showCancelDialog: Boolean)

sealed class NavigationTarget {
    object Questionnaire : NavigationTarget()
    object OrderLfdTest : NavigationTarget()
    object Home : NavigationTarget()
    object Finish : NavigationTarget()
}
