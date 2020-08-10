package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.api.Status

sealed class ExposureNotificationActivationResult {

    data class ResolutionRequired(val status: Status) : ExposureNotificationActivationResult()

    object Success : ExposureNotificationActivationResult()

    data class Error(val exception: Exception) : ExposureNotificationActivationResult()
}
