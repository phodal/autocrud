package cc.unitmesh.devti.language.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext
import java.io.File

class FileReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        const val REF_TYPE = "file"
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val basePath = project.guessProjectDir()?.path ?: return

        val editorHistoryManager = EditorHistoryManager.getInstance(project)
        val fileList: List<VirtualFile> = editorHistoryManager.fileList

        fileList.forEach {
            val removePrefix = it.path.removePrefix(basePath)
            val relativePath: String = removePrefix.removePrefix(File.separator)

            val element = LookupElementBuilder.create(relativePath)
                .withIcon(VirtualFilePresentation.getIcon(it))
                .withInsertHandler { context, _ ->
                    context.editor.caretModel.moveCaretRelatively(
                        1, 0, false, false, false
                    )
                }

            result.addElement(element)
        }
    }
}