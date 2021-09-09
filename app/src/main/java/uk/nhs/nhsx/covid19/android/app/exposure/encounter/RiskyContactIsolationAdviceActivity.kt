package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.adviceContainer
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.primaryActionButton
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.riskyContactIsolationAdviceCommonQuestions
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.riskyContactIsolationAdviceIcon
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.riskyContactIsolationAdviceRemainingDaysInIsolation
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.riskyContactIsolationAdviceStateInfoView
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.riskyContactIsolationAdviceTitle
import kotlinx.android.synthetic.main.activity_risky_contact_isolation_advice.secondaryActionButton
import kotlinx.android.synthetic.main.view_toolbar_background.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
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
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.widgets.IconTextView
import javax.inject.Inject

class RiskyContactIsolationAdviceActivity : BaseActivity(R.layout.activity_risky_contact_isolation_advice) {

    @Inject
    lateinit var factory: RiskyContactIsolationAdviceViewModel.Factory

    private val viewModel: RiskyContactIsolationAdviceViewModel by assistedViewModel {
        val optOutOfContactIsolationExtra =
            intent.getSerializableExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA) as? OptOutOfContactIsolationExtra ?: NONE
        factory.create(optOutOfContactIsolationExtra)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(toolbar, R.string.empty, closeIndicator = R.drawable.ic_close_primary) {
            navigateToStatusActivity()
        }

        startListeningToViewState()
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
            is NewlyIsolating -> handleNewlyIsolating(viewState.remainingDaysInIsolation)
            is AlreadyIsolating -> handleAlreadyIsolating(viewState.remainingDaysInIsolation)
            NotIsolatingAsMinor -> handleNotIsolatingAsMinor()
            NotIsolatingAsFullyVaccinated -> handleNotIsolatingAsFullyVaccinated()
            NotIsolatingAsMedicallyExempt -> handleNotIsolatingAsMedicallyExempt()
        }
    }

    private fun handleNewlyIsolating(days: Int) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_self_isolate_for)
        riskyContactIsolationAdviceRemainingDaysInIsolation.text =
            resources.getQuantityString(R.plurals.state_isolation_days, days, days)
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_new_isolation_information)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_new_isolation_testing_advice, R.drawable.ic_get_free_test)
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

    private fun handleAlreadyIsolating(days: Int) {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_contact)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_continue_isolataion_for)
        riskyContactIsolationAdviceRemainingDaysInIsolation.text =
            resources.getQuantityString(R.plurals.state_isolation_days, days, days)
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_already_isolating_information)

        adviceContainer.removeAllViews()
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

    private fun handleNotIsolatingAsFullyVaccinated() {
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

        setupActionButtonsForNotIsolating()
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMinor() {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_minors_no_self_isolation_required)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_minors_information)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_minors_testing_advice, R.drawable.ic_social_distancing)
        addAdvice(R.string.risky_contact_isolation_advice_minors_show_to_adult_advice, R.drawable.ic_family)

        setupActionButtonsForNotIsolating()
        setAccessibilityTitle(isIsolating = false)
    }

    private fun handleNotIsolatingAsMedicallyExempt() {
        riskyContactIsolationAdviceIcon.setImageResource(R.drawable.ic_isolation_book_test)
        riskyContactIsolationAdviceTitle.setText(R.string.risky_contact_isolation_advice_medically_exempt_heading)
        riskyContactIsolationAdviceRemainingDaysInIsolation.gone()
        riskyContactIsolationAdviceStateInfoView.stateText =
            getString(R.string.risky_contact_isolation_advice_medically_exempt_information)

        adviceContainer.removeAllViews()
        addAdvice(R.string.risky_contact_isolation_advice_medically_exempt_research, R.drawable.ic_info)
        addAdvice(R.string.risky_contact_isolation_advice_medically_exempt_advice, R.drawable.ic_social_distancing)

        setupActionButtonsForNotIsolating()
        setAccessibilityTitle(isIsolating = false)
    }

    private fun setupActionButtonsForNotIsolating() {
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
        adviceContainer.addView(IconTextView(this, stringResId, drawableResId))

    private fun setAccessibilityTitle(isIsolating: Boolean) {
        title = if (isIsolating) {
            "${riskyContactIsolationAdviceTitle.text} ${riskyContactIsolationAdviceRemainingDaysInIsolation.text}"
        } else {
            riskyContactIsolationAdviceTitle.text
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
            context.startActivity(getIntent(context, optOutOfContactIsolationExtra))
        }

        private fun getIntent(context: Context, optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra) =
            Intent(context, RiskyContactIsolationAdviceActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, optOutOfContactIsolationExtra)
                }

        const val OPT_OUT_OF_CONTACT_ISOLATION_EXTRA = "OPT_OUT_OF_CONTACT_ISOLATION_EXTRA"
    }

    enum class OptOutOfContactIsolationExtra {
        NONE, MINOR, FULLY_VACCINATED, MEDICALLY_EXEMPT
    }
}
