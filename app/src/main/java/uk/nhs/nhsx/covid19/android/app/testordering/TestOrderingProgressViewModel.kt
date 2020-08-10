package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class TestOrderingProgressViewModel(
    private val loadVirologyTestOrder: LoadVirologyTestOrder,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val clock: Clock
) : ViewModel() {

    @Inject
    constructor(
        loadVirologyTestOrder: LoadVirologyTestOrder,
        testOrderingTokensProvider: TestOrderingTokensProvider
    ) : this(loadVirologyTestOrder, testOrderingTokensProvider, clock = Clock.systemUTC())

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

                    websiteUrlWithQuery.postValue(
                        Lce.Success(result.value.websiteUrlWithQuery)
                    )
                }
                is Failure -> websiteUrlWithQuery.postValue(Lce.Error(result.throwable))
            }
        }
    }
}
