package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class NewFunctionalityLabelProvider @Inject constructor(sharedPreferences: SharedPreferences) {
    private val reportSymptomsNewLabelPrefs = sharedPreferences.with<Boolean>(VALUE_KEY_REPORT_SYMPTOMS)

    private var _hasSeenReportSymptomsNewLabel: Boolean? by reportSymptomsNewLabelPrefs

    var hasSeenReportSymptomsNewLabel: Boolean
    get() = _hasSeenReportSymptomsNewLabel ?: false
        set(value) {
            _hasSeenReportSymptomsNewLabel = value
        }

    companion object {
        @Deprecated("Only used in version 4.29")
        private const val VALUE_KEY_REPORT_SYMPTOMS = "REPORT_SYMPTOMS_NEW_LABEL_ENABLED"
    }
}
