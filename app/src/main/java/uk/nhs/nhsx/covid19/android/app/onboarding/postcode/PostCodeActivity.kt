package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_post_code.postCodeContinue
import kotlinx.android.synthetic.main.activity_post_code.postCodeView
import kotlinx.android.synthetic.main.include_onboarding_toolbar.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.INVALID_POST_DISTRICT
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.POST_DISTRICT_NOT_SUPPORTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.SUCCESS
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class PostCodeActivity : BaseActivity(R.layout.activity_post_code) {

    @Inject
    lateinit var factory: ViewModelFactory<PostCodeViewModel>

    private val viewModel by viewModels<PostCodeViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar as MaterialToolbar, R.string.empty)

        postCodeContinue.setOnClickListener {
            viewModel.updateMainPostCode(postCodeView.postCodeDistrict)
        }

        viewModel.viewState().observe(this) { postCodeUpdateState ->
            when (postCodeUpdateState) {
                SUCCESS -> PermissionActivity.start(this)
                POST_DISTRICT_NOT_SUPPORTED -> postCodeView.showPostCodeNotSupportedErrorState()
                INVALID_POST_DISTRICT -> postCodeView.showErrorState()
            }
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) = Intent(context, PostCodeActivity::class.java)
    }
}
