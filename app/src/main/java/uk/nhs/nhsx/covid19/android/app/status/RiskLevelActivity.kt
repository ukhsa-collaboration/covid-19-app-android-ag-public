package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_risk_level.imageRiskLevel
import kotlinx.android.synthetic.main.activity_risk_level.riskLevelInformation
import kotlinx.android.synthetic.main.activity_risk_level.titleRiskLevel
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.widgets.setRawText
import javax.inject.Inject

class RiskLevelActivity : BaseActivity(R.layout.activity_risk_level) {

    @Inject
    lateinit var factory: ViewModelFactory<RiskLevelViewModel>

    private val viewModel: RiskLevelViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.risk_level_title, R.drawable.ic_close_white)

        val riskyPostCodeViewState =
            intent.getParcelableExtra<RiskyPostCodeViewState>(EXTRA_RISK_LEVEL)

        riskyPostCodeViewState?.let {
            when (it) {
                is Risk -> handleRisk(it)
                Unknown -> finish()
            }
        }
    }

    private fun handleRisk(risk: Risk) {
        when (risk.areaRisk) {
            LOW -> imageRiskLevel.setImageResource(R.drawable.ic_low_risk_map)
            MEDIUM -> imageRiskLevel.setImageResource(R.drawable.ic_medium_risk_map)
            HIGH -> imageRiskLevel.setImageResource(R.drawable.ic_high_risk_map)
        }
        titleRiskLevel.text =
            getString(risk.textResId, risk.mainPostCode, getString(risk.areaRiskLevelResId))
        val riskLevelText =
            getString(viewModel.getDistrictAwareRiskLevelInformation(risk.areaRisk))
        riskLevelInformation.setRawText(riskLevelText)
    }

    companion object {
        const val EXTRA_RISK_LEVEL = "EXTRA_RISK_LEVEL"

        fun start(context: Context, riskyPostCodeViewState: RiskyPostCodeViewState) {
            context.startActivity(
                Intent(context, RiskLevelActivity::class.java)
                    .putExtra(EXTRA_RISK_LEVEL, riskyPostCodeViewState)
            )
        }
    }
}
