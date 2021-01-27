package uk.nhs.nhsx.covid19.android.ktlint

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import uk.nhs.nhsx.covid19.android.ktlint.rule.NoSetOnClickListenerRule
import uk.nhs.nhsx.covid19.android.ktlint.rule.ReportedTestRule

class CustomRuleSetProvider : RuleSetProvider {
    override fun get() = RuleSet("custom-ktlint-rules",
        NoSetOnClickListenerRule(),
        ReportedTestRule()
    )
}
