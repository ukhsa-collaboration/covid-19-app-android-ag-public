package uk.nhs.nhsx.covid19.android.app.analytics

import android.os.Build
import android.os.Build.VERSION
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import javax.inject.Inject

class MetadataProvider @Inject constructor(val postCodeProvider: PostCodeProvider, val localAuthorityProvider: LocalAuthorityProvider) {
    fun getMetadata(): Metadata {
        val latestApplicationVersion = BuildConfig.VERSION_NAME_SHORT
        return Metadata(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            latestApplicationVersion = latestApplicationVersion,
            postalDistrict = postCodeProvider.value.orEmpty(),
            operatingSystemVersion = "${VERSION.SDK_INT}",
            localAuthority = localAuthorityProvider.value
        )
    }
}
