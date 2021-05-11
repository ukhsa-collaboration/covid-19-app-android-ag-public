package uk.nhs.nhsx.covid19.android.app.testordering

import java.security.SecureRandom
import javax.inject.Inject
import kotlin.math.max

class GetEmptyExposureWindowSubmissionCount @Inject constructor(private val secureRandom: SecureRandom) {

    operator fun invoke(numberOfExposureWindowsSent: Int = 0): Int {
        val totalCallCount = secureRandom.nextInt(MAX_CALLS - MIN_CALLS) + MIN_CALLS
        return max(0, totalCallCount - numberOfExposureWindowsSent)
    }

    companion object {
        private const val MIN_CALLS = 2
        private const val MAX_CALLS = 16
    }
}
