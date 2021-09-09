package uk.nhs.nhsx.covid19.android.a11y

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class NhsIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(AccessibilityTitleDetector.ISSUE_MISSING_ACCESSIBILITY_LABEL)

    override val api: Int
        get() = CURRENT_API
}