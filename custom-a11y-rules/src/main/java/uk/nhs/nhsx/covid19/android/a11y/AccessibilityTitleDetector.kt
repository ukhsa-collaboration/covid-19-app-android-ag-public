package uk.nhs.nhsx.covid19.android.a11y

import com.android.SdkConstants
import com.android.tools.lint.checks.ManifestDetector
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.w3c.dom.Element


class AccessibilityTitleDetector : ManifestDetector(), XmlScanner {

    companion object {
        val ISSUE_MISSING_ACCESSIBILITY_LABEL: Issue = Issue.create(
            // used in @SuppressLint warnings etc
            id = "AccessibilityLabel",
            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            briefDescription = "Ensure accessibility title is announced using non-empty android:label=\'\' attribute",
            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            explanation = """
                When in accessibility mode, the title of the activity will not be announced to the \
                user unless there is a label attribute associated to that screen. This may result \
                in the user not being aware of the screen in which the application has arrived.
                """.trimMargin(),
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                AccessibilityTitleDetector::class.java,
                Scope.MANIFEST_SCOPE
            )
        )

        const val ACTIVITY_NAME_ATTRIBUTE = "android:name"
        const val ACTIVITY_LABEL_ATTRIBUTE = "android:label"
    }

    override fun getApplicableElements(): Collection<String> =
        listOf(SdkConstants.TAG_ACTIVITY)

    override fun visitElement(context: XmlContext, element: Element) {
        if (SdkConstants.TAG_ACTIVITY.equals(element.nodeName, ignoreCase = true)) {

            if (!element.hasAttribute(ACTIVITY_LABEL_ATTRIBUTE)
                || element.getAttributeNode(ACTIVITY_LABEL_ATTRIBUTE) == null
                || element.getAttributeNode(ACTIVITY_LABEL_ATTRIBUTE).value.isNullOrBlank()) {
                context.report(
                    issue = ISSUE_MISSING_ACCESSIBILITY_LABEL,
                    location = context.getLocation(element.getAttributeNode(ACTIVITY_NAME_ATTRIBUTE)),
                    message = ISSUE_MISSING_ACCESSIBILITY_LABEL.getBriefDescription(TextFormat.TEXT)
                )
            }
        }
    }
}