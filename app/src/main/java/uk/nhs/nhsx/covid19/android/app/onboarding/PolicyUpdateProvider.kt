package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class PolicyUpdateProvider @Inject constructor(
    private val policyUpdateStorage: PolicyUpdateStorage
) {

    fun isPolicyAccepted(): Boolean =
        policyUpdateStorage.value?.let {
            compareVersions(it, LATEST_POLICY_VERSION) >= 0
        } ?: false

    fun markPolicyAccepted() {
        policyUpdateStorage.value = BuildConfig.VERSION_NAME_SHORT
    }

    @VisibleForTesting
    /***
     * Returns:
     * -1 - if version1 < version2, 0 - if version1 == version2, 1 - if version1 > version2
     */
    internal fun compareVersions(version1: String, version2: String): Int {
        val version1Splits = version1.split(".")
        val version2Splits = version2.split(".")
        val maxLengthOfVersionSplits = max(version1Splits.size, version2Splits.size)
        for (i in 0 until maxLengthOfVersionSplits) {
            val v1 = if (i < version1Splits.size) version1Splits[i].toInt() else 0
            val v2 = if (i < version2Splits.size) version2Splits[i].toInt() else 0
            val compare = v1.compareTo(v2)
            if (compare != 0) {
                return compare
            }
        }
        return 0
    }

    companion object {
        private const val LATEST_POLICY_VERSION = "3.10"
    }
}

class PolicyUpdateStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        private const val VALUE_KEY = "ACCEPTED_POLICY_APP_VERSION"
    }
}
