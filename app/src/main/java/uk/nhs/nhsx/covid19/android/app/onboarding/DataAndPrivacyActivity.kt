package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_data_and_privacy.buttonAgree
import kotlinx.android.synthetic.main.activity_data_and_privacy.buttonNoThanks
import kotlinx.android.synthetic.main.include_onboarding_toolbar.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class DataAndPrivacyActivity : BaseActivity(R.layout.activity_data_and_privacy) {

    @Inject
    lateinit var factory: ViewModelFactory<DataAndPrivacyViewModel>

    private val viewModel: DataAndPrivacyViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar as MaterialToolbar,
            R.string.empty,
            upIndicator = R.drawable.ic_arrow_back_primary
        )

        buttonAgree.setOnSingleClickListener {
            viewModel.markPolicyAccepted()
            PostCodeActivity.start(this)
        }

        buttonNoThanks.setOnSingleClickListener { finish() }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DataAndPrivacyActivity::class.java)
    }
}
