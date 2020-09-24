package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class AreaRiskLevelProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    private var value: String? by prefs

    fun toRiskLevel(): RiskLevel? = when (value) {
        "H" -> HIGH
        "M" -> MEDIUM
        "L" -> LOW
        else -> null
    }

    fun setRiskyPostCodeLevel(riskLevel: RiskLevel?) {
        value = riskLevel?.let {
            when (it) {
                HIGH -> "H"
                MEDIUM -> "M"
                LOW -> "L"
            }
        }
    }

    companion object {
        const val VALUE_KEY = "RISKY_POST_CODE_LEVEL_KEY"
    }
}
