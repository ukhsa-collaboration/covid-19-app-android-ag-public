package uk.nhs.nhsx.covid19.android.app.questionnaire

import android.content.Context
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityNewGuidanceForSymptomaticCasesEnglandBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning

class NewGuidanceForSymptomaticCaseEnglandActivity : BaseActivity() {

    private lateinit var binding: ActivityNewGuidanceForSymptomaticCasesEnglandBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityNewGuidanceForSymptomaticCasesEnglandBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setClickListeners()
        configureToolbar()
        setAccessibilityText()
    }

    private fun setAccessibilityText() {
        binding.exposureFaqsLinkTextView.setUpOpensInBrowserWarning()
        binding.onlineServiceLinkTextView.setUpOpensInBrowserWarning()
    }

    private fun setClickListeners() {
        binding.primaryActionButton.setOnSingleClickListener {
            StatusActivity.start(this)
        }

        binding.exposureFaqsLinkTextView.setOnSingleClickListener {
            openInExternalBrowserForResult(getString(R.string.url_exposure_faqs), REQUEST_ADVICE_EXTERNAL_LINK)
        }

        binding.onlineServiceLinkTextView.setOnSingleClickListener {
            openInExternalBrowserForResult(getString(R.string.url_nhs_111_online), REQUEST_ADVICE_EXTERNAL_LINK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADVICE_EXTERNAL_LINK) {
            StatusActivity.start(this)
        }
    }

    private fun configureToolbar() =
        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            titleResId = R.string.empty,
            upIndicator = R.drawable.ic_arrow_back_primary
        )

    companion object {
        fun getIntent(context: Context) = Intent(context, NewGuidanceForSymptomaticCaseEnglandActivity::class.java)

        private const val REQUEST_ADVICE_EXTERNAL_LINK = 1125
    }
}
