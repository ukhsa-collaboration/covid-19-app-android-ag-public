package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.Instant
import javax.inject.Inject

@Deprecated("Not used anymore since 3.3. Use TestResultsProvider instead.")
class LatestTestResultProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var latestTestResult: LatestTestResult? by storage(LATEST_TEST_RESULT_KEY)

    companion object {
        const val LATEST_TEST_RESULT_KEY = "LATEST_TEST_RESULT_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class LatestTestResult(
    val diagnosisKeySubmissionToken: String,
    val testEndDate: Instant,
    val testResult: VirologyTestResult
)
