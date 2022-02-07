package uk.nhs.nhsx.covid19.android.app.localstats

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.ProgressActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.localdata.LocalDataAndStatisticsActivity
import javax.inject.Inject

class FetchLocalDataProgressActivity : ProgressActivity<LocalStats>() {

    @Inject
    lateinit var factory: ViewModelFactory<FetchLocalDataViewModel>
    private val viewModel: FetchLocalDataViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun startAction() {
        viewModel.loadData()
    }

    override fun viewModelLiveData(): LiveData<Lce<LocalStats>> {
        return viewModel.localStats()
    }

    override fun onSuccess(result: LocalStats) {
        val intent = LocalDataAndStatisticsActivity.getIntent(this, result)
        startActivity(intent)
        finish()
    }
}
