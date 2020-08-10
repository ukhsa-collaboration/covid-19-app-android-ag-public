package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import org.junit.Assert
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptoms

class RiskCalculatorTest {

    private val subject = RiskCalculator()

    @Test
    fun `risk calculation is correct`() {
        val risk = subject.calculateRisk(symptoms)
        Assert.assertEquals(19.0, risk, 0.0001)
    }

    @Test
    fun `calculated risk is equal to threshold returns true`() {
        val isRiskAboveThreshold = subject.isRiskAboveThreshold(symptoms, 19.0f)
        Assert.assertEquals(true, isRiskAboveThreshold)
    }

    @Test
    fun `if calculated risk is less that threshold returns false`() {
        val isRiskAboveThreshold = subject.isRiskAboveThreshold(symptoms, 19.1f)
        Assert.assertEquals(false, isRiskAboveThreshold)
    }

    @Test
    fun `if calculated risk is greater that threshold returns true`() {
        val isRiskAboveThreshold = subject.isRiskAboveThreshold(symptoms, 18.9f)
        Assert.assertEquals(true, isRiskAboveThreshold)
    }
}
