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
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.CONFIRM
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.IGNORE
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.OVERWRITE
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class RelevantTestResultProvider @Inject constructor(
    private val relevantTestResultStorage: RelevantTestResultStorage,
    private val clock: Clock,
    moshi: Moshi
) : TestResultChecker {

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

    override fun hasTestResultMatching(predicate: (TestResult) -> Boolean): Boolean =
        testResult?.let { predicate(it) } ?: false

    fun storeMigratedTestResult(migratedTestResult: AcknowledgedTestResult?) = synchronized(lock) {
        testResult = migratedTestResult
    }

    fun onTestResultAcknowledged(newTestResult: ReceivedTestResult, testResultStorageOperation: TestResultStorageOperation) = synchronized(lock) {
        newTestResult.testResult.toRelevantVirologyTestResult()?.let { virologyTestResult ->
            val acknowledgedTestResult = AcknowledgedTestResult(
                newTestResult.diagnosisKeySubmissionToken,
                newTestResult.testEndDate,
                virologyTestResult,
                newTestResult.testKitType,
                acknowledgedDate = Instant.now(clock),
                requiresConfirmatoryTest = newTestResult.requiresConfirmatoryTest,
                confirmedDate = null
            )
            store(acknowledgedTestResult, testResultStorageOperation)
        }
    }

    private fun store(newTestResult: AcknowledgedTestResult, testResultStorageOperation: TestResultStorageOperation) = synchronized(lock) {
        when (testResultStorageOperation) {
            OVERWRITE -> testResult = newTestResult
            CONFIRM -> {
                val currentTestResult = testResult
                if (currentTestResult != null) {
                    testResult = currentTestResult.copy(confirmedDate = newTestResult.testEndDate)
                } else {
                    Timber.e("There is no test result to confirm")
                }
            }
            IGNORE -> { /* nothing to do */ }
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
    override val diagnosisKeySubmissionToken: String?,
    override val testEndDate: Instant,
    val testResult: RelevantVirologyTestResult,
    override val testKitType: VirologyTestKitType?,
    val acknowledgedDate: Instant,
    override val requiresConfirmatoryTest: Boolean = false,
    val confirmedDate: Instant? = null
) : TestResult {

    override fun isPositive(): Boolean =
        testResult == POSITIVE

    override fun isConfirmed(): Boolean =
        !requiresConfirmatoryTest || confirmedDate != null
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
