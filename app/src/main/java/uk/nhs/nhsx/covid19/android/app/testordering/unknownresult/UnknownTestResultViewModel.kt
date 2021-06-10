package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class UnknownTestResultViewModel @Inject constructor(
    private val unknownTestResultProvider: ReceivedUnknownTestResultProvider
) : ViewModel() {

    fun acknowledgeUnknownTestResult() {
        unknownTestResultProvider.value = false
    }
}
