package uk.nhs.nhsx.covid19.android.app.localstats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.util.getViewModelScopeOrDefault
import javax.inject.Inject

class FetchLocalDataViewModel(
    private val coroutineScopeProvider: CoroutineScope? = null,
    private val fetchLocalStats: FetchLocalStats,
    private val localStatsMapper: LocalStatsMapper
) : ViewModel() {

    @Inject
    constructor(
        fetchLocalStats: FetchLocalStats,
        localStatsMapper: LocalStatsMapper
    ) : this(coroutineScopeProvider = null, fetchLocalStats, localStatsMapper)

    private var localStats = MutableLiveData<Lce<LocalStats>>()
    fun localStats(): LiveData<Lce<LocalStats>> = localStats

    private val viewModelScope = getViewModelScopeOrDefault(coroutineScopeProvider)

    fun loadData() {
        localStats.value = Lce.Loading
        viewModelScope.launch {
            try {
                val response = fetchLocalStats()
                localStats.value = Lce.Success(localStatsMapper.map(response))
            } catch (throwable: Throwable) {
                localStats.value = Lce.Error(throwable)
            }
        }
    }
}
