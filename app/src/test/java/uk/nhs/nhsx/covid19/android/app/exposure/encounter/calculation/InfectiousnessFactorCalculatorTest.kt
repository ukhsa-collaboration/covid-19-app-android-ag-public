package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class InfectiousnessFactorCalculatorTest(
    private val daysFromOnset: Int,
    private val expectedFactor: Double
) {
    private val infectiousnessFactorCalculator =
        InfectiousnessFactorCalculator()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(0, 1.0),
            arrayOf(1, 0.9360225578954148),
            arrayOf(2, 0.7676181961208854),
            arrayOf(3, 0.5515397744971644),
            arrayOf(4, 0.3472010612276297),
            arrayOf(5, 0.19149519501466308),
            arrayOf(6, 0.09253528115842204),
            arrayOf(7, 0.03917684398136177)
        )
    }

    @Test
    fun `calculates infectiousness factor for given number of days from onset`() {
        val actualFactor = infectiousnessFactorCalculator.infectiousnessFactor(daysFromOnset)
        assertEquals(expectedFactor, actualFactor)
    }
}
