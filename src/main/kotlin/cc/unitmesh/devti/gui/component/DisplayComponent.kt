package cc.unitmesh.devti.gui.component

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import javax.accessibility.AccessibleContext
import javax.swing.JEditorPane
import org.apache.commons.text.StringEscapeUtils

class DisplayComponent(question: String) : JEditorPane() {
    init {
        this.contentType = "text/plain;charset=UTF-8"
        this.putClientProperty(HONOR_DISPLAY_PROPERTIES, true)
        this.font = UIUtil.getMenuFont()
        this.isEditable = false
        this.border = JBEmptyBorder(8)
        this.text = question
        this.isOpaque = false
        this.putClientProperty(
            AccessibleContext.ACCESSIBLE_NAME_PROPERTY, stripHtmlAndUnescapeXmlEntities(question)
        )

        if (this.caret != null) {
            this.caretPosition = 0
        }
    }

    fun updateMessage(content: String) {
        this.text = content
    }

    private fun stripHtmlAndUnescapeXmlEntities(input: String): String {
        // 使用 Jsoup 去除HTML标签
        val text = Jsoup.parse(input).text()
        // 使用 Apache Commons Text 解码XML实体
        return StringEscapeUtils.unescapeXml(text)
    }
}