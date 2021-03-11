package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_edit_postal_district.continuePostCode
import kotlinx.android.synthetic.main.activity_edit_postal_district.postCodeView
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.ParseJsonError
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
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

        continuePostCode.setOnSingleClickListener {
            viewModel.validatePostCode(postCodeView.postCodeDistrict)
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

        continuePostCode.text = getString(R.string.continue_button)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCAL_AUTHORITY_REQUEST && resultCode == RESULT_OK) {
            finish()
        }
    }

    companion object {
        private const val LOCAL_AUTHORITY_REQUEST = 1339

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) = Intent(context, EditPostalDistrictActivity::class.java)
    }
}
