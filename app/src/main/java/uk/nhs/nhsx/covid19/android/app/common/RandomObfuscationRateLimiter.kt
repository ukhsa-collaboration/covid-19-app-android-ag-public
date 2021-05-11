package uk.nhs.nhsx.covid19.android.app.common

import java.security.SecureRandom
import javax.inject.Inject

class RandomObfuscationRateLimiter @Inject constructor(private val random: SecureRandom) {

    val allow: Boolean
        get() = random.nextInt(10) == 0
}
