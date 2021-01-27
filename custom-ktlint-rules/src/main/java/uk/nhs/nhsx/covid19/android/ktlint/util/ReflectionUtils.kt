package uk.nhs.nhsx.covid19.android.ktlint.util

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

fun KtCallExpression.getFunctionCallName(): String? =
    (firstChild as? KtNameReferenceExpression)?.getIdentifier()?.text

fun KtCallExpression.setFunctionCallName(functionName: String): Boolean =
    ((firstChild as? KtNameReferenceExpression)?.getIdentifier() as? LeafElement)?.let { leaf ->
        leaf.replaceWithText(functionName)
        true
    } ?: false

fun PsiElement?.getWrappingFunction(): KtFunction? =
    this?.let { element ->
        if (element is KtFunction) {
            element
        } else {
            element.parent.getWrappingFunction()
        }
    }

fun KtFile.hasImport(import: String): Boolean =
    importList?.let { importList ->
        val importFqName = FqName(import)
        importList.imports.any { it.importedFqName == importFqName }
    } ?: false

fun KtFile.addImport(import: String): Boolean {
    if (hasImport(import)) {
        return true
    }
    return importList?.let { importList ->
        val psiElementFactory = KtPsiFactory(project)
        val importDirective = psiElementFactory.createImportDirective(
            ImportPath(
                FqName(import),
                false
            )
        )
        importList.add(psiElementFactory.createNewLine())
        importList.add(importDirective)
        true
    } ?: false
}
