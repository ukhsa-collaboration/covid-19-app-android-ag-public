package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class NewFunctionalityLabelProvider @Inject constructor(sharedPreferences: SharedPreferences) {
    private val reportSymptomsNewLabelPrefs = sharedPreferences.with<Boolean>(VALUE_KEY_REPORT_SYMPTOMS)
    private val guidanceHubEnglandLongCovidNewLabelPrefs = sharedPreferences.with<Boolean>(VALUE_KEY_GUIDANCE_HUB_ENGLAND_LONG_COVID)
    private val guidanceHubWalesLongCovidNewLabelPrefs = sharedPreferences.with<Boolean>(VALUE_KEY_GUIDANCE_HUB_WALES_LONG_COVID)

    private var _hasSeenReportSymptomsNewLabel: Boolean? by reportSymptomsNewLabelPrefs
    private var _hasInteractedWithLongCovidEnglandNewLabel: Boolean? by guidanceHubEnglandLongCovidNewLabelPrefs
    private var _hasInteractedWithLongCovidWalesNewLabel: Boolean? by guidanceHubWalesLongCovidNewLabelPrefs

    var hasSeenReportSymptomsNewLabel: Boolean
        get() = _hasSeenReportSymptomsNewLabel ?: false
        set(value) {
            _hasSeenReportSymptomsNewLabel = value
        }

    var hasInteractedWithLongCovidEnglandNewLabel: Boolean
        get() = _hasInteractedWithLongCovidEnglandNewLabel ?: false
        set(value) {
            _hasInteractedWithLongCovidEnglandNewLabel = value
        }

    var hasInteractedWithLongCovidWalesNewLabel: Boolean
        get() = _hasInteractedWithLongCovidWalesNewLabel ?: false
        set(value) {
            _hasInteractedWithLongCovidWalesNewLabel = value
        }

    companion object {
        @Deprecated("Only used in version 4.29")
        private const val VALUE_KEY_REPORT_SYMPTOMS = "REPORT_SYMPTOMS_NEW_LABEL_ENABLED"
        @Deprecated("Only used in versions 4.34 to 4.37")
        private const val VALUE_KEY_GUIDANCE_HUB_ENGLAND_LONG_COVID = "GUIDANCE_HUB_ENGLAND_LONG_COVID_NEW_LABEL_ENABLED"
        @Deprecated("Only used in versions 4.34 to 4.37")
        private const val VALUE_KEY_GUIDANCE_HUB_WALES_LONG_COVID = "GUIDANCE_HUB_WALES_LONG_COVID_NEW_LABEL_ENABLED"
    }
}
