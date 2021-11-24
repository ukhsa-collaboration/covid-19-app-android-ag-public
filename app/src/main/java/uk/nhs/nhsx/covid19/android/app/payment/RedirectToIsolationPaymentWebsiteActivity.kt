package uk.nhs.nhsx.covid19.android.app.payment

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.ProgressActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import javax.inject.Inject

typealias Url = String

class RedirectToIsolationPaymentWebsiteActivity : ProgressActivity<Url>() {

    @Inject
    lateinit var factory: ViewModelFactory<RedirectToIsolationPaymentWebsiteViewModel>
    private val viewModel: RedirectToIsolationPaymentWebsiteViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun startAction() {
        viewModel.loadIsolationPaymentUrl()
    }

    override fun viewModelLiveData(): LiveData<Lce<Url>> {
        return viewModel.fetchWebsiteUrl()
    }

    override fun onSuccess(result: Url) {
        openUrl(result, useInternalBrowser = false)
        setResult(RESULT_OK)
        finish()
    }
}
