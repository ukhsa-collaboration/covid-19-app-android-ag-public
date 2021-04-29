package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import java.security.SecureRandom

class AndroidRandomNonRiskyExposureWindowsLimiter(
    private val random: SecureRandom = SecureRandom()
) : RandomNonRiskyExposureWindowsLimiter {

    override fun isAllowed(): Boolean =
        random.nextInt(TOTAL) < LIMIT

    companion object {
        private const val LIMIT = 25
        private const val TOTAL = 1000
    }
}
