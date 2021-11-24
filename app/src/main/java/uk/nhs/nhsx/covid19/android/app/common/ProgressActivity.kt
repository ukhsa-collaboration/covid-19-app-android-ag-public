package uk.nhs.nhsx.covid19.android.app.common

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.LiveData
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.Lce.Error
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Lce.Success
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityProgressBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.announce
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.interruptAnnouncement
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

abstract class ProgressActivity<ResultType> : BaseActivity() {

    private lateinit var binding: ActivityProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCloseToolbar(binding.progressToolbar.toolbar, R.string.empty, R.drawable.ic_close_primary)

        setupListeners()
        startAction()
        setupViewModelListeners(viewModelLiveData())
    }

    abstract fun startAction()

    abstract fun viewModelLiveData(): LiveData<Lce<ResultType>>

    abstract fun onSuccess(result: ResultType)

    private fun setupListeners() {
        binding.buttonTryAgain.setOnSingleClickListener {
            startAction()
        }
    }

    protected fun setupViewModelListeners(observable: LiveData<Lce<ResultType>>) {
        observable.observe(this) { lce ->
            when (lce) {
                is Loading -> showLoadingSpinner()
                is Success -> onSuccess(lce.data)
                is Error -> showErrorState()
            }
        }
    }

    private fun showLoadingSpinner() = with(binding) {
        interruptAnnouncement()
        announce(R.string.loading)

        errorStateContainer.gone()
        loadingProgress.visible()
    }

    private fun showErrorState() = with(binding) {
        interruptAnnouncement()
        textErrorTitle.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)

        errorStateContainer.visible()
        loadingProgress.gone()
    }
}
