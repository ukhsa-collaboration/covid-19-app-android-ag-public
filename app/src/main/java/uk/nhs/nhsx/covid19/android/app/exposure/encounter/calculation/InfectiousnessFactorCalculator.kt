package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import kotlin.math.exp
import kotlin.math.pow

class InfectiousnessFactorCalculator {
    companion object {
        private const val sigma = 2.75
    }

    // Calculated using equation 3 from the paper at https://arxiv.org/pdf/2005.11057.pdf
    fun infectiousnessFactor(daysFromOnset: Int): Double {
        val step1 = daysFromOnset / sigma
        val step2 = step1.pow(2)
        val step3 = -0.5 * step2
        return exp(step3)
    }
}
