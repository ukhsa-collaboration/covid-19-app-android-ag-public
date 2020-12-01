package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon

class PolicyIconAdapter {
    @ToJson
    fun toJson(policyIcon: PolicyIcon): String {
        return policyIcon.jsonName
    }

    @FromJson
    fun fromJson(policyIcon: String): PolicyIcon {
        return PolicyIcon.values().firstOrNull { it.jsonName == policyIcon } ?: PolicyIcon.DEFAULT
    }
}
