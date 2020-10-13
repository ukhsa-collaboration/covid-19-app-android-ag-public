package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_risk_level.*
import kotlinx.android.synthetic.main.view_toolbar_primary.*
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.OldRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
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
                is Risk -> handleNewRiskLevel(it)
                is OldRisk -> handleOldRiskLevel(it)
                Unknown -> finish()
            }
        }

        viewModel.buttonUrlLiveData().observe(this) { urlResource: Int ->
            openUrl(getString(urlResource))
        }

        buttonRiskLevelLink.setOnClickListener {
            viewModel.onRestrictionsButtonClicked()
        }

        buttonRiskLevelLink.setUpOpensInBrowserWarning()
    }

    private fun handleNewRiskLevel(risk: Risk) {
        titleRiskLevel.text = risk.riskIndicator.name.translate()

        when (risk.riskIndicator.colorScheme) {
            NEUTRAL -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_neutral)
            GREEN -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_green)
            YELLOW -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_yellow)
            AMBER -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_amber)
            RED -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_red)
        }

        subtitleRiskLevel.visible()
        subtitleRiskLevel.text = risk.riskIndicator.heading.translate()

        riskLevelInformation.setRawText(risk.riskIndicator.content.translate())

        buttonRiskLevelLink.text = risk.riskIndicator.linkTitle.translate()

        buttonRiskLevelLink.setOnClickListener {
            openUrl(risk.riskIndicator.linkUrl.translate())
        }
    }

    private fun handleOldRiskLevel(oldRisk: OldRisk) {
        titleRiskLevel.text = getString(oldRisk.textResId, oldRisk.mainPostCode, getString(oldRisk.areaRiskLevelResId))

        when (oldRisk.areaRisk) {
            LOW -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_green)
            MEDIUM -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_yellow)
            HIGH -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_red)
        }

        subtitleRiskLevel.gone()

        val riskLevelText =
            getString(viewModel.getDistrictAwareRiskLevelInformation(oldRisk.areaRisk))
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
