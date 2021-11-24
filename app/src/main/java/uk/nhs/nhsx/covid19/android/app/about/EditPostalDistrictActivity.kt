package uk.nhs.nhsx.covid19.android.app.about

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.ParseJsonError
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityEditPostalDistrictBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class EditPostalDistrictActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<EditPostalDistrictViewModel>

    private val viewModel: EditPostalDistrictViewModel by viewModels { factory }

    private lateinit var binding: ActivityEditPostalDistrictBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityEditPostalDistrictBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.edit_postcode_district_title,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            continuePostCode.setOnSingleClickListener {
                viewModel.validatePostCode(postCodeView.postCodeDistrict)
            }

            viewModel.postCodeValidationResult().observe(this@EditPostalDistrictActivity) { validationResult ->
                handleValidationResult(validationResult)
            }

            continuePostCode.text = getString(R.string.continue_button)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCAL_AUTHORITY_REQUEST && resultCode == RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun handleValidationResult(
        validationResult: LocalAuthorityPostCodeValidationResult?
    ) = with(binding) {
        when (validationResult) {
            is Valid -> {
                postCodeView.resetErrorState()
                val intent =
                    LocalAuthorityActivity.getIntent(this@EditPostalDistrictActivity, validationResult.postCode)
                startActivityForResult(intent, LOCAL_AUTHORITY_REQUEST)
            }
            ParseJsonError -> Timber.d("Error parsing localAuthorities.json")
            Invalid -> postCodeView.showErrorState()
            Unsupported -> postCodeView.showPostCodeNotSupportedErrorState()
        }
    }

    companion object {
        const val LOCAL_AUTHORITY_REQUEST = 1339

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) = Intent(context, EditPostalDistrictActivity::class.java)
    }
}
