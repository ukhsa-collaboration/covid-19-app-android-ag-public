package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class UnacknowledgedTestResultsProvider @Inject constructor(
    private val clock: Clock,
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    private var storedTestResults: List<ReceivedTestResult> by listStorage(UNACKNOWLEDGED_TEST_RESULTS_KEY, default = emptyList())

    var testResults: List<ReceivedTestResult>
        get() = storedTestResults
        private set(testResults) {
            storedTestResults = testResults
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
            it.testEndDate.toLocalDate(clock.zone).isEqualOrAfter(date)
        }
        testResults = updatedList
    }

    companion object {
        const val UNACKNOWLEDGED_TEST_RESULTS_KEY = "UNACKNOWLEDGED_TEST_RESULTS_KEY"
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class ReceivedTestResult(
    val diagnosisKeySubmissionToken: String?,
    val testEndDate: Instant,
    val testResult: VirologyTestResult,
    override val testKitType: VirologyTestKitType?,
    val diagnosisKeySubmissionSupported: Boolean,
    val requiresConfirmatoryTest: Boolean = false,
    val symptomsOnsetDate: SymptomsDate? = null,
    override val confirmatoryDayLimit: Int? = null
) : TestResult, Parcelable {

    override fun isPositive(): Boolean =
        testResult == POSITIVE

    override fun isNegative(): Boolean =
        testResult == NEGATIVE

    fun isConfirmed(): Boolean =
        !requiresConfirmatoryTest

    override fun testEndDate(clock: Clock): LocalDate =
        testEndDate.toLocalDate(clock.zone)
}

@Parcelize
@JsonClass(generateAdapter = true)
data class SymptomsDate(val explicitDate: LocalDate?) : Parcelable
