package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class RiskyPostCodeIndicatorProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var riskyPostCodeIndicator: RiskIndicatorWrapper? by storage(RISKY_POST_CODE_INDICATOR_KEY)

    fun clear() {
        riskyPostCodeIndicator = null
    }

    companion object {
        const val RISKY_POST_CODE_INDICATOR_KEY = "RISKY_POST_CODE_INDICATOR_KEY"
    }
}
