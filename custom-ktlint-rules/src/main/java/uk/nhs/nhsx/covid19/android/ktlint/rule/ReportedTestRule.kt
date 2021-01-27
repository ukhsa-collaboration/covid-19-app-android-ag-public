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
 * All functions annotated with @Test within classes that extend EspressoTest must be initialized with
 * notReported or reporter. See ReportedTestRuleTest for valid and invalid examples.
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
                !function.isExplicitAboutReporting()
            ) {
                emit(node.startOffset, DEFAULT_ERROR_MESSAGE, false)
            }
        }
    }

    private fun KtFunction.isTestFunction(): Boolean =
        annotationEntries.any { it.shortName?.asString() == TEST_ANNOTATION }
                && containingKtFile.hasImport(TEST_ANNOTATION_QUALIFIED)

    private fun KtElement.isContainedInEspressoTest(): Boolean {
        val extendsEspressoTest =
            (containingClass()?.getSuperTypeList()?.entries?.any {
                it.typeAsUserType?.referencedName == ESPRESSO_TEST
            } // TODO deal with superclasses
                ?: false)
        return extendsEspressoTest && containingKtFile.hasImport(ESPRESSO_TEST_QUALIFIED)
    }

    private fun KtFunction.isExplicitAboutReporting(): Boolean {
        val initializerCallName = ((this as? KtNamedFunction)?.initializer as? KtCallElement)?.getCallNameExpression()?.text
        return initializerCallName == NOT_REPORTED || initializerCallName == REPORTER
    }

    companion object {
        const val RULE_ID = "reported-test"
        const val DEFAULT_ERROR_MESSAGE = "For Espresso Tests, each @Test must be explicitly reported or not reported"
        const val TEST_ANNOTATION = "Test"
        const val TEST_ANNOTATION_QUALIFIED = "org.junit.$TEST_ANNOTATION"
        const val ESPRESSO_TEST = "EspressoTest"
        const val ESPRESSO_TEST_QUALIFIED = "uk.nhs.nhsx.covid19.android.app.testhelpers.base.$ESPRESSO_TEST"
        private const val NOT_REPORTED = "notReported"
        private const val REPORTER = "reporter"
    }
}
