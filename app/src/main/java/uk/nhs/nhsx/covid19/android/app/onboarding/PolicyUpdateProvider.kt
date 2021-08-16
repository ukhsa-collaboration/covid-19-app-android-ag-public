package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.util.CompareReleaseVersions
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyUpdateProvider @Inject constructor(
    private val policyUpdateStorage: PolicyUpdateStorage,
    private val compareReleaseVersions: CompareReleaseVersions
) {

    fun isPolicyAccepted(): Boolean =
        policyUpdateStorage.value?.let {
            compareReleaseVersions(it, LATEST_POLICY_VERSION) >= 0
        } ?: false

    fun markPolicyAccepted() {
        policyUpdateStorage.value = BuildConfig.VERSION_NAME_SHORT
    }

    companion object {
        private const val LATEST_POLICY_VERSION = "4.16"
    }
}

class PolicyUpdateStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        private const val VALUE_KEY = "ACCEPTED_POLICY_APP_VERSION"
    }
}
