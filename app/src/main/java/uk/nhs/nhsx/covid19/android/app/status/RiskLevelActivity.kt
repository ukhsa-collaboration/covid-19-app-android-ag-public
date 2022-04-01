package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.LocaleProvider
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityRiskLevelBinding
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorSchemeToImageResource
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
import javax.inject.Inject

class RiskLevelActivity : BaseActivity() {

    @Inject
    lateinit var transformer: ColorSchemeToImageResource

    @Inject
    lateinit var localeProvider: LocaleProvider

    private lateinit var binding: ActivityRiskLevelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityRiskLevelBinding.inflate(layoutInflater)

        with(binding) {

            setContentView(root)

            setCloseToolbar(
                primaryToolbar.toolbar,
                R.string.risk_level_title
            )

            val riskyPostCodeViewState = intent.getParcelableExtra<RiskyPostCodeViewState>(EXTRA_RISK_LEVEL)

            riskyPostCodeViewState?.let {
                when (it) {
                    is Risk -> {
                        handleRiskLevel(it)
                    }
                    Unknown -> finish()
                }
            }

            buttonRiskLevelLink.setUpOpensInBrowserWarning()
        }
    }

    private fun handleRiskLevel(risk: Risk) = with(binding) {
        val colorScheme = risk.riskIndicator.colorSchemeV2 ?: risk.riskIndicator.colorScheme
        imageRiskLevel.setImageResource(transformer(colorScheme))

        buttonRiskLevelLink.text = risk.riskIndicator.linkTitle.translate()

        buttonRiskLevelLink.setOnSingleClickListener {
            openUrl(risk.riskIndicator.linkUrl.translate(localeProvider.default()))
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
                val policyItemView = PolicyItemView(this@RiskLevelActivity)
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
