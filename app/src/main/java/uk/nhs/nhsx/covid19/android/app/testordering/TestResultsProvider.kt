package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.mapStorage
import java.time.Instant
import javax.inject.Inject

@Suppress("DEPRECATION")
@Deprecated("Not used anymore since 4.3. Use TestResultHandler/UnacknowledgedTestResultsProvider/RelevantTestResultProvider instead.")
class TestResultsProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private var storedTestResults: Map<String, OldTestResult>? by mapStorage(TEST_RESULTS_KEY)
    val testResults: Map<String, OldTestResult>
        get() = storedTestResults ?: emptyMap()

    fun clear() {
        storedTestResults = null
    }

    companion object {
        const val TEST_RESULTS_KEY = "TEST_RESULTS_KEY"
    }
}

@Deprecated("Not used anymore since 4.3. Use ReceivedTestResult/AcknowledgedTestResult instead.")
@JsonClass(generateAdapter = true)
data class OldTestResult(
    val diagnosisKeySubmissionToken: String,
    val testEndDate: Instant,
    val testResult: VirologyTestResult,
    val acknowledgedDate: Instant? = null
)
