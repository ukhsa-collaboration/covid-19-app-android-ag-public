package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedTestOrdering
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success

class TestOrderingProgressViewModel @Inject constructor(
    private val loadVirologyTestOrder: LoadVirologyTestOrder,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) : ViewModel() {

    private var websiteUrlWithQuery = MutableLiveData<Lce<String>>()
    fun websiteUrlWithQuery(): LiveData<Lce<String>> = websiteUrlWithQuery

    fun loadVirologyTestOrder() {
        viewModelScope.launch {
            websiteUrlWithQuery.postValue(Lce.Loading)

            when (val result = loadVirologyTestOrder.invoke()) {
                is Success -> {
                    testOrderingTokensProvider.add(
                        TestOrderPollingConfig(
                            testResultPollingToken = result.value.testResultPollingToken,
                            diagnosisKeySubmissionToken = result.value.diagnosisKeySubmissionToken,
                            startedAt = Instant.now(clock)
                        )
                    )

                    analyticsEventProcessor.track(LaunchedTestOrdering)

                    websiteUrlWithQuery.postValue(Lce.Success(result.value.websiteUrlWithQuery))
                }
                is Failure -> websiteUrlWithQuery.postValue(Lce.Error(result.throwable))
            }
        }
    }
}
