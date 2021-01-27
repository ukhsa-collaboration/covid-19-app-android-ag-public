package uk.nhs.nhsx.covid19.android.ktlint

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.covid19.android.ktlint.rule.NoSetOnClickListenerRule
import uk.nhs.nhsx.covid19.android.ktlint.rule.NoSetOnClickListenerRule.Companion.SET_ON_CLICK_LISTENER
import uk.nhs.nhsx.covid19.android.ktlint.rule.NoSetOnClickListenerRule.Companion.SET_ON_SINGLE_CLICK_LISTENER
import uk.nhs.nhsx.covid19.android.ktlint.rule.NoSetOnClickListenerRule.Companion.SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED

class NoSetOnClickListenerRuleTest {

    @Test
    fun `when calling setOnClickListener without lambda, error is reported`() {
        assertThat(
            NoSetOnClickListenerRule().lint(
                """
                import android.view.View
                
                fun setListener(view: View) {
                    view.$SET_ON_CLICK_LISTENER(x)
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                4, 10,
                NoSetOnClickListenerRule.RULE_ID,
                NoSetOnClickListenerRule.DEFAULT_ERROR_MESSAGE
            )
        )
    }

    @Test
    fun `when calling setOnClickListener with lambda, error is reported`() {
        assertThat(
            NoSetOnClickListenerRule().lint(
                """
                import android.view.View
                
                fun setListener(view: View) {
                    view.$SET_ON_CLICK_LISTENER {
                        view.background = null
                    }
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                4, 10,
                NoSetOnClickListenerRule.RULE_ID,
                NoSetOnClickListenerRule.DEFAULT_ERROR_MESSAGE
            )
        )
    }

    @Test
    fun `when calling setOnClickListener from setOnSingleClickListener, no error is reported`() {
        assertThat(
            NoSetOnClickListenerRule().lint(
                """
                fun View.$SET_ON_SINGLE_CLICK_LISTENER(listener: () -> Unit) {
                    $SET_ON_CLICK_LISTENER(object : OnSingleClickListener() {
                        override fun onSingleClick(v: View?) {
                            listener()
                        }
                    })
                }
                """.trimIndent()
            )
        ).noneMatch {
            error -> error is LintError && error.ruleId == NoSetOnClickListenerRule.RULE_ID
        }
    }

    @Test
    fun `when calling setOnClickListener without lambda and without existing import, error can be fixed`() {
        assertThat(
            NoSetOnClickListenerRule().format(
                """
                import android.view.View
                
                fun setListener(view: View) {
                    view.$SET_ON_CLICK_LISTENER(x)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            import android.view.View
             import $SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED
            
            fun setListener(view: View) {
                view.$SET_ON_SINGLE_CLICK_LISTENER(x)
            }
            """.trimIndent()
        )
    }

    @Test
    fun `when calling setOnClickListener without lambda and with existing import, error can be fixed`() {
        assertThat(
            NoSetOnClickListenerRule().format(
                """
                import android.view.View
                import $SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED
                
                fun setListener(view: View) {
                    view.$SET_ON_CLICK_LISTENER(x)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            import android.view.View
            import $SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED
            
            fun setListener(view: View) {
                view.$SET_ON_SINGLE_CLICK_LISTENER(x)
            }
            """.trimIndent()
        )
    }

    @Test
    fun `when calling setOnClickListener with lambda and without existing import, error can be fixed`() {
        assertThat(
            NoSetOnClickListenerRule().format(
                """
                import android.view.View
                
                fun setListener(view: View) {
                    view.$SET_ON_CLICK_LISTENER {
                        view.background = null
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            import android.view.View
             import $SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED
            
            fun setListener(view: View) {
                view.$SET_ON_SINGLE_CLICK_LISTENER {
                    view.background = null
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `when calling setOnClickListener with lambda and with existing import, error can be fixed`() {
        assertThat(
            NoSetOnClickListenerRule().format(
                """
                import android.view.View
                import $SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED
                
                fun setListener(view: View) {
                    view.$SET_ON_CLICK_LISTENER {
                        view.background = null
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            import android.view.View
            import $SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED
            
            fun setListener(view: View) {
                view.$SET_ON_SINGLE_CLICK_LISTENER {
                    view.background = null
                }
            }
            """.trimIndent()
        )
    }
}
