@file:Suppress("DEPRECATION", "ClassName")

package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Instant
import javax.inject.Inject

@Deprecated("Not used anymore since 4.10. Use IndexInfo.testResult instead.")
class RelevantTestResultProvider @Inject constructor(
    private val relevantTestResultStorage: RelevantTestResultStorage,
    moshi: Moshi
) {

    private val testResultSerializationAdapter: JsonAdapter<AcknowledgedTestResult4_9> =
        moshi.adapter(AcknowledgedTestResult4_9::class.java)

    private val lock = Object()

    var testResult: AcknowledgedTestResult4_9?
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
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal set(testResult) {
            return synchronized(lock) {
                relevantTestResultStorage.value =
                    testResultSerializationAdapter.toJson(testResult)
            }
        }

    fun storeMigratedTestResult(migratedTestResult: AcknowledgedTestResult4_9?) = synchronized(lock) {
        testResult = migratedTestResult
    }

    fun clear() = synchronized(lock) {
        relevantTestResultStorage.value = null
    }
}

@Deprecated("Not used anymore since 4.10.")
class RelevantTestResultStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "RELEVANT_TEST_RESULT_KEY"
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
