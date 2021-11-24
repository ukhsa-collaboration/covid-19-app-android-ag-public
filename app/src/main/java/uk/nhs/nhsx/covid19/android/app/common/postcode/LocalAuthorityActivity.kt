package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NOT_SELECTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NOT_SUPPORTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityViewModel.ErrorState.NO_ERROR
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityLocalAuthorityBinding
import uk.nhs.nhsx.covid19.android.app.databinding.ItemLocalAuthorityBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setToolbarNoNavigation
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class LocalAuthorityActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<LocalAuthorityViewModel>

    private val viewModel: LocalAuthorityViewModel by viewModels { factory }

    private lateinit var binding: ActivityLocalAuthorityBinding

    private val localAuthoritiesRadioGroupBindings = mutableListOf<ItemLocalAuthorityBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityLocalAuthorityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.primaryToolbar.toolbar

        if (isBackAllowed()) {
            setNavigateUpToolbar(
                toolbar,
                R.string.local_authority_title,
                upIndicator = R.drawable.ic_arrow_back_white
            )
        } else {
            setToolbarNoNavigation(
                toolbar,
                R.string.local_authority_title
            )
        }

        val postCode = intent.getStringExtra(EXTRA_POST_CODE)

        if (!viewModel.initializePostCode(postCode)) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        binding.buttonConfirmLink.setOnSingleClickListener {
            viewModel.confirmLocalAuthority()
        }

        setupViewModelListeners(viewModel.postCode)

        viewModel.loadLocalAuthorities()
    }

    override fun onBackPressed() {
        if (isBackAllowed()) {
            super.onBackPressed()
        }
    }

    private fun setupViewModelListeners(postCode: String) {
        viewModel.localAuthorities().observe(this) { authorities ->
            if (authorities.size == 1) {
                initializeSingleLocalAuthority(postCode, authorities[0])
            } else {
                initializeMultipleLocalAuthorities(postCode, authorities)
            }
        }

        viewModel.viewState().observe(this) { viewState ->
            with(binding) {

            localAuthoritiesRadioGroupBindings.forEach { child ->
                if (child.localAuthority.tag != viewState.localAuthorityId) {
                    child.localAuthority.isChecked = false
                    child.root.background = getDrawable(R.drawable.question_not_selected_background)
                } else {
                    child.root.background = getDrawable(R.drawable.question_selected_background)
                }
            }
            buttonConfirmLink.visible()
            when (viewState.errorState) {
                NO_ERROR -> errorView.gone()
                NOT_SUPPORTED -> handleLocalAuthorityNotSupported()
                NOT_SELECTED -> handleNoLocalAuthoritySelected()
            }
            }
        }

        viewModel.finishActivity().observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun initializeMultipleLocalAuthorities(
        postCode: String,
        authoritiesWithId: List<LocalAuthorityWithId>
    ) = with(binding) {
        titleLocalAuthority.text = getString(R.string.multiple_local_authorities_title)
        descriptionLocalAuthority.text =
            getString(R.string.multiple_local_authorities_description, postCode)
        moreInfoLink.visible()
        localAuthoritiesRadioGroupBindings.clear()
        localAuthoritiesRadioGroup.removeAllViews()
        authoritiesWithId.forEach {

            val itemBinding = ItemLocalAuthorityBinding.inflate(layoutInflater, localAuthoritiesRadioGroup, false)
            itemBinding.localAuthority.text = it.localAuthority.name
            itemBinding.localAuthority.tag = it.id

            itemBinding.localAuthority.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    viewModel.selectLocalAuthority(buttonView.tag as String)
                }
            }

            localAuthoritiesRadioGroupBindings.add(itemBinding)
            localAuthoritiesRadioGroup.addView(itemBinding.root)
        }
    }

    private fun initializeSingleLocalAuthority(
        postCode: String,
        authorityWithId: LocalAuthorityWithId
    ) = with(binding) {
        titleLocalAuthority.text =
            getString(
                R.string.single_local_authority_title,
                postCode,
                authorityWithId.localAuthority.name
            )
        descriptionLocalAuthority.text = getString(R.string.single_local_authority_description)
        moreInfoLink.gone()
        localAuthoritiesRadioGroup.gone()
    }

    private fun handleNoLocalAuthoritySelected() {
        val title = getString(R.string.local_authority_error_no_authority_selected_title)
        val description =
            getString(R.string.local_authority_error_no_authority_selected_description)
        showErrorView(title, description)
    }

    private fun handleLocalAuthorityNotSupported() {
        val title = getString(R.string.local_authority_error_authority_not_supported_title)
        val description =
            getString(R.string.local_authority_error_authority_not_supported_description)
        showErrorView(title, description)
    }

    private fun showErrorView(title: String, description: String) = with(binding) {
        errorView.errorTitle = title
        errorView.errorDescription = description
        errorView.visible()
        val announcementText = "$title. $description"
        errorView.announceForAccessibility(announcementText)
        errorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun isBackAllowed(): Boolean =
        intent.getBooleanExtra(EXTRA_BACK_ALLOWED, false)

    companion object {
        const val EXTRA_POST_CODE = "EXTRA_POST_CODE"
        const val EXTRA_BACK_ALLOWED = "EXTRA_BACK_ALLOWED"

        fun getIntent(context: Context, postCode: String, backAllowed: Boolean = true) =
            Intent(context, LocalAuthorityActivity::class.java)
                .putExtra(EXTRA_POST_CODE, postCode)
                .putExtra(EXTRA_BACK_ALLOWED, backAllowed)

        fun getIntent(context: Context, backAllowed: Boolean = true) =
            Intent(context, LocalAuthorityActivity::class.java)
                .putExtra(EXTRA_BACK_ALLOWED, backAllowed)
    }
}
