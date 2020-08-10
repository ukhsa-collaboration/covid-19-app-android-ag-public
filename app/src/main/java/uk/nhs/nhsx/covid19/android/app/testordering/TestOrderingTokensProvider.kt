package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

class TestOrderingTokensProvider @Inject constructor(
    private val testOrderingTokensStorage: TestOrderingTokensStorage,
    moshi: Moshi
) {
    private val pollingConfigSerializationAdapter: JsonAdapter<List<TestOrderPollingConfig>> =
        moshi.adapter(listOfTestOrderTokenPairType)

    private val lock = Object()

    var configs: List<TestOrderPollingConfig>
        get() {
            return synchronized(lock) {
                testOrderingTokensStorage.value?.let {
                    runCatching {
                        pollingConfigSerializationAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            listOf()
                        } // TODO add crash analytics and come up with a more sophisticated solution
                } ?: listOf()
            }
        }
        private set(listOfTokenPairs) {
            return synchronized(lock) {
                testOrderingTokensStorage.value =
                    pollingConfigSerializationAdapter.toJson(listOfTokenPairs)
            }
        }

    fun add(pollingConfig: TestOrderPollingConfig) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            add(pollingConfig)
        }
        configs = updatedList
    }

    fun remove(pollingConfig: TestOrderPollingConfig) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            remove(pollingConfig)
        }
        configs = updatedList
    }

    companion object {
        val listOfTestOrderTokenPairType: Type = Types.newParameterizedType(
            List::class.java,
            TestOrderPollingConfig::class.java
        )
    }
}

class TestOrderingTokensStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "TEST_ORDERING_TOKENS_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class TestOrderPollingConfig(
    val startedAt: Instant,
    val testResultPollingToken: String,
    val diagnosisKeySubmissionToken: String
)
