package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_risk_level.imageRiskLevel
import kotlinx.android.synthetic.main.activity_risk_level.linkRiskLevel
import kotlinx.android.synthetic.main.activity_risk_level.textRiskLevel
import kotlinx.android.synthetic.main.activity_risk_level.titleRiskLevel
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.HighRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.LowRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.MediumRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar

class RiskLevelActivity : BaseActivity(R.layout.activity_risk_level) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setNavigateUpToolbar(toolbar, R.string.risk_level_title, R.drawable.ic_close_white)

        val riskyPostCodeViewState = intent.getParcelableExtra<RiskyPostCodeViewState>(EXTRA_RISK_LEVEL)

        linkRiskLevel.setOnClickListener {
            openUrl(R.string.url_postal_code_risk_more_info)
        }

        riskyPostCodeViewState?.let {
            when (it) {
                is LowRisk -> handleLowRisk(it)
                is MediumRisk -> handleMediumRisk(it)
                is HighRisk -> handleHighRisk(it)
                Unknown -> finish()
            }
        }
    }

    private fun handleHighRisk(highRisk: HighRisk) {
        imageRiskLevel.setImageResource(R.drawable.ic_high_risk_map)
        titleRiskLevel.text = getString(
            R.string.status_area_risk_level,
            highRisk.mainPostCode,
            getString(R.string.status_area_risk_level_high)
        )
        textRiskLevel.text = getString(R.string.high_risk_level_text)
    }

    private fun handleMediumRisk(mediumRisk: MediumRisk) {
        imageRiskLevel.setImageResource(R.drawable.ic_medium_risk_map)
        titleRiskLevel.text = getString(
            R.string.status_area_risk_level,
            mediumRisk.mainPostCode,
            getString(R.string.status_area_risk_level_medium)
        )
        textRiskLevel.text = getString(R.string.medium_risk_level_text)
    }

    private fun handleLowRisk(lowRisk: LowRisk) {
        imageRiskLevel.setImageResource(R.drawable.ic_low_risk_map)
        titleRiskLevel.text = getString(
            R.string.status_area_risk_level,
            lowRisk.mainPostCode,
            getString(R.string.status_area_risk_level_low)
        )
        textRiskLevel.text = getString(R.string.low_risk_level_text)
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
