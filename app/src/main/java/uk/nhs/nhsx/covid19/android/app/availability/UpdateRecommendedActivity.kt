package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_update_recommended.askMeLater
import kotlinx.android.synthetic.main.activity_update_recommended.updateInStore
import kotlinx.android.synthetic.main.activity_update_recommended.updateRecommendationDescription
import kotlinx.android.synthetic.main.activity_update_recommended.updateRecommendationTitle
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class UpdateRecommendedActivity : BaseActivity(R.layout.activity_update_recommended) {

    @Inject
    lateinit var factory: ViewModelFactory<UpdateRecommendedViewModel>

    private val viewModel: UpdateRecommendedViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        updateInStore.setOnSingleClickListener {
            openAppStore()
        }

        askMeLater.setOnSingleClickListener {
            finishAndNavigate()
        }

        viewModel.observeRecommendationInfo().observe(this) {
            updateRecommendationDescription.text = it.description
            updateRecommendationTitle.text = it.title
        }

        viewModel.fetchRecommendationInfo()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAndNavigate()
    }

    private fun finishAndNavigate() {
        if (intent.getBooleanExtra(STARTED_FROM_NOTIFICATION, false)) {
            MainActivity.start(this)
        } else {
            finish()
        }
    }

    companion object {
        const val STARTED_FROM_NOTIFICATION = "STARTED_FROM_NOTIFICATION"
    }
}
