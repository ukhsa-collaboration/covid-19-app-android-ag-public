package uk.nhs.nhsx.covid19.android.ktlint.rule

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import uk.nhs.nhsx.covid19.android.ktlint.util.hasImport

/**
 * All test functions within classes that extend EspressoTest annotated with @Reported
 * or calling reported method must both be annotated and call the reported method
 * See ReportedTestRuleTest for valid and invalid examples.
 *
 * Caveat: at the moment this rule can only deal with direct inheritance of EspressoTest. Classes that
 * extend another class that in turn extends EspressoTest will not be covered by the rule.
 */
class ReportedTestRule : Rule(RULE_ID) {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (
            offset: Int, errorMessage: String, canBeAutoCorrected:
            Boolean
        ) -> Unit
    ) {
        if (node.elementType == KtNodeTypes.FUN) {
            val function = node.psi as KtFunction
            if (function.isTestFunction() &&
                function.isContainedInEspressoTest() &&
                function.hasReporterInitializer() != function.isAnnotatedAsReported()
            ) {
                emit(node.startOffset, DEFAULT_ERROR_MESSAGE, false)
            }
        }
    }

    private fun KtFunction.isTestFunction(): Boolean =
        annotationEntries.any { it.shortName?.asString() == TEST_ANNOTATION }
                && containingKtFile.hasImport(TEST_ANNOTATION_QUALIFIED)

    private fun KtFunction.isAnnotatedAsReported(): Boolean =
        annotationEntries.any { it.shortName?.asString() == REPORTED_ANNOTATION }
                && containingKtFile.hasImport(TEST_ANNOTATION_QUALIFIED)

    private fun KtElement.isContainedInEspressoTest(): Boolean {
        val extendsEspressoTest =
            (containingClass()?.getSuperTypeList()?.entries?.any {
                it.typeAsUserType?.referencedName == ESPRESSO_TEST
            } // TODO deal with superclasses
                ?: false)
        return extendsEspressoTest && containingKtFile.hasImport(ESPRESSO_TEST_QUALIFIED)
    }

    private fun KtFunction.hasReporterInitializer(): Boolean {
        val initializerCallName = ((this as? KtNamedFunction)?.initializer as? KtCallElement)?.getCallNameExpression()?.text
        return initializerCallName == REPORTER
    }

    companion object {
        const val RULE_ID = "reported-test"
        const val DEFAULT_ERROR_MESSAGE = "For DoReTo tests, each test must be annotated with @Reported and explicitly reported"
        const val TEST_ANNOTATION = "Test"
        const val REPORTED_ANNOTATION = "Reported"
        const val TEST_ANNOTATION_QUALIFIED = "org.junit.$TEST_ANNOTATION"
        const val REPORTED_ANNOTATION_QUALIFIED = "uk.nhs.nhsx.covid19.android.app.report.$REPORTED_ANNOTATION"
        const val ESPRESSO_TEST = "EspressoTest"
        const val ESPRESSO_TEST_QUALIFIED = "uk.nhs.nhsx.covid19.android.app.testhelpers.base.$ESPRESSO_TEST"
        private const val REPORTER = "reporter"
    }
}
