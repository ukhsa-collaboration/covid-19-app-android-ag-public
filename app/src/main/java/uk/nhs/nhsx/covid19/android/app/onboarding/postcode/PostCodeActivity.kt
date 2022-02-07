package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.ParseJsonError
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityPostCodeBinding
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeViewModel.NavigationTarget.LocalAuthority
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setToolbar
import javax.inject.Inject

class PostCodeActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<PostCodeViewModel>

    private val viewModel by viewModels<PostCodeViewModel> { factory }

    private lateinit var binding: ActivityPostCodeBinding
    private var missingLocalAuthorityMapping: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityPostCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        missingLocalAuthorityMapping = intent.getBooleanExtra(EXTRA_MISSING_LOCAL_AUTHORITY_MAPPING, false)

        with(binding) {

            if (missingLocalAuthorityMapping) {
                setToolbar(
                    primaryToolbar.toolbar,
                    R.string.empty
                )
            } else {
                setNavigateUpToolbar(
                    primaryToolbar.toolbar,
                    R.string.empty,
                    upIndicator = R.drawable.ic_arrow_back_primary
                )
            }

            postCodeContinue.setOnSingleClickListener {
                viewModel.validateMainPostCode(postCodeView.postCodeDistrict)
            }

            viewModel.navigationTarget().observe(this@PostCodeActivity) { navigationTarget ->
                when (navigationTarget) {
                    is LocalAuthority -> {
                        val intent = LocalAuthorityActivity.getIntent(this@PostCodeActivity, navigationTarget.postCode)
                        startActivityForResult(intent, LOCAL_AUTHORITY_REQUEST)
                    }
                    else -> Unit
                }
            }

            viewModel.postCodeValidationError().observe(this@PostCodeActivity) { validationResult ->
                when (validationResult) {
                    ParseJsonError -> Timber.d("Error parsing localAuthorities.json")
                    Invalid -> postCodeView.showErrorState()
                    Unsupported -> postCodeView.showPostCodeNotSupportedErrorState()
                    is Valid -> postCodeView.resetErrorState()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCAL_AUTHORITY_REQUEST && resultCode == RESULT_OK) {
            if (missingLocalAuthorityMapping) {
                MainActivity.start(this)
                finish()
            } else {
                transitionToPermissionsActivity()
            }
        }
    }

    private fun transitionToPermissionsActivity() = startActivity<PermissionActivity>()

    companion object {
        private const val LOCAL_AUTHORITY_REQUEST = 1338
        private const val EXTRA_MISSING_LOCAL_AUTHORITY_MAPPING = "EXTRA_MISSING_LOCAL_AUTHORITY_MAPPING"

        fun start(context: Context, missingLocalAuthorityMapping: Boolean = false) =
            context.startActivity(
                getIntent(context)
                    .putExtra(EXTRA_MISSING_LOCAL_AUTHORITY_MAPPING, missingLocalAuthorityMapping)
            )

        private fun getIntent(context: Context) = Intent(context, PostCodeActivity::class.java)
    }
}
