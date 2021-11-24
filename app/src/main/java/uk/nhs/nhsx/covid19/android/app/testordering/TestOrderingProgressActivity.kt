package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.Context
import android.content.Intent
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

class TestOrderingProgressActivity : ProgressActivity<Url>() {

    @Inject
    lateinit var factory: ViewModelFactory<TestOrderingProgressViewModel>
    private val viewModel: TestOrderingProgressViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun startAction() {
        viewModel.loadVirologyTestOrder()
    }

    override fun viewModelLiveData(): LiveData<Lce<Url>> {
        return viewModel.websiteUrlWithQuery()
    }

    override fun onSuccess(result: Url) {
        openUrl(result)
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, TestOrderingProgressActivity::class.java)
    }
}
