package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

@Suppress("DEPRECATION")
@Deprecated("Not used anymore since 4.3. Use TestResultHandler/UnacknowledgedTestResultsProvider/RelevantTestResultProvider instead.")
class TestResultsProvider @Inject constructor(
    private val testResultsStorage: TestResultsStorage,
    moshi: Moshi
) {

    private val testResultsSerializationAdapter: JsonAdapter<Map<String, TestResult>> =
        moshi.adapter(listOfTestResultPairType)

    private val lock = Object()

    val testResults: Map<String, TestResult>
        get() {
            return synchronized(lock) {
                testResultsStorage.value?.let {
                    runCatching {
                        testResultsSerializationAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            mapOf()
                        } // TODO add crash analytics and come up with a more sophisticated solution
                } ?: mapOf()
            }
        }

    fun clear() {
        testResultsStorage.value = null
    }

    companion object {
        val listOfTestResultPairType: Type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            TestResult::class.java
        )
    }
}

class TestResultsStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "TEST_RESULTS_KEY"
    }
}

@Deprecated("Not used anymore since 4.3. Use ReceivedTestResult/AcknowledgedTestResult instead.")
@JsonClass(generateAdapter = true)
data class TestResult(
    val diagnosisKeySubmissionToken: String,
    val testEndDate: Instant,
    val testResult: VirologyTestResult,
    val acknowledgedDate: Instant? = null
)
