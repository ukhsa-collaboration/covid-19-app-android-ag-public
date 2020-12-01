package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.google.android.material.appbar.MaterialToolbar
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_AUTHORITY
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.synthetic.main.activity_edit_postal_district.continuePostCode
import kotlinx.android.synthetic.main.activity_edit_postal_district.postCodeView
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.ParseJsonError
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.InvalidPostDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.PostDistrictNotSupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.Success
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import javax.inject.Inject

class EditPostalDistrictActivity : BaseActivity(R.layout.activity_edit_postal_district) {

    @Inject
    lateinit var factory: ViewModelFactory<EditPostalDistrictViewModel>

    private val viewModel: EditPostalDistrictViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar as MaterialToolbar,
            R.string.edit_postcode_district_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        continuePostCode.setOnClickListener {
            if (RuntimeBehavior.isFeatureEnabled(LOCAL_AUTHORITY)) {
                viewModel.validatePostCode(postCodeView.postCodeDistrict)
            } else {
                viewModel.updatePostCode(postCodeView.postCodeDistrict)
            }
        }

        viewModel.postCodeUpdateState().observe(this) { postCodeViewState ->
            when (postCodeViewState) {
                is Success -> finish()
                InvalidPostDistrict -> postCodeView.showErrorState()
                PostDistrictNotSupported -> postCodeView.showPostCodeNotSupportedErrorState()
            }
        }

        viewModel.postCodeValidationResult().observe(this) { validationResult ->
            when (validationResult) {
                is Valid -> {
                    val intent = LocalAuthorityActivity.getIntent(this, validationResult.postCode)
                    startActivityForResult(intent, LOCAL_AUTHORITY_REQUEST)
                }
                ParseJsonError -> Timber.d("Error parsing localAuthorities.json")
                Invalid -> postCodeView.showErrorState()
                Unsupported -> postCodeView.showPostCodeNotSupportedErrorState()
            }
        }

        setContinueButtonText()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCAL_AUTHORITY_REQUEST && resultCode == RESULT_OK) {
            finish()
        }
    }

    private fun setContinueButtonText() {
        val continueButtonStringResId =
            if (RuntimeBehavior.isFeatureEnabled(LOCAL_AUTHORITY)) string.continue_button else string.save
        continuePostCode.text = getString(continueButtonStringResId)
    }

    companion object {
        private const val LOCAL_AUTHORITY_REQUEST = 1339

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) = Intent(context, EditPostalDistrictActivity::class.java)
    }
}
