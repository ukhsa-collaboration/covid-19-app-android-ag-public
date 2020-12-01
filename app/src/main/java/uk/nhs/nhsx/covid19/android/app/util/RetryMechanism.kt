package uk.nhs.nhsx.covid19.android.app.util

import timber.log.Timber
import java.security.SecureRandom
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

object RetryMechanism {

    private val random = KotlinSecureRandom()
    private const val DEFAULT_MAX_TOTAL_DELAY_MILLISECONDS = 15 * 1000L
    private const val DEFAULT_MAX_DELAY_MILLISECONDS = 3 * 1000L
    private const val DEFAULT_MIN_DELAY_MILLISECONDS = 25L
    private const val DEFAULT_RETRY_MULTIPLIER = 1.5

    fun <T> retryWithBackOff(
        maxTotalDelay: Long = DEFAULT_MAX_TOTAL_DELAY_MILLISECONDS,
        action: () -> T
    ): T {
        var current = Attempt()
        while (true) {
            Timber.v("Executing attempt: $current")
            try {
                return action()
            } catch (e: Exception) {
                current = current.copy(exception = e)
            }

            if (current.totalDelay > maxTotalDelay) {
                Timber.w("Retry condition exceeded: $current")
                throw current.exception!!
            }

            val newDelay = calculateDelay(current)
            Thread.sleep(newDelay)

            current = current.copy(
                count = current.count + 1,
                lastDelay = newDelay,
                totalDelay = current.totalDelay + newDelay
            )
        }
    }

    private fun calculateDelay(
        attempt: Attempt,
        maxDelay: Long = DEFAULT_MAX_DELAY_MILLISECONDS,
        minDelay: Long = DEFAULT_MIN_DELAY_MILLISECONDS,
        multiplier: Double = DEFAULT_RETRY_MULTIPLIER
    ): Long {
        val exp = 2.0.pow(attempt.count.toDouble())
        val calculatedDelay = (multiplier * exp).roundToLong()

        val (shorterDelay, longerDelay) = listOf(attempt.lastDelay, calculatedDelay).sorted()
        val newDelay = (shorterDelay..longerDelay).random(random)

        return newDelay.coerceIn(minDelay, maxDelay)
    }

    data class Attempt(
        val count: Int = 1,
        val totalDelay: Long = 0L,
        val lastDelay: Long = 0L,
        val exception: Exception? = null
    )

    class KotlinSecureRandom : Random() {
        private val secureRandom = InternalRandom().apply {
            nextBytes(ByteArray(20)) // seed random
        }

        override fun nextBits(bitCount: Int) = secureRandom.nextBits(bitCount)

        class InternalRandom : SecureRandom() {
            fun nextBits(bits: Int): Int {
                return next(bits)
            }
        }
    }
}
