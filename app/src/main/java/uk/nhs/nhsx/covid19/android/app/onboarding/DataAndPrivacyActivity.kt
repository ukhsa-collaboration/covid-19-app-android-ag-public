package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityDataAndPrivacyBinding
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class DataAndPrivacyActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<DataAndPrivacyViewModel>

    private val viewModel: DataAndPrivacyViewModel by viewModels { factory }

    private lateinit var binding: ActivityDataAndPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityDataAndPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.empty,
                upIndicator = R.drawable.ic_arrow_back_primary
            )

            buttonAgree.setOnSingleClickListener {
                viewModel.markPolicyAccepted()
                PostCodeActivity.start(this@DataAndPrivacyActivity)
            }

            buttonNoThanks.setOnSingleClickListener {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DataAndPrivacyActivity::class.java)
    }
}
