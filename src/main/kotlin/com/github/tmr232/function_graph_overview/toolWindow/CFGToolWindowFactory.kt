package com.github.tmr232.function_graph_overview.toolWindow

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
import com.intellij.ui.components.JBPanel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefJSQuery.Response
import javax.swing.JButton
import javax.swing.JComponent

private fun internalLanguageName(language: String) =
    when (language) {
        // We need to choose either C or C++ here,
        // and our current implementation of C++ is a strict superset of C.
        "C/C++" -> "C++"
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
        localBrowser.call("setCode", jsStr(code), jsNum(cursorOffset), jsStr(cfgLanguage))
        initializeCallbacks()
    }

    private fun setColors(colors: String) {
        localBrowser.call("setColors", jsStr(colors))
    }

    private fun updateCaretPosition(editor: Editor?) {
        if (editor != null) {
            val caret = editor.caretModel.primaryCaret
            val document = editor.document

            thisLogger().warn(
                "Language displayName = ${editor.virtualFile.fileType.displayName}, name = ${editor.virtualFile.fileType.name}",
            )

            setCode(document.text, caret.offset, editor.virtualFile.fileType.displayName)
        }
    }

    private fun createButton(
        text: String,
        onClick: () -> Unit,
    ): JComponent = JButton(text).apply { addActionListener { onClick() } }

    private fun createWebViewContent(): JComponent {
//         TODO: Find a way to enable debugging and keep the scaling!
//        return myBrowser.component
        return JBPanel<JBPanel<*>>().apply {
            add(createButton("Open Devtools") { localBrowser.openDevtools() })
            add(localBrowser.component)
        }
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
