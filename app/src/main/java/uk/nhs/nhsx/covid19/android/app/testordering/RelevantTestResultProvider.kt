package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class RelevantTestResultProvider @Inject constructor(
    private val relevantTestResultStorage: RelevantTestResultStorage,
    private val clock: Clock,
    moshi: Moshi
) {

    private val testResultSerializationAdapter: JsonAdapter<AcknowledgedTestResult> =
        moshi.adapter(AcknowledgedTestResult::class.java)

    private val lock = Object()

    var testResult: AcknowledgedTestResult?
        get() {
            return synchronized(lock) {
                relevantTestResultStorage.value?.let {
                    runCatching {
                        testResultSerializationAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            null
                        } // TODO add crash analytics and come up with a more sophisticated solution
                }
            }
        }
        private set(testResult) {
            return synchronized(lock) {
                relevantTestResultStorage.value =
                    testResultSerializationAdapter.toJson(testResult)
            }
        }

    fun isTestResultPositive() =
        testResult?.testResult == POSITIVE

    fun isTestResultNegative() =
        testResult?.testResult == NEGATIVE

    fun getTestResultIfPositive() = synchronized(lock) {
        if (isTestResultPositive()) testResult else null
    }

    fun hasPositiveTestResultAfter(instant: Instant): Boolean =
        getTestResultIfPositive()?.testEndDate?.isAfter(instant) ?: false

    fun hasPositiveTestResultAfterOrEqual(instant: Instant): Boolean =
        getTestResultIfPositive()?.testEndDate?.isEqualOrAfter(instant) ?: false

    fun onTestResultAcknowledged(newTestResult: ReceivedTestResult) = synchronized(lock) {
        newTestResult.testResult.toRelevantVirologyTestResult()?.let { virologyTestResult ->
            val acknowledgedTestResult = AcknowledgedTestResult(
                newTestResult.diagnosisKeySubmissionToken,
                newTestResult.testEndDate,
                virologyTestResult,
                newTestResult.testKitType,
                Instant.now(clock)
            )
            updateIfRelevant(acknowledgedTestResult)
        }
    }

    fun updateIfRelevant(newTestResult: AcknowledgedTestResult) = synchronized(lock) {
        if (newTestResult.isMoreRelevantThan(testResult)) {
            testResult = newTestResult
        }
    }

    fun clear() = synchronized(lock) {
        relevantTestResultStorage.value = null
    }
}

class RelevantTestResultStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "RELEVANT_TEST_RESULT_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class AcknowledgedTestResult(
    val diagnosisKeySubmissionToken: String?,
    val testEndDate: Instant,
    val testResult: RelevantVirologyTestResult,
    val testKitType: VirologyTestKitType?,
    val acknowledgedDate: Instant
) {

    fun isMoreRelevantThan(otherTest: AcknowledgedTestResult?): Boolean =
        if (otherTest == null) true
        else compareRelevance(otherTest) > 0

    /**
     * Compare by relevance, ascending
     */
    private fun compareRelevance(otherTest: AcknowledgedTestResult): Int {
        return compareBy<AcknowledgedTestResult> { it.testResult.relevance }
            .thenBy { it.testEndDate }
            .compare(this, otherTest)
    }
}

enum class RelevantVirologyTestResult(val relevance: Int) {
    POSITIVE(1),
    NEGATIVE(0)
}

fun VirologyTestResult.toRelevantVirologyTestResult(): RelevantVirologyTestResult? =
    when (this) {
        VirologyTestResult.POSITIVE -> POSITIVE
        VirologyTestResult.NEGATIVE -> NEGATIVE
        VOID -> null
    }
