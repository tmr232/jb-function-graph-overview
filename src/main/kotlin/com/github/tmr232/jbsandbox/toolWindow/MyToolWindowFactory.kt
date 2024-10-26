package com.github.tmr232.jbsandbox.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBuilder
import org.cef.handler.CefRequestHandler
import java.io.File
import java.util.Base64
import javax.swing.JButton
import javax.swing.JComponent

private fun extensionToMime(extension: String) =
    when (extension) {
        "html" -> "text/html"
        "png" -> "image/png"
        "wasm" -> "application/wasm"
        "js" -> "text/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        else -> null
    }

private fun internalLanguageName(language: String) =
    when (language) {
        "C/C++" -> "C"
        else -> language
    }

private fun safeString(text: String): String {
    val base64text = Base64.getEncoder().encodeToString(text.toByteArray())
    return """(()=>{
            function base64ToBytes(base64) {
                const binString = atob(base64);
                return Uint8Array.from(binString, (m) => m.codePointAt(0));
            }
            return new TextDecoder().decode(base64ToBytes("$base64text"));
            })()"""
}

class MyToolWindowFactory :
    ToolWindowFactory,
    Disposable {
    companion object {
        private const val PLUGIN_TITLE = "Function Graph Overview"

        private const val HOST_NAME = "localhost"
        private const val PROTOCOL = "http"

        private const val VIEWER_PATH = "/index.html"

        private const val VIEWER_URL = "$PROTOCOL://$HOST_NAME$VIEWER_PATH"
    }

    private val ourCefClient = JBCefApp.getInstance().createClient()

    private fun isDebugMode() = true || RegistryManager.getInstance().`is`("ide.browser.jcef.svg-viewer.debug")

    private val myBrowser: JBCefBrowser =
        JBCefBrowserBuilder().setClient(ourCefClient).setEnableOpenDevToolsMenuItem(isDebugMode()).build()
    private val myRequestHandler: CefRequestHandler =
        CefResDirRequestHandler(PROTOCOL, HOST_NAME) { path: String ->
            javaClass.getResourceAsStream("/webview/$path")?.let { stream ->
                extensionToMime(File(path).extension)?.let { mimeType -> CefStreamResourceHandler(stream, mimeType, this) }
            }
        }

    init {
        ourCefClient.addRequestHandler(myRequestHandler, myBrowser.cefBrowser)

        myBrowser.loadURL(VIEWER_URL)

        Disposer.register(this, myBrowser)
        Disposer.register(this, ourCefClient)
    }

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        toolWindow.title = PLUGIN_TITLE
        toolWindow.stripeTitle = PLUGIN_TITLE
        toolWindow.setIcon(IconLoader.getIcon("/webview/favicon.png", javaClass))

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


        // Add listeners
        EditorFactory.getInstance().eventMulticaster.addCaretListener(createCaretListener(), this)

        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val editor = event.manager.selectedTextEditor ?: return
                    updateCaretPosition(editor)
                }
            },
        )
    }

    private fun createCaretListener(): CaretListener =
        object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                updateCaretPosition(event.editor)
            }

            // Implement other methods if needed
            override fun caretAdded(event: CaretEvent) {}

            override fun caretRemoved(event: CaretEvent) {}
        }

    override fun dispose() {
        ourCefClient.removeRequestHandler(myRequestHandler, myBrowser.cefBrowser)
    }

    private fun setCode(
        code: String,
        cursorOffset: Int,
        language: String,
    ) {
        val cfgLanguage = internalLanguageName(language)
        val jsToExecute = """setCode(${safeString(code)}, $cursorOffset, "$cfgLanguage");"""
        myBrowser.cefBrowser.executeJavaScript(jsToExecute, "", 0)
    }

    private fun setColors(colors: String) {
        val jsToExecute = """setColors(${safeString(colors)});"""
        myBrowser.cefBrowser.executeJavaScript(jsToExecute, "", 0)
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
        return myBrowser.component
//        return JBPanel<JBPanel<*>>().apply {
//            add(createButton("Open Devtools") { myBrowser.openDevtools() })
//            add(myBrowser.component)
//        }
    }
}
