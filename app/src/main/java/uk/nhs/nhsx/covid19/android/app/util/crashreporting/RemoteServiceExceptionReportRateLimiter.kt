package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import java.security.SecureRandom
import javax.inject.Inject

class RemoteServiceExceptionReportRateLimiter @Inject constructor(private val random: SecureRandom) {

    fun isAllowed(): Boolean =
        random.nextInt(TOTAL) < LIMIT

    companion object {
        private const val LIMIT = 5
        private const val TOTAL = 100
    }
}
