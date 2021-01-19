package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_risk_level.buttonRiskLevelLink
import kotlinx.android.synthetic.main.activity_risk_level.imageRiskLevel
import kotlinx.android.synthetic.main.activity_risk_level.policyItemsContainer
import kotlinx.android.synthetic.main.activity_risk_level.riskLevelFooter
import kotlinx.android.synthetic.main.activity_risk_level.riskLevelInformation
import kotlinx.android.synthetic.main.activity_risk_level.subtitleRiskLevel
import kotlinx.android.synthetic.main.activity_risk_level.titleRiskLevel
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.widgets.PolicyItemView
import uk.nhs.nhsx.covid19.android.app.widgets.setRawText

class RiskLevelActivity : BaseActivity(R.layout.activity_risk_level) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(
            toolbar,
            R.string.risk_level_title
        )

        val riskyPostCodeViewState =
            intent.getParcelableExtra<RiskyPostCodeViewState>(EXTRA_RISK_LEVEL)

        riskyPostCodeViewState?.let {
            when (it) {
                is Risk -> handleRiskLevel(it)
                Unknown -> finish()
            }
        }

        buttonRiskLevelLink.setUpOpensInBrowserWarning()
    }

    private fun handleRiskLevel(risk: Risk) {
        when (risk.riskIndicator.colorScheme) {
            NEUTRAL -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_neutral)
            GREEN -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_green)
            YELLOW -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_yellow)
            AMBER -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_amber)
            RED -> imageRiskLevel.setImageResource(R.drawable.ic_map_risk_red)
        }

        buttonRiskLevelLink.text = risk.riskIndicator.linkTitle.translate()

        buttonRiskLevelLink.setOnSingleClickListener {
            openUrl(risk.riskIndicator.linkUrl.translate())
        }

        val policyData = risk.riskIndicator.policyData

        titleRiskLevel.text = if (risk.riskLevelFromLocalAuthority) {
            risk.riskIndicator.policyData?.localAuthorityRiskTitle?.translate()
                ?: risk.riskIndicator.name.translate()
        } else {
            risk.riskIndicator.name.translate()
        }

        if (policyData == null) {
            riskLevelInformation.setRawText(risk.riskIndicator.content.translate())
            subtitleRiskLevel.text = risk.riskIndicator.heading.translate()
            riskLevelFooter.gone()
            policyItemsContainer.gone()
        } else {
            riskLevelInformation.setRawText(policyData.content.translate())
            riskLevelFooter.setRawText(policyData.footer.translate())
            riskLevelFooter.visible()
            subtitleRiskLevel.text = policyData.heading.translate()
            val topMargin = 16.dpToPx.toInt()
            for ((index, policy) in policyData.policies.withIndex()) {
                val policyItemView = PolicyItemView(this)
                policyItemView.setPolicyItem(policy)
                policyItemsContainer.addView(policyItemView)
                if (index > 0) {
                    val layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(0, topMargin, 0, 0)
                    policyItemView.layoutParams = layoutParams
                }
            }
            policyItemsContainer.isVisible = policyData.policies.isNotEmpty()
        }
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
