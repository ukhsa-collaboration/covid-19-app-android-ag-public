package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class PolicyUpdateViewModel @Inject constructor(
    private val policyUpdateProvider: PolicyUpdateProvider
) : ViewModel() {

    fun markPolicyAccepted() {
        policyUpdateProvider.markPolicyAccepted()
    }
}
