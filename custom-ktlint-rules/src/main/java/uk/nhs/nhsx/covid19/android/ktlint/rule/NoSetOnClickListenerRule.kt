package uk.nhs.nhsx.covid19.android.ktlint.rule

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import uk.nhs.nhsx.covid19.android.ktlint.util.addImport
import uk.nhs.nhsx.covid19.android.ktlint.util.getFunctionCallName
import uk.nhs.nhsx.covid19.android.ktlint.util.getWrappingFunction
import uk.nhs.nhsx.covid19.android.ktlint.util.setFunctionCallName

/**
 * Clicking a button (or any view) multiple times in quick succession can trigger the views's action
 * repeatedly. To prevent this, we should always use setOnSingleClickListener rather than setOnClickListener.
 * setOnSingleClickListener ignores clicks that happen too soon after the first one.
 *
 * This ktlint rule that checks that setOnClickListener is not used anywhere except for in the
 * implementation of setOnSingleClickListener.
 */
class NoSetOnClickListenerRule : Rule(RULE_ID) {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (
            offset: Int, errorMessage: String, canBeAutoCorrected:
            Boolean
        ) -> Unit
    ) {
        if (node.elementType == KtNodeTypes.CALL_EXPRESSION) {
            val call = node.psi as KtCallExpression
            if (call.isInvalidUseOfSetOnClickListener()) {
                if (autoCorrect) {
                    emit(node.startOffset, DEFAULT_ERROR_MESSAGE, autoCorrect)
                    val success = correct(call)
                    if (!success) {
                        throw RuntimeException("Error applying correction for $RULE_ID")
                        // TODO ideally would like to not abort linting with an exception but rather revert
                        // changes and report as error that cannot be corrected, but I cannot find documentation
                        // on how to do this
                    }
                } else {
                    emit(node.startOffset, DEFAULT_ERROR_MESSAGE, autoCorrect)
                }
            }
        }
    }

    private fun KtCallExpression.isInvalidUseOfSetOnClickListener(): Boolean =
        getFunctionCallName() == SET_ON_CLICK_LISTENER
                && !parent.isSetOnSingleClickListenerFun()

    private fun PsiElement?.isSetOnSingleClickListenerFun(): Boolean =
        getWrappingFunction()?.name == SET_ON_SINGLE_CLICK_LISTENER

    private fun correct(call: KtCallExpression): Boolean =
        call.setFunctionCallName(SET_ON_SINGLE_CLICK_LISTENER)
        && call.containingKtFile.addImport(SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED)

    companion object {
        const val RULE_ID = "no-set-on-click-listener"

        const val SET_ON_CLICK_LISTENER = "setOnClickListener"
        const val SET_ON_SINGLE_CLICK_LISTENER = "setOnSingleClickListener"
        const val SET_ON_SINGLE_CLICK_LISTENER_QUALIFIED = "uk.nhs.nhsx.covid19.android.app.util.viewutils.$SET_ON_SINGLE_CLICK_LISTENER"

        const val DEFAULT_ERROR_MESSAGE = "Use $SET_ON_SINGLE_CLICK_LISTENER rather than $SET_ON_CLICK_LISTENER"
    }
}
