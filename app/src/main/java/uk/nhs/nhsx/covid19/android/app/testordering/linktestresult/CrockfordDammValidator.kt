package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import javax.inject.Inject

class CrockfordDammValidator @Inject constructor() {

    fun validate(code: String) =
        code.isNotEmpty() && checksum(code) == 0 && "^[$CROCKFORD_BASE32]+$".toRegex().matches(code)

    private fun checksum(code: String): Int {
        return code
            .map { CROCKFORD_BASE32.indexOf(it) }
            .fold(0) { checksum: Int, index: Int -> damm32(checksum, index) }
    }

    private fun damm32(checksum: Int, digit: Int): Int {
        var dammChecksum = checksum xor digit
        dammChecksum = dammChecksum shl 1
        if (dammChecksum >= DAMM_MODULUS) {
            dammChecksum = (dammChecksum xor DAMM_MASK) % DAMM_MODULUS
        }
        return dammChecksum
    }

    companion object {
        private const val DAMM_MODULUS = 32
        private const val DAMM_MASK = 5
        const val CROCKFORD_BASE32 = "0123456789abcdefghjkmnpqrstvwxyz"
    }
}
