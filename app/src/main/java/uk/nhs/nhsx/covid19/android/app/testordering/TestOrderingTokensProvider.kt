package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import java.time.Instant
import javax.inject.Inject

class TestOrderingTokensProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    private var storedConfigs: List<TestOrderPollingConfig> by listStorage(
        TEST_ORDERING_TOKENS_KEY,
        default = emptyList()
    )
    var configs: List<TestOrderPollingConfig>
        get() = storedConfigs
        private set(listOfTokenPairs) {
            storedConfigs = listOfTokenPairs
        }

    fun add(pollingConfig: TestOrderPollingConfig) = synchronized(lock) {
        Timber.d("Storing test result polling token: $pollingConfig")
        val updatedList = configs.toMutableList().apply {
            add(pollingConfig)
        }
        configs = updatedList
    }

    fun remove(pollingConfig: TestOrderPollingConfig) = synchronized(lock) {
        Timber.d("Deleting test result polling token: $pollingConfig")
        val updatedList = configs.toMutableList().apply {
            remove(pollingConfig)
        }
        configs = updatedList
    }

    fun removeAll(condition: TestOrderPollingConfig.() -> Boolean) = synchronized(lock) {
        val tokensToRemove = configs.filter { it.condition() }
        val updatedList = configs.toMutableList().apply {
            Timber.d("Deleting test result polling tokens: $tokensToRemove")
            removeAll(tokensToRemove)
        }
        configs = updatedList
    }

    companion object {
        const val TEST_ORDERING_TOKENS_KEY = "TEST_ORDERING_TOKENS_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class TestOrderPollingConfig(
    val startedAt: Instant,
    val testResultPollingToken: String,
    val diagnosisKeySubmissionToken: String
)
