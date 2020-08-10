package uk.nhs.nhsx.covid19.android.app.exposure

import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.DateWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

fun filterKeysInWindow(
    dateWindow: DateWindow?,
    keys: List<NHSTemporaryExposureKey>
): List<NHSTemporaryExposureKey> {
    return if (dateWindow != null) {
        keys.filter { it.rollingPeriod == 144 }
            .filter {
                val utcTime = it.rollingStartNumber * Duration.ofMinutes(10).toMillis()
                val localDate =
                    Instant.ofEpochMilli(utcTime).atZone(ZoneOffset.UTC).toLocalDate()
                val inDateWindow =
                    !(localDate.isBefore(dateWindow.fromInclusive) || localDate.isAfter(dateWindow.toInclusive))
                inDateWindow
            }
    } else {
        keys
    }
}
