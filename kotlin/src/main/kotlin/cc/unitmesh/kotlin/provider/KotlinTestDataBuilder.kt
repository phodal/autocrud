package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import cc.unitmesh.kotlin.context.KotlinClassContextBuilder
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

class KotlinTestDataBuilder : TestDataBuilder {
    override fun inboundData(element: PsiElement): Map<String, String> {
        if (element !is KtNamedFunction) return emptyMap()

        val result = mutableMapOf<String, String>()
        val parameters = element.valueParameters
        for (parameter in parameters) {
            result += handleFromType(parameter)
        }
        return result
    }

    private fun handleFromType(parameter: KtParameter): Map<@NlsSafe String, String> {
        when (val type = parameter.typeReference?.typeElement) {
            is KtClass -> processingClassType(type)
        }

        return emptyMap()
    }


    private fun processingClassType(type: KtClass): Map<@NlsSafe String, String> {
        val result = mutableMapOf<String, String>()
        val fqn = type.fqName?.asString() ?: return result

        KotlinClassContextBuilder().getClassContext(type, false)?.format()?.let {
            result += mapOf(fqn to it)
        }

        return result
    }

    override fun outboundData(element: PsiElement): Map<String, String> {
        if (element !is KtNamedFunction) return emptyMap()

        val returnType = element.getReturnTypeReference() ?: return emptyMap()

        val result = mutableMapOf<String, String>()

        result += processing(returnType)

        return result
    }

    private fun processing(returnType: KtTypeReference): Map<String, String> {
        val result = mutableMapOf<String, String>()
        when (val typeElement = returnType.typeElement) {
            is KtUserType -> {
                val referenceExpression = typeElement.referenceExpression?.resolveMainReference()
                if (referenceExpression is KtClass) {
                    result += processingClassType(referenceExpression)
                }
            }
        }

        return result
    }
}

fun KtReferenceExpression.resolveMainReference(): PsiElement? =
    try {
        mainReference.resolve()
    } catch (e: Exception) {
        if (e is ControlFlowException) throw e
        throw KotlinExceptionWithAttachments("Unable to resolve reference", e)
            .withPsiAttachment("reference.txt", this)
            .withPsiAttachment("file.kt", containingFile)
    }