package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.LanguageChangedCallback.jBLabel
import com.intellij.ide.actions.RevealFileAction
import com.intellij.idea.LoggerFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class LLMSettingComponent(private val settings: AutoDevSettingsState) {
    // 以下 LLMParam 变量不要改名，因为这些变量名会被用作配置文件的 key
    private val languageParam by LLMParam.creating({ LanguageChangedCallback.language = it}) {
        ComboBox(settings.language, HUMAN_LANGUAGES.values().map { it.display }) }
    private val aiEngineParam by LLMParam.creating(onChange = { onSelectedEngineChanged() }) {
        ComboBox(settings.aiEngine, AIEngines.values().toList().map { it.name })
    }
    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }
    private val openAIModelsParam by LLMParam.creating { ComboBox(settings.openAiModel, OPENAI_MODEL.toList()) }
    private val openAIKeyParam by LLMParam.creating { Password(settings.openAiKey) }
    private val customModelParam: LLMParam by LLMParam.creating { Editable(settings.customModel) }
    private val customOpenAIHostParam: LLMParam by LLMParam.creating { Editable(settings.customOpenAiHost) }

    private val gitTypeParam: LLMParam by LLMParam.creating { ComboBox(settings.gitType, GIT_TYPE.toList()) }
    private val gitLabUrlParam: LLMParam by LLMParam.creating { Editable(settings.gitlabUrl) }
    private val gitLabTokenParam: LLMParam by LLMParam.creating { Password(settings.gitlabToken) }

    private val gitHubTokenParam by LLMParam.creating { Password(settings.githubToken) }
    private val customEngineServerParam by LLMParam.creating { Editable(settings.customEngineServer) }
    private val customEngineTokenParam by LLMParam.creating { Password(settings.customEngineToken) }

    private val customEngineResponseTypeParam by LLMParam.creating { ComboBox(settings.customEngineResponseType, ResponseType.values().map { it.name }.toList()) }
    private val customEngineResponseFormatParam by LLMParam.creating { Editable(settings.customEngineResponseFormat) }
    private val customEngineRequestBodyFormatParam by LLMParam.creating { Editable(settings.customEngineRequestFormat) }


    val project = ProjectManager.getInstance().openProjects.firstOrNull()
    private val customEnginePrompt: EditorTextField by lazy {
        JsonLanguageField(
            project,
            settings.customPrompts,
            AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.prompt.placeholder"),
            CUSTOM_AGENT_FILE_NAME
        ).apply { LanguageChangedCallback.placeholder("autodev.custom.prompt.placeholder", this, 1) }
    }

    private val llmGroups = mapOf<AIEngines, List<LLMParam>>(
            AIEngines.Azure to listOf(
                    openAIModelsParam,
                    openAIKeyParam,
                    customOpenAIHostParam,
            ),
            AIEngines.OpenAI to listOf(
                    openAIModelsParam,
                    openAIKeyParam,
                    customModelParam,
                    customOpenAIHostParam,
            ),
            AIEngines.Custom to listOf(
                    customEngineResponseTypeParam,
                    customEngineServerParam,
                    customEngineTokenParam,
                    customEngineResponseFormatParam,
                    customEngineRequestBodyFormatParam,
            ),
    )


    private val onSelectedEngineChanged: () -> Unit = {
        applySettings(settings, updateParams = false)
    }
    private val _currentSelectedEngine: AIEngines
        get() = AIEngines.values().firstOrNull { it.name.lowercase() == aiEngineParam.value.lowercase() } ?: AIEngines.OpenAI

    private val currentLLMParams: List<LLMParam>
        get() {
            return llmGroups[_currentSelectedEngine]
                    ?: throw IllegalStateException("Unknown engine: ${aiEngineParam.value}")
        }

    private fun FormBuilder.addLLMParams(llmParams: List<LLMParam>): FormBuilder = apply {
        llmParams.forEach { addLLMParam(it) }
    }

    private fun FormBuilder.addLLMParam(llmParam: LLMParam): FormBuilder = apply {
        llmParam.addToFormBuilder(this)
    }

    private fun LLMParam.addToFormBuilder(formBuilder: FormBuilder) {
        when (this.type) {
            LLMParam.ParamType.Password -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactivePasswordField(this) {
                    this.text = it.value
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.Text -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveTextField(this) {
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.ComboBox -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveComboBox(this), 1, false)
            }

            else -> {
                formBuilder.addSeparator()
            }
        }
    }

    private val formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    val panel: JPanel get() = formBuilder.panel


    fun applySettings(settings: AutoDevSettingsState, updateParams: Boolean = false) {

        if (updateParams && engineChanged(settings).also { updateParams(settings) }) {
            return
        }
        panel.removeAll()

        formBuilder
                .addLLMParam(languageParam)
                .addSeparator()
                .addTooltip("For Custom LLM, config Custom Engine Server & Custom Engine Token & Custom Response Format")
                .addLLMParam(aiEngineParam)
                .addLLMParam(maxTokenLengthParam)
                .addLLMParam(delaySecondsParam)
            .addSeparator()
                .addTooltip("Select Git Type")
            .addLLMParam(gitTypeParam)
            .addTooltip("GitHub Token is for AutoCRUD Model")
                .addLLMParam(gitHubTokenParam)
            .addTooltip("GitLab options is for AutoCRUD Model")
            .addLLMParam(gitLabUrlParam)
            .addLLMParam(gitLabTokenParam)
                .addSeparator()
                .addComponent(panel {
                    row {
                        comment("For OpenAI LLM, config OpenAI Key & OpenAI Model & Custom OpenAI Host <a>Open Log for Debug</a>") {
                            RevealFileAction.openFile(LoggerFactory.getLogFilePath())
                        }
                    }
                })
                .addLLMParams(currentLLMParams)
                .addComponent(panel {
                    if (project != null) {
                        testLLMConnection(project)
                    }
                })
                .addVerticalGap(2)
                .addSeparator()
                .addLabeledComponent(jBLabel("settings.autodev.coder.customEnginePrompt", 1), customEnginePrompt, 1, true)
                .addComponentFillVertically(JPanel(), 0)
                .panel

        panel.invalidate()
        panel.repaint()
    }

    private fun updateParams(settings: AutoDevSettingsState) {
        settings.apply {
            maxTokenLengthParam.value = maxTokenLength
            gitTypeParam.value = gitType
            gitHubTokenParam.value = githubToken
            gitLabTokenParam.value = gitlabToken
            gitLabUrlParam.value = gitlabUrl
            openAIKeyParam.value = openAiKey
            customModelParam.value = customModel
            customOpenAIHostParam.value = customOpenAiHost
            customEngineServerParam.value = customEngineServer
            customEngineResponseTypeParam.value = customEngineResponseType
            customEngineTokenParam.value = customEngineToken
            openAIModelsParam.value = openAiModel
            languageParam.value = language
            aiEngineParam.value = aiEngine
            customEnginePrompt.text = customPrompts
            customEngineResponseFormatParam.value = customEngineResponseFormat
            customEngineRequestBodyFormatParam.value = customEngineRequestFormat
            delaySecondsParam.value = delaySeconds
        }
    }

    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            gitType = gitTypeParam.value
            githubToken = gitHubTokenParam.value
            gitlabUrl = gitLabUrlParam.value
            gitlabToken = gitLabTokenParam.value
            openAiKey = openAIKeyParam.value
            customModel = customModelParam.value
            customOpenAiHost = customOpenAIHostParam.value
            aiEngine = aiEngineParam.value
            language = languageParam.value
            customEngineServer = customEngineServerParam.value
            customEngineResponseType = customEngineResponseTypeParam.value
            customEngineToken = customEngineTokenParam.value
            customPrompts = customEnginePrompt.text
            openAiModel = openAIModelsParam.value
            customEngineResponseFormat = customEngineResponseFormatParam.value
            customEngineRequestFormat = customEngineRequestBodyFormatParam.value
            delaySeconds = delaySecondsParam.value
        }
    }

    fun isModified(settings: AutoDevSettingsState): Boolean {
        return settings.maxTokenLength != maxTokenLengthParam.value ||
                settings.gitType != gitTypeParam.value ||
                settings.githubToken != gitHubTokenParam.value ||
                settings.gitlabUrl != gitLabUrlParam.value ||
                settings.gitlabToken != gitLabTokenParam.value ||
                settings.openAiKey != openAIKeyParam.value ||
                settings.customModel != customModelParam.value ||
                settings.aiEngine != aiEngineParam.value ||
                settings.language != languageParam.value ||
                settings.customEngineServer != customEngineServerParam.value ||
                settings.customEngineResponseType != customEngineResponseTypeParam.value ||
                settings.customEngineToken != customEngineTokenParam.value ||
                settings.customPrompts != customEnginePrompt.text ||
                settings.openAiModel != openAIModelsParam.value ||
                settings.customOpenAiHost != customOpenAIHostParam.value ||
                settings.customEngineResponseFormat != customEngineResponseFormatParam.value ||
                settings.customEngineRequestFormat != customEngineRequestBodyFormatParam.value ||
                settings.delaySeconds != delaySecondsParam.value
    }

    private fun engineChanged(settings: AutoDevSettingsState): Boolean {
        return settings.aiEngine != aiEngineParam.value
    }

    init {
        applySettings(settings)
        LanguageChangedCallback.language = AutoDevSettingsState.getInstance().language
    }
}
