package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class ReceivedUnknownTestResultProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<Boolean>(RECEIVED_UNKNOWN_TEST_RESULT)

    var value: Boolean? by prefs

    companion object {
        const val RECEIVED_UNKNOWN_TEST_RESULT = "RECEIVED_UNKNOWN_TEST_RESULT"
    }
}
