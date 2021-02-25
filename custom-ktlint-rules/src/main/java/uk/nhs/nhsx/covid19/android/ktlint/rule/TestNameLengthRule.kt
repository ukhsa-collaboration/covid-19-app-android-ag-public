package uk.nhs.nhsx.covid19.android.ktlint.rule

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import uk.nhs.nhsx.covid19.android.ktlint.util.hasImport

/**
 * All functions annotated with @Test within classes that extend EspressoTest must be initialized with
 * notReported or reporter. See ReportedTestRuleTest for valid and invalid examples.
 *
 * Caveat: at the moment this rule can only deal with direct inheritance of EspressoTest. Classes that
 * extend another class that in turn extends EspressoTest will not be covered by the rule.
 */
class TestNameLengthRule : Rule(RULE_ID) {
    private var className: String = ""

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (
            offset: Int, errorMessage: String, canBeAutoCorrected:
            Boolean
        ) -> Unit
    ) {
        when (node.elementType) {
            KtNodeTypes.FUN -> {
                val function = node.psi as KtFunction
                val excessiveCharacters = function.nameLengthExcessiveCharacters()
                if (function.isTestFunction() &&
                    function.isContainedInEspressoTest() &&
                    excessiveCharacters > 0
                ) {
                    val ownName = (function as? KtNamedFunction)?.name ?: ""
                    val substring = ownName.substring(0, ownName.length - excessiveCharacters)
                    val excessivePart = ownName.substring(ownName.length - excessiveCharacters)
                    emit(node.startOffset, "$DEFAULT_ERROR_MESSAGE\n$substring[$excessivePart]", false)
                }
            }
            KtNodeTypes.CLASS -> {
                val clazz = node.psi as KtClass
                className = clazz.fqName.toString()
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

    private fun KtFunction.nameLengthExcessiveCharacters(): Int {
        val ownName = (this as? KtNamedFunction)?.name
        return "coverage.ec$className#$ownName.ec_.gstmp".length - 255
    }

    companion object {
        const val RULE_ID = "test-name-too-long"
        const val DEFAULT_ERROR_MESSAGE = "For Espresso Tests, fully qualified test name should be limited to 255 until the bug is fixed: https://issuetracker.google.com/issues/114162875"
        const val TEST_ANNOTATION = "Test"
        const val TEST_ANNOTATION_QUALIFIED = "org.junit.$TEST_ANNOTATION"
        const val ESPRESSO_TEST = "EspressoTest"
        const val ESPRESSO_TEST_QUALIFIED = "uk.nhs.nhsx.covid19.android.app.testhelpers.base.$ESPRESSO_TEST"
    }
}
