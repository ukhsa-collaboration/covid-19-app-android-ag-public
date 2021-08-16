@file:Suppress("DEPRECATION", "ClassName")

package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.Instant
import javax.inject.Inject

@Deprecated("Not used anymore since 4.10. Use IndexInfo.testResult instead.")
class RelevantTestResultProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    private var storedTestResult: AcknowledgedTestResult4_9? by storage(RELEVANT_TEST_RESULT_KEY)

    val testResult: AcknowledgedTestResult4_9?
        get() = storedTestResult

    fun storeMigratedTestResult(migratedTestResult: AcknowledgedTestResult4_9?) = synchronized(lock) {
        storedTestResult = migratedTestResult
    }

    fun clear() = synchronized(lock) {
        storedTestResult = null
    }

    companion object {
        const val RELEVANT_TEST_RESULT_KEY = "RELEVANT_TEST_RESULT_KEY"
    }
}

@Deprecated("Not used anymore since 4.10. Use AcknowledgedTestResult instead.")
@JsonClass(generateAdapter = true)
data class AcknowledgedTestResult4_9(
    val diagnosisKeySubmissionToken: String?,
    val testEndDate: Instant,
    val testResult: RelevantVirologyTestResult,
    val testKitType: VirologyTestKitType?,
    val acknowledgedDate: Instant,
    val requiresConfirmatoryTest: Boolean = false,
    val confirmedDate: Instant? = null
)
