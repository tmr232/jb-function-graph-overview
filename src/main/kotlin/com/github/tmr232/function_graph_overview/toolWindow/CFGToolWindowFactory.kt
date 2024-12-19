package com.github.tmr232.function_graph_overview.toolWindow

import com.github.tmr232.function_graph_overview.settings.Settings
import com.github.tmr232.function_graph_overview.settings.SettingsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefJSQuery.Response
import javax.swing.JButton
import javax.swing.JComponent

private fun internalLanguageName(language: String) =
    when (language) {
        // We need to choose either C or C++ here,
        // and our current implementation of C++ is a strict superset of C.
        "C/C++" -> "C++"
        // TypeScript is a superset of JavaScript
        "JavaScript" -> "TypeScript"
        "TypeScript JSX" -> "TSX"
        else -> language
    }

private fun registerCaretListener(
    project: Project,
    parentDisposable: Disposable,
    onPositionChanged: (editor: Editor?) -> Unit,
) {
    EditorFactory.getInstance().eventMulticaster.addCaretListener(
        object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                onPositionChanged(event.editor)
            }
        },
        parentDisposable,
    )

    project.messageBus.connect(parentDisposable).subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                val editor = event.manager.selectedTextEditor ?: return
                onPositionChanged(editor)
            }
        },
    )
}

class CFGToolWindowFactory :
    ToolWindowFactory,
    DumbAware,
    Disposable {
    companion object {
        private const val PLUGIN_TITLE = "Function Graph Overview"
    }

    private val localBrowser: LocalBrowser = LocalBrowser("/webview")

    private val navigateQuery = localBrowser.createJSQuery()

    init {

        navigateQuery.addHandler { onNavigate(it) }

        Disposer.register(this, localBrowser)
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            SettingsListener.TOPIC,
            object : SettingsListener {
                override fun settingsChanged() {
                    loadSettings()
                }
            },
        )
    }

    private fun onNavigate(position: String): Response {
        thisLogger().debug(position)
        val offset = position.toInt()
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            setCursorPosition(project, offset)
        }
        return Response(null)
    }

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        toolWindow.title = PLUGIN_TITLE
        toolWindow.stripeTitle = PLUGIN_TITLE
        toolWindow.setIcon(IconLoader.getIcon("/icons/view-icon.svg", javaClass))

        val contentManager = toolWindow.contentManager
        if (JBCefApp.isSupported()) {
            val webContent =
                contentManager.factory.createContent(
                    createWebViewContent(),
                    null,
                    false,
                )
            contentManager.addContent(webContent)
        }

        registerCaretListener(project, this) { updateCaretPosition(it) }
    }

    override fun dispose() {
    }

    /**
     * Initializes the webview callbacks into the plugin code
     */
    private fun initializeCallbacks() {
        localBrowser.injectFunction("navigateTo", "position", code = navigateQuery.inject("position"))
    }

    private fun setCode(
        code: String,
        cursorOffset: Int,
        language: String,
    ) {
        val cfgLanguage = internalLanguageName(language)
        loadSettings()
        localBrowser.call("setCode", jsStr(code), jsNum(cursorOffset), jsStr(cfgLanguage))
        initializeCallbacks()
    }

    private fun loadSettings() {
        localBrowser.call("setSimplify", jsBool(Settings.simplify))
        localBrowser.call("setFlatSwitch", jsBool(Settings.flatSwitch))
        localBrowser.call("setHighlight", jsBool(Settings.highlight))
        setColors(Settings.colorScheme)
    }

    private fun setColors(colors: String) {
        val colorScheme =
            when (colors) {
                "" -> return
                "dark" -> """{"version":1,"scheme":[{"name":"node.default","hex":"#707070"},{"name":"node.entry","hex":"#48AB30"},{"name":"node.exit","hex":"#AB3030"},{"name":"node.throw","hex":"#590c0c"},{"name":"node.yield","hex":"#0a9aca"},{"name":"node.border","hex":"#000000"},{"name":"node.highlight","hex":"#dddddd"},{"name":"edge.regular","hex":"#2592a1"},{"name":"edge.consequence","hex":"#4ce34c"},{"name":"edge.alternative","hex":"#ff3e3e"},{"name":"cluster.border","hex":"#302e2e"},{"name":"cluster.with","hex":"#7d007d"},{"name":"cluster.tryComplex","hex":"#344c74"},{"name":"cluster.try","hex":"#1b5f1b"},{"name":"cluster.finally","hex":"#999918"},{"name":"cluster.except","hex":"#590c0c"},{"name":"graph.background","hex":"#2B2D30"}]}"""
                "light" -> """{"version":1,"scheme":[{"name":"node.default","hex":"#d3d3d3"},{"name":"node.entry","hex":"#48AB30"},{"name":"node.exit","hex":"#AB3030"},{"name":"node.throw","hex":"#ffdddd"},{"name":"node.yield","hex":"#00bfff"},{"name":"node.border","hex":"#000000"},{"name":"node.highlight","hex":"#000000"},{"name":"edge.regular","hex":"#0000ff"},{"name":"edge.consequence","hex":"#008000"},{"name":"edge.alternative","hex":"#ff0000"},{"name":"cluster.border","hex":"#ffffff"},{"name":"cluster.with","hex":"#ffddff"},{"name":"cluster.tryComplex","hex":"#ddddff"},{"name":"cluster.try","hex":"#ddffdd"},{"name":"cluster.finally","hex":"#ffffdd"},{"name":"cluster.except","hex":"#ffdddd"},{"name":"graph.background","hex":"#F7F8FA"}]}"""
                else -> colors
            }
        localBrowser.call("setColors", jsStr(colorScheme))
    }

    private fun updateCaretPosition(editor: Editor?) {
        if (editor != null) {
            val caret = editor.caretModel.primaryCaret
            val document = editor.document

            val virtualFile = editor.virtualFile ?: return

            thisLogger().warn(
                "Language displayName = ${virtualFile.fileType.displayName}, name = ${virtualFile.fileType.name}",
            )

            setCode(document.text, caret.offset, virtualFile.fileType.displayName)
        }
    }

    private fun createButton(
        text: String,
        onClick: () -> Unit,
    ): JComponent = JButton(text).apply { addActionListener { onClick() } }

    private fun createWebViewContent(): JComponent {
//         TODO: Find a way to enable debugging and keep the scaling!
        return localBrowser.component
//        return JBPanel<JBPanel<*>>().apply {
//            add(createButton("Open Devtools") { localBrowser.openDevtools() })
//            add(localBrowser.component)
//        }
    }
}

fun setCursorPosition(
    project: Project,
    offset: Int,
) {
    // Get the current active editor
    val editor = FileEditorManager.getInstance(project).selectedTextEditor

    editor?.let {
        editor.contentComponent.requestFocusInWindow()
        // Get the caret model to modify cursor position
        val caretModel = it.caretModel

        ApplicationManager.getApplication().invokeLater {
            // Set the cursor position to the specified offset
            caretModel.moveToOffset(offset)

            // Make sure the cursor is visible
            it.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        }
    }
}
