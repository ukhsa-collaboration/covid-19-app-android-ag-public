package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Instant
import javax.inject.Inject

class LatestTestResultProvider @Inject constructor(
    private val latestTestResultStorage: LatestTestResultStorage,
    moshi: Moshi
) {

    private val latestTestResultSerializationAdapter =
        moshi.adapter(LatestTestResult::class.java)

    var latestTestResult: LatestTestResult?
        get() =
            latestTestResultStorage.value?.let {
                runCatching {
                    latestTestResultSerializationAdapter.fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        null
                    } // TODO add crash analytics and come up with a more sophisticated solution
            }
        set(value) {
            latestTestResultStorage.value =
                latestTestResultSerializationAdapter.toJson(value)
        }
}

class LatestTestResultStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "LATEST_TEST_RESULT_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class LatestTestResult(
    val diagnosisKeySubmissionToken: String,
    val testEndDate: Instant,
    val testResult: VirologyTestResult
)
