package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.drawable
import uk.nhs.nhsx.covid19.android.app.R.string
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
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.AlreadyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NewlyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsFullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMinor
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
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
            titleResId = string.empty,
            closeIndicator = drawable.ic_close_primary
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
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST && resultCode == Activity.RESULT_OK) {
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

    private fun renderViewState(viewState: ViewState) {
        when (viewState) {
            is NewlyIsolating -> handleNewlyIsolating(viewState.remainingDaysInIsolation, viewState.testingAdviceToShow)
            is AlreadyIsolating -> handleAlreadyIsolating(
                viewState.remainingDaysInIsolation,
                viewState.testingAdviceToShow
            )
            is NotIsolatingAsMinor -> handleNotIsolatingAsMinor(viewState.testingAdviceToShow)
            is NotIsolatingAsFullyVaccinated -> handleNotIsolatingAsFullyVaccinated(viewState.testingAdviceToShow)
            NotIsolatingAsMedicallyExempt -> handleNotIsolatingAsMedicallyExempt()
        }
    }

    private fun handleNewlyIsolating(days: Int, testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_self_isolate_for)
        riskyContactIsolationAdviceRemainingDaysInIsolation.text =
            resources.getQuantityString(R.plurals.state_isolation_days, days, days)
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_new_isolation_information)

        adviceContainer.removeAllViews()
        if (testingAdviceToShow == Default) {
            addAdvice(
                R.string.risky_contact_isolation_advice_new_isolation_testing_advice,
                R.drawable.ic_get_free_test
            )
        } else if (testingAdviceToShow is WalesWithinAdviceWindow) {
            addTestingAdviceWithDate(
                R.string.contact_case_start_isolation_list_item_testing_with_date,
                testingAdviceToShow.date
            )
        }
        addAdvice(R.string.risky_contact_isolation_advice_new_isolation_stay_at_home_advice, R.drawable.ic_stay_at_home)

        riskyContactIsolationAdviceCommonQuestions.gone()
        primaryActionButton.setText(R.string.risky_contact_isolation_advice_book_pcr_test)
        primaryActionButton.setOnSingleClickListener {
            viewModel.onBookPcrTestTestClicked()
        }
        secondaryActionButton.setOnSingleClickListener {
            viewModel.onBackToHomeClicked()
        }
        secondaryActionButton.visible()
        setAccessibilityTitle(isIsolating = true)
    }

    private fun handleAlreadyIsolating(days: Int, testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_continue_isolataion_for)
        riskyContactIsolationAdviceRemainingDaysInIsolation.text =
            resources.getQuantityString(R.plurals.state_isolation_days, days, days)
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_already_isolating_information)

        adviceContainer.removeAllViews()
        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            addTestingAdviceWithDate(
                R.string.contact_case_continue_isolation_list_item_testing_with_date,
                testingAdviceToShow.date
            )
        }
        addAdvice(
            R.string.risky_contact_isolation_advice_already_isolating_stay_at_home_advice,
            R.drawable.ic_stay_at_home
        )

        riskyContactIsolationAdviceCommonQuestions.gone()
        primaryActionButton.setText(R.string.risky_contact_isolation_advice_already_isolating_acknowledge_button_text)
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

    private fun handleNotIsolatingAsFullyVaccinated(testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_already_vaccinated_information)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_already_vaccinated_vaccine_research, R.drawable.ic_info)
        addAdvice(
            R.string.risky_contact_isolation_advice_already_vaccinated_testing_advice,
            R.drawable.ic_social_distancing
        )
        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            addTestingAdviceWithDate(
                R.string.contact_case_no_isolation_fully_vaccinated_list_item_testing_with_date,
                testingAdviceToShow.date
            )
        }

        setupActionButtonsForNotIsolating()
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMinor(testingAdviceToShow: TestingAdviceToShow) = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_minors_no_self_isolation_required)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_minors_information)

        adviceContainer.removeAllViews()
        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            addTestingAdviceWithDate(
                R.string.contact_case_no_isolation_under_age_limit_list_item_testing_with_date,
                testingAdviceToShow.date
            )
        }
        addAdvice(R.string.risky_contact_isolation_advice_minors_testing_advice, R.drawable.ic_social_distancing)
        addAdvice(R.string.risky_contact_isolation_advice_minors_show_to_adult_advice, R.drawable.ic_family)

        setupActionButtonsForNotIsolating()
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMedicallyExempt() = with(binding) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_medically_exempt_heading)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_medically_exempt_information)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_medically_exempt_research, R.drawable.ic_info)
        addAdvice(R.string.risky_contact_isolation_advice_medically_exempt_group, R.drawable.ic_group_of_people)
        addAdvice(R.string.risky_contact_isolation_advice_medically_exempt_advice, R.drawable.ic_social_distancing)

        setupActionButtonsForNotIsolating()
        setAccessibilityTitle(isIsolating = false)
    }

    private fun setupActionButtonsForNotIsolating() = with(binding) {
        riskyContactIsolationAdviceCommonQuestions.visible()
        primaryActionButton.setText(R.string.risky_contact_isolation_advice_book_pcr_test)
        primaryActionButton.setOnSingleClickListener {
            viewModel.onBookPcrTestTestClicked()
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
    }

    enum class OptOutOfContactIsolationExtra {
        NONE, MINOR, FULLY_VACCINATED, MEDICALLY_EXEMPT
    }
}
