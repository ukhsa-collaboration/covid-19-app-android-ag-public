package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityUpdateRecommendedBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class UpdateRecommendedActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<UpdateRecommendedViewModel>

    private val viewModel: UpdateRecommendedViewModel by viewModels { factory }

    private lateinit var binding: ActivityUpdateRecommendedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityUpdateRecommendedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            updateInStore.setOnSingleClickListener {
                openAppStore()
                finish()
            }

            askMeLater.setOnSingleClickListener {
                finishAndNavigate()
            }

            viewModel.observeRecommendationInfo().observe(this@UpdateRecommendedActivity) {
                updateRecommendationDescription.text = it.description
                updateRecommendationTitle.text = it.title
                title = it.title
            }
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
