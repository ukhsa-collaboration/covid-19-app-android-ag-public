package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import android.os.Parcelable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter

class UnacknowledgedTestResultsProvider @Inject constructor(
    private val unacknowledgedTestResultsStorage: UnacknowledgedTestResultsStorage,
    private val clock: Clock,
    moshi: Moshi
) : TestResultChecker {

    private val testResultsSerializationAdapter: JsonAdapter<List<ReceivedTestResult>> =
        moshi.adapter(listOfReceivedTestResultEntriesType)

    private val lock = Object()

    var testResults: List<ReceivedTestResult>
        get() {
            return synchronized(lock) {
                unacknowledgedTestResultsStorage.value?.let {
                    runCatching {
                        testResultsSerializationAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            listOf()
                        } // TODO add crash analytics and come up with a more sophisticated solution
                } ?: listOf()
            }
        }
        private set(testResults) {
            return synchronized(lock) {
                unacknowledgedTestResultsStorage.value =
                    testResultsSerializationAdapter.toJson(testResults)
            }
        }

    fun add(testResult: ReceivedTestResult) = synchronized(lock) {
        val updatedList = testResults.toMutableList().apply {
            add(testResult)
        }
        testResults = updatedList
    }

    fun setSymptomsOnsetDate(testResult: ReceivedTestResult, symptomsOnsetDate: SymptomsDate) = synchronized(lock) {
        val updatedList = testResults.map {
            if (it == testResult) it.copy(symptomsOnsetDate = symptomsOnsetDate) else it
        }
        testResults = updatedList
    }

    fun remove(testResult: ReceivedTestResult) = synchronized(lock) {
        val updatedList = testResults.filter {
            it != testResult
        }
        testResults = updatedList
    }

    fun clearBefore(date: LocalDate) = synchronized(lock) {
        val updatedList = testResults.filter {
            it.testEndDate.atZone(clock.zone).toLocalDate().isEqualOrAfter(date)
        }
        testResults = updatedList
    }

    override fun hasTestResultMatching(predicate: (TestResult) -> Boolean): Boolean =
        testResults.any { predicate(it) }

    companion object {
        val listOfReceivedTestResultEntriesType: Type = Types.newParameterizedType(
            List::class.java,
            ReceivedTestResult::class.java
        )
    }
}

class UnacknowledgedTestResultsStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "UNACKNOWLEDGED_TEST_RESULTS_KEY"
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class ReceivedTestResult(
    override val diagnosisKeySubmissionToken: String?,
    override val testEndDate: Instant,
    val testResult: VirologyTestResult,
    override val testKitType: VirologyTestKitType?,
    val diagnosisKeySubmissionSupported: Boolean,
    override val requiresConfirmatoryTest: Boolean = false,
    val symptomsOnsetDate: SymptomsDate? = null
) : TestResult, Parcelable {

    override fun isPositive(): Boolean =
        testResult == POSITIVE

    override fun isConfirmed(): Boolean =
        !requiresConfirmatoryTest
}

@Parcelize
@JsonClass(generateAdapter = true)
data class SymptomsDate(val explicitDate: LocalDate?) : Parcelable
