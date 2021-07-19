package uk.nhs.nhsx.covid19.android.ktlint

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.covid19.android.ktlint.rule.ReportedTestRule

class ReportedTestRuleTest {

    @Test
    fun `when @Test function in EspressoTest is neither not annotated as Reported nor calls reported, no error is reported`() {
        assertThat(
            ReportedTestRule().lint(
                """
                import org.junit.Test
                import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest

                class SomeUiTest : EspressoTest() {
                
                    @Test
                    fun testSomethingNotReported()  {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            )
        ).noneMatch { error -> error is LintError && error.ruleId == ReportedTestRule.RULE_ID }
    }

    @Test
    fun `when @Test function in EspressoTest is initialized with reporter, no error is reported`() {
        assertThat(
            ReportedTestRule().lint(
                """
                import org.junit.Test
                import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
                import uk.nhs.nhsx.covid19.android.app.report.Reported
                import uk.nhs.nhsx.covid19.android.app.report.Reporter
                import uk.nhs.nhsx.covid19.android.app.report.reporter

                class SomeUiTest : EspressoTest() {
                    
                    @Test
                    @Reported
                    fun testSomethingReported() = reporter(
                        "Scenario",
                        "Title",
                        "Description",
                        Reporter.Kind.FLOW
                    ) {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            )
        ).noneMatch { error -> error is LintError && error.ruleId == ReportedTestRule.RULE_ID }
    }


    @Test
    fun `when non-@Test function in EspressoTest is not initialized with notReported or reporter, no error is reported`() {
        assertThat(
            ReportedTestRule().lint(
                """
                import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest

                class SomeUiTest : EspressoTest() {
                
                    fun noTestSomethingNotSpecified() {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            )
        ).noneMatch { error -> error is LintError && error.ruleId == ReportedTestRule.RULE_ID }
    }

    @Test
    fun `when @Test function in EspressoTest is annotated with @Reported but not initialized with reporter, an error is reported`() {
        assertThat(
            ReportedTestRule().lint(
                """
                import org.junit.Test
                import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
                import uk.nhs.nhsx.covid19.android.app.report.Reported
                
                class SomeUiTest : EspressoTest() {
                
                    @Test
                    @Reported
                    fun testNotInitialized() {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                7, 5,
                ReportedTestRule.RULE_ID,
                ReportedTestRule.DEFAULT_ERROR_MESSAGE
            )
        )
    }

    @Test
    fun `when @Test function in EspressoTest is initialized with reporter but @Reported annotation is missing, an error is reported`() {
        assertThat(
            ReportedTestRule().lint(
                """
                import org.junit.Test
                import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
                import uk.nhs.nhsx.covid19.android.app.report.reporter
                
                class SomeUiTest : EspressoTest() {
                
                    @Test
                    fun testNotAnnotated() = reporter(
                        "Scenario",
                        "Title",
                        "Description",
                        Reporter.Kind.FLOW
                    ) {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                7, 5,
                ReportedTestRule.RULE_ID,
                ReportedTestRule.DEFAULT_ERROR_MESSAGE
            )
        )
    }
}
