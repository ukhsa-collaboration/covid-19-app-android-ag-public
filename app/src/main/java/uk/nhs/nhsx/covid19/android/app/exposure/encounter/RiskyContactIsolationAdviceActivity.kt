package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityRiskyContactIsolationAdviceBinding
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.BookLfdTest
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.AlreadyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NewlyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsFullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMinor
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.widgets.IconTextView
import java.time.LocalDate
import javax.inject.Inject

class RiskyContactIsolationAdviceActivity : BaseActivity() {

    @Inject
    lateinit var factory: RiskyContactIsolationAdviceViewModel.Factory

    private val viewModel: RiskyContactIsolationAdviceViewModel by assistedViewModel {
        val optOutOfContactIsolationExtra =
            intent.getSerializableExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA) as? OptOutOfContactIsolationExtra ?: NONE
        factory.create(optOutOfContactIsolationExtra)
    }

    private lateinit var binding: ActivityRiskyContactIsolationAdviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityRiskyContactIsolationAdviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()

        startListeningToViewState()
    }

    private fun configureToolbar() =
        setCloseToolbar(
            binding.primaryToolbar.toolbar,
            titleResId = R.string.empty,
            closeIndicator = R.drawable.ic_close_primary
        ) {
            navigateToStatusActivity()
        }

    private fun startListeningToViewState() {
        viewModel.viewState().observe(this) {
            renderViewState(it)
        }

        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                BookPcrTest -> bookPcrTest()
                Home -> navigateToStatusActivity()
                is BookLfdTest -> bookLfdTest(navigationTarget.url)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val orderedPcrTest =
            requestCode == TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST && resultCode == Activity.RESULT_OK
        val orderedLfdTest = requestCode == REQUEST_ORDER_LFD
        val readGuidanceForContacts = requestCode == REQUEST_READ_GUIDANCE
        if (orderedPcrTest || orderedLfdTest || readGuidanceForContacts) {
            navigateToStatusActivity()
        }
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
    }

    private fun bookPcrTest() {
        startActivityForResult(
            TestOrderingActivity.getIntent(this),
            TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST
        )
    }

    private fun bookLfdTest(@StringRes url: Int) {
        openInExternalBrowserForResult(getString(url), REQUEST_ORDER_LFD)
    }

    private fun renderViewState(viewState: ViewState) {
        when (viewState) {
            is NewlyIsolating -> {
                if (viewState.country == ENGLAND)
                    handleNewlyIsolatingForEngland(viewState.remainingDaysInIsolation)
                else
                    handleNewlyIsolatingForWales(
                        viewState.remainingDaysInIsolation,
                        viewState.testingAdviceToShow
                    )
            }
            is AlreadyIsolating -> handleAlreadyIsolating(
                viewState.remainingDaysInIsolation,
                viewState.testingAdviceToShow
            )
            is NotIsolatingAsMinor -> {
                if (viewState.country == ENGLAND)
                    handleNotIsolatingAsMinorForEngland()
                else
                    handleNotIsolatingAsMinorForWales(viewState.testingAdviceToShow)
            }
            is NotIsolatingAsFullyVaccinated -> {
                if (viewState.country == ENGLAND)
                    handleNotIsolatingAsFullyVaccinatedForEngland()
                else
                    handleNotIsolatingAsFullyVaccinatedForWales(viewState.testingAdviceToShow)
            }
            NotIsolatingAsMedicallyExempt -> handleNotIsolatingAsMedicallyExemptForEngland()
        }
    }

    private fun handleNewlyIsolatingForEngland(days: Int) =
        with(binding) {
            riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
            riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_self_isolate_for)
            riskyContactIsolationAdviceRemainingDaysInIsolation.text =
                resources.getQuantityString(R.plurals.state_isolation_days, days, days)
            riskyContactIsolationAdviceStateInfoView.stateText =
                getString(R.string.risky_contact_isolation_advice_new_isolation_information)

            adviceContainer.removeAllViews()
            addAdvice(
                R.string.risky_contact_isolation_advice_new_isolation_testing_advice,
                R.drawable.ic_get_free_test
            )
            addAdvice(
                R.string.risky_contact_isolation_advice_new_isolation_stay_at_home_advice,
                R.drawable.ic_stay_at_home
            )

            riskyContactIsolationAdviceCommonQuestions.gone()
            furtherAdviceTextView.visible()
            nhsGuidanceLinkTextView.visible()

            primaryActionButton.setText(R.string.risky_contact_isolation_advice_book_pcr_test)
            primaryActionButton.setOnSingleClickListener {
                viewModel.onBookPcrTestClicked()
            }

            secondaryActionButton.setOnSingleClickListener {
                viewModel.onBackToHomeClicked()
            }
            secondaryActionButton.visible()
            setAccessibilityTitle(isIsolating = true)
        }

    private fun handleNewlyIsolatingForWales(days: Int, testingAdviceToShow: TestingAdviceToShow) =
        with(binding) {
            riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
            riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_self_isolate_for)
            riskyContactIsolationAdviceRemainingDaysInIsolation.text =
                resources.getQuantityString(R.plurals.state_isolation_days, days, days)
            riskyContactIsolationAdviceStateInfoView.stateText =
                getString(R.string.risky_contact_isolation_advice_new_isolation_information)

            adviceContainer.removeAllViews()
            if (testingAdviceToShow == Default) {
                addAdvice(
                    R.string.contact_case_start_isolation_list_item_testing_once_asap_wales,
                    R.drawable.ic_get_free_test
                )
            } else if (testingAdviceToShow is WalesWithinAdviceWindow) {
                addTestingAdviceWithDate(
                    R.string.contact_case_start_isolation_list_item_testing_with_date,
                    testingAdviceToShow.date
                )
            }
            addAdvice(
                R.string.risky_contact_isolation_advice_new_isolation_stay_at_home_advice,
                R.drawable.ic_stay_at_home
            )

            riskyContactIsolationAdviceCommonQuestions.gone()
            furtherAdviceTextView.visible()
            nhsGuidanceLinkTextView.visible()

            primaryActionButton.setText(R.string.contact_case_start_isolation_primary_button_title_wales)
            primaryActionButton.setOnSingleClickListener {
                viewModel.onBookLfdTestClicked()
            }

            secondaryActionButton.setOnSingleClickListener {
                viewModel.onBackToHomeClicked()
            }
            secondaryActionButton.visible()
            setAccessibilityTitle(isIsolating = true)
        }

    private fun handleAlreadyIsolating(days: Int, testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        adviceContainer.removeAllViews()
        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            riskyContactIsolationAdviceTitle.setText(R.string.contact_case_continue_isolation_title_wls)
            riskyContactIsolationAdviceRemainingDaysInIsolation.text =
                resources.getQuantityString(R.plurals.state_isolation_days, days, days)

            riskyContactIsolationAdviceStateInfoView.stateText =
                getString(R.string.contact_case_continue_isolation_info_box_wls)

            addAdvice(R.string.contact_case_continue_isolation_list_item_isolation_wls, R.drawable.ic_stay_at_home)

            furtherAdviceTextView.setText(R.string.contact_case_continue_isolation_advice_wls)
            nhsGuidanceLinkTextView.setText(R.string.contact_case_continue_isolation_link_title_wls)
            primaryActionButton.setText(R.string.contact_case_continue_isolation_primary_button_title_wls)
        } else {
            riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_continue_isolataion_for)
            riskyContactIsolationAdviceRemainingDaysInIsolation.text =
                resources.getQuantityString(R.plurals.state_isolation_days, days, days)
            riskyContactIsolationAdviceStateInfoView.stateText =
                getString(R.string.risky_contact_isolation_advice_already_isolating_information)

            addAdvice(
                R.string.risky_contact_isolation_advice_already_isolating_stay_at_home_advice,
                R.drawable.ic_stay_at_home
            )
            furtherAdviceTextView.setText(R.string.risky_contact_isolation_advice_further_nhs_guidance)
            nhsGuidanceLinkTextView.setText(R.string.risky_contact_isolation_advice_nhs_guidance_link_text)
            primaryActionButton.setText(R.string.risky_contact_isolation_advice_already_isolating_acknowledge_button_text)
        }

        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
        riskyContactIsolationAdviceCommonQuestions.gone()
        furtherAdviceTextView.visible()
        nhsGuidanceLinkTextView.visible()

        primaryActionButton.setOnSingleClickListener {
            viewModel.onBackToHomeClicked()
        }
        secondaryActionButton.gone()
        setAccessibilityTitle(isIsolating = true)
    }

    private fun addTestingAdviceWithDate(@StringRes stringResId: Int, testAdviceDate: LocalDate) {
        val formattedDate = testAdviceDate.uiLongFormat(this)
        addAdvice(getString(stringResId, formattedDate), R.drawable.ic_get_free_test)
    }

    private fun handleNotIsolatingAsFullyVaccinatedForEngland() = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_already_vaccinated_information)

        adviceContainer.removeAllViews()
        addAdvice(
            R.string.contact_case_no_isolation_fully_vaccinated_list_item_social_distancing_england,
            R.drawable.ic_social_distancing
        )
        addAdvice(
            R.string.contact_case_no_isolation_fully_vaccinated_list_item_get_tested_before_meeting_vulnerable_people_england,
            R.drawable.ic_get_free_test
        )
        addAdvice(
            R.string.contact_case_no_isolation_fully_vaccinated_list_item_wear_a_mask_england,
            R.drawable.ic_mask
        )
        addAdvice(
            R.string.contact_case_no_isolation_fully_vaccinated_list_item_work_from_home_england,
            R.drawable.ic_work_from_home
        )

        riskyContactIsolationAdviceCommonQuestions.gone()
        furtherAdviceTextView.gone()
        nhsGuidanceLinkTextView.gone()

        setupActionButtonsForNotIsolating(
            country = ENGLAND,
            primaryButtonTitle = R.string.contact_case_no_isolation_fully_vaccinated_primary_button_title_read_guidance_england
        )
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsFullyVaccinatedForWales(testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required_wls)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_already_vaccinated_information_wls)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_already_vaccinated_vaccine_research_wls, R.drawable.ic_info)
        addAdvice(
            R.string.risky_contact_isolation_advice_already_vaccinated_testing_advice_wls,
            R.drawable.ic_social_distancing
        )
        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            addTestingAdviceWithDate(
                R.string.contact_case_no_isolation_fully_vaccinated_list_item_testing_with_date,
                testingAdviceToShow.date
            )
        }

        riskyContactIsolationAdviceCommonQuestions.visible()
        furtherAdviceTextView.visible()
        nhsGuidanceLinkTextView.visible()

        setupActionButtonsForNotIsolating(
            country = WALES,
            primaryButtonTitle = R.string.risky_contact_isolation_advice_book_pcr_test
        )
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMinorForEngland() = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_minors_no_self_isolation_required)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_minors_information)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_minors_show_to_adult_advice, R.drawable.ic_family)
        addAdvice(
            R.string.contact_case_no_isolation_under_age_limit_list_item_social_distancing_england,
            R.drawable.ic_social_distancing
        )
        addAdvice(
            R.string.contact_case_no_isolation_under_age_limit_list_item_get_tested_before_meeting_vulnerable_people_england,
            R.drawable.ic_get_free_test
        )
        addAdvice(R.string.contact_case_no_isolation_under_age_limit_list_item_wear_a_mask_england, R.drawable.ic_mask)

        riskyContactIsolationAdviceCommonQuestions.gone()
        furtherAdviceTextView.gone()
        nhsGuidanceLinkTextView.gone()

        setupActionButtonsForNotIsolating(
            country = ENGLAND,
            primaryButtonTitle = R.string.contact_case_no_isolation_under_age_limit_primary_button_title_read_guidance_england
        )
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMinorForWales(testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_minors_no_self_isolation_required_wls)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_minors_information_wls)

        adviceContainer.removeAllViews()
        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            addTestingAdviceWithDate(
                R.string.contact_case_no_isolation_under_age_limit_list_item_testing_with_date,
                testingAdviceToShow.date
            )
        }
        addAdvice(R.string.risky_contact_isolation_advice_minors_testing_advice_wls, R.drawable.ic_social_distancing)
        addAdvice(R.string.risky_contact_isolation_advice_minors_show_to_adult_advice_wls, R.drawable.ic_family)

        riskyContactIsolationAdviceCommonQuestions.visible()
        furtherAdviceTextView.visible()
        nhsGuidanceLinkTextView.visible()

        setupActionButtonsForNotIsolating(
            country = WALES,
            primaryButtonTitle = R.string.risky_contact_isolation_advice_book_pcr_test
        )
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMedicallyExemptForEngland() = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_medically_exempt_heading)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_medically_exempt_information)

        adviceContainer.removeAllViews()
        addAdvice(
            R.string.risky_contact_isolation_advice_medically_exempt_social_distancing_england,
            R.drawable.ic_social_distancing
        )
        addAdvice(
            R.string.risky_contact_isolation_advice_medically_exempt_get_tested_before_meeting_vulnerable_people_england,
            R.drawable.ic_get_free_test
        )
        addAdvice(
            R.string.risky_contact_isolation_advice_medically_exempt_wear_a_mask_england,
            R.drawable.ic_mask
        )
        addAdvice(
            R.string.risky_contact_isolation_advice_medically_exempt_work_from_home_england,
            R.drawable.ic_work_from_home
        )

        riskyContactIsolationAdviceCommonQuestions.gone()
        furtherAdviceTextView.gone()
        nhsGuidanceLinkTextView.gone()

        setupActionButtonsForNotIsolating(
            country = ENGLAND,
            primaryButtonTitle = R.string.risky_contact_isolation_advice_medically_exempt_primary_button_title_read_guidance_england
        )
        setAccessibilityTitle(isIsolating = false)
    }

    private fun setupActionButtonsForNotIsolating(country: SupportedCountry, @StringRes primaryButtonTitle: Int) =
        with(binding) {
            when (country) {
                ENGLAND -> {
                    riskyContactIsolationAdviceCommonQuestions.gone()
                    furtherAdviceTextView.gone()
                    nhsGuidanceLinkTextView.gone()

                    showPrimaryButtonIcon(showIcon = true)
                    primaryActionButton.setText(primaryButtonTitle)
                    primaryActionButton.setOnSingleClickListener {
                        openInExternalBrowserForResult(
                            getString(R.string.contact_case_guidance_for_contacts_in_england_url),
                            REQUEST_READ_GUIDANCE
                        )
                    }
                    primaryActionButton.setUpOpensInBrowserWarning()
                }
                else -> {
                    riskyContactIsolationAdviceCommonQuestions.visible()
                    furtherAdviceTextView.visible()
                    nhsGuidanceLinkTextView.visible()

                    showPrimaryButtonIcon(showIcon = false)
                    primaryActionButton.setText(primaryButtonTitle)
                    primaryActionButton.setOnSingleClickListener {
                        viewModel.onBookPcrTestClicked()
                    }
                }
            }
            secondaryActionButton.setOnSingleClickListener {
                viewModel.onBackToHomeClicked()
            }
            secondaryActionButton.visible()
        }

    private fun addAdvice(@StringRes stringResId: Int, @DrawableRes drawableResId: Int) =
        binding.adviceContainer.addView(
            IconTextView(
                context = this,
                stringResId = stringResId,
                drawableResId = drawableResId
            )
        )

    private fun addAdvice(text: String, @DrawableRes drawableResId: Int) =
        binding.adviceContainer.addView(IconTextView(context = this, _text = text, _drawableResId = drawableResId))

    private fun setAccessibilityTitle(isIsolating: Boolean) = with(binding) {
        title = if (isIsolating) {
            "${riskyContactIsolationAdviceTitle.text} ${riskyContactIsolationAdviceRemainingDaysInIsolation.text}"
        } else {
            riskyContactIsolationAdviceTitle.text
        }
        riskyContactIsolationAdviceRemainingDaysInIsolationContainer.setUpAccessibilityHeading()
    }

    private fun showPrimaryButtonIcon(showIcon: Boolean) {
        if (showIcon) {
            binding.primaryActionButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_link)
            binding.primaryActionButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
            binding.primaryActionButton.iconPadding = 8.dpToPx.toInt()
        } else {
            binding.primaryActionButton.icon = null
        }
    }

    companion object {
        fun start(context: Context) {
            start(context, NONE)
        }

        fun startAsMinor(context: Context) {
            start(context, MINOR)
        }

        fun startAsFullyVaccinated(context: Context) {
            start(context, FULLY_VACCINATED)
        }

        fun startAsMedicallyExempt(context: Context) {
            start(context, MEDICALLY_EXEMPT)
        }

        private fun start(context: Context, optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra) {
            context.startActivity(getIntentAsNewClearedTask(context, optOutOfContactIsolationExtra))
        }

        private fun getIntentAsNewClearedTask(
            context: Context,
            optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra
        ) = getIntent(
            context,
            optOutOfContactIsolationExtra,
            intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )

        private fun getIntent(
            context: Context,
            optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra,
            intentFlags: Int
        ) = Intent(context, RiskyContactIsolationAdviceActivity::class.java).apply {
            this.flags = intentFlags
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, optOutOfContactIsolationExtra)
        }

        const val OPT_OUT_OF_CONTACT_ISOLATION_EXTRA = "OPT_OUT_OF_CONTACT_ISOLATION_EXTRA"
        const val REQUEST_ORDER_LFD = 1002
        const val REQUEST_READ_GUIDANCE = 1003
    }

    enum class OptOutOfContactIsolationExtra {
        NONE, MINOR, FULLY_VACCINATED, MEDICALLY_EXEMPT
    }
}
