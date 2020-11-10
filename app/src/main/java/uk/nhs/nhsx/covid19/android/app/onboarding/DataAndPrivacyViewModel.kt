package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class DataAndPrivacyViewModel @Inject constructor(
    private val provider: PolicyUpdateProvider
) : ViewModel() {

    fun markPolicyAccepted() {
        provider.markPolicyAccepted()
    }
}
