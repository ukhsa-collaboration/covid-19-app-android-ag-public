package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class TestResultsProvider(
    private val latestTestResultProvider: LatestTestResultProvider,
    private val testResultsStorage: TestResultsStorage,
    moshi: Moshi,
    private val clock: Clock
) {
    @Inject
    constructor(
        latestTestResultProvider: LatestTestResultProvider,
        testResultsStorage: TestResultsStorage,
        moshi: Moshi
    ) : this(latestTestResultProvider, testResultsStorage, moshi, clock = Clock.systemUTC())

    private val testResultsSerializationAdapter: JsonAdapter<Map<String, ReceivedTestResult>> =
        moshi.adapter(listOfReceivedTestResultPairType)

    private val lock = Object()

    var testResults: Map<String, ReceivedTestResult>
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
        private set(testResults) {
            return synchronized(lock) {
                testResultsStorage.value =
                    testResultsSerializationAdapter.toJson(testResults)
            }
        }

    init {
        // Migration from from latestTestResultProvider to TestResultsProvider
        latestTestResultProvider.latestTestResult?.let {
            add(
                ReceivedTestResult(
                    it.diagnosisKeySubmissionToken,
                    it.testEndDate,
                    it.testResult
                )
            )
            latestTestResultProvider.latestTestResult = null
        }
    }

    fun add(testResult: ReceivedTestResult) = synchronized(lock) {
        val updatedList = testResults.toMutableMap().apply {
            put(testResult.diagnosisKeySubmissionToken, testResult)
        }
        testResults = updatedList
    }

    fun remove(testResult: ReceivedTestResult) = synchronized(lock) {
        val updatedList = testResults.toMutableMap().apply {
            remove(testResult.diagnosisKeySubmissionToken)
        }
        testResults = updatedList
    }

    fun clear() = synchronized(lock) {
        testResultsStorage.value = null
    }

    fun acknowledge(testResult: ReceivedTestResult) = synchronized(lock) {
        val updatedList = testResults.toMutableMap().apply {
            put(
                testResult.diagnosisKeySubmissionToken,
                testResult.copy(acknowledgedDate = Instant.now(clock))
            )
        }
        testResults = updatedList
    }

    fun isLastTestResultPositive(): Boolean =
        getLastTestResult()?.testResult == POSITIVE

    fun isLastTestResultNegative() =
        getLastTestResult()?.testResult == NEGATIVE

    fun getLastTestResult(): ReceivedTestResult? = synchronized(lock) {
        testResults.values
            .filter { it.acknowledgedDate != null }
            .sortedBy { it.acknowledgedDate }
            .lastOrNull()
    }

    fun find(submissionToken: String): ReceivedTestResult? =
        testResults[submissionToken]

    companion object {
        val listOfReceivedTestResultPairType: Type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            ReceivedTestResult::class.java
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

@JsonClass(generateAdapter = true)
data class ReceivedTestResult(
    val diagnosisKeySubmissionToken: String,
    val testEndDate: Instant,
    val testResult: VirologyTestResult,
    val acknowledgedDate: Instant? = null
)
