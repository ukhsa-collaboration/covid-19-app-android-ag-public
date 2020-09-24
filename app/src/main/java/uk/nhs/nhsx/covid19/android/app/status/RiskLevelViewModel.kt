package uk.nhs.nhsx.covid19.android.app.status

import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import javax.inject.Inject

class RiskLevelViewModel @Inject constructor(
    private val districtAreaStringProvider: DistrictAreaStringProvider
) : ViewModel() {

    fun getDistrictAwareRiskLevelInformation(riskLevel: RiskLevel): Int =
        when (riskLevel) {
            LOW -> districtAreaStringProvider.provide(R.string.low_risk_level_text)
            MEDIUM -> districtAreaStringProvider.provide(R.string.medium_risk_level_text)
            HIGH -> districtAreaStringProvider.provide(R.string.high_risk_level_text)
        }
}
