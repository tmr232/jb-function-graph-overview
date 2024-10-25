package com.github.tmr232.jbsandbox.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBuilder
import org.cef.handler.CefRequestHandler
import java.io.File
import java.util.*
import javax.swing.JButton
import javax.swing.JComponent


private const val PLUGIN_TITLE = "Function Graph Overview"


class MyToolWindowFactory : ToolWindowFactory, Disposable {
    companion object {
        private const val DEFAULT_DARK_COLORS =
            "{\"version\":1,\"scheme\":[{\"name\":\"node.default\",\"hex\":\"#707070\"},{\"name\":\"node.entry\",\"hex\":\"#48AB30\"},{\"name\":\"node.exit\",\"hex\":\"#AB3030\"},{\"name\":\"node.throw\",\"hex\":\"#590c0c\"},{\"name\":\"node.yield\",\"hex\":\"#0a9aca\"},{\"name\":\"node.border\",\"hex\":\"#000000\"},{\"name\":\"node.highlight\",\"hex\":\"#dddddd\"},{\"name\":\"edge.regular\",\"hex\":\"#2592a1\"},{\"name\":\"edge.consequence\",\"hex\":\"#4ce34c\"},{\"name\":\"edge.alternative\",\"hex\":\"#ff3e3e\"},{\"name\":\"cluster.border\",\"hex\":\"#302e2e\"},{\"name\":\"cluster.with\",\"hex\":\"#7d007d\"},{\"name\":\"cluster.tryComplex\",\"hex\":\"#344c74\"},{\"name\":\"cluster.try\",\"hex\":\"#1b5f1b\"},{\"name\":\"cluster.finally\",\"hex\":\"#999918\"},{\"name\":\"cluster.except\",\"hex\":\"#590c0c\"},{\"name\":\"graph.background\",\"hex\":\"#2B2D30\"}]}"


        private const val HOST_NAME = "localhost"
        private const val PROTOCOL = "http"


        private const val VIEWER_PATH = "/index.html"

        private const val VIEWER_URL = "$PROTOCOL://$HOST_NAME$VIEWER_PATH"

        init {
            Disposer.register(ApplicationManager.getApplication(), ourCefClient)
        }

        private fun extensionToMime(extension: String) = when (extension) {
            "html" -> "text/html"
            "png" -> "image/png"
            "wasm" -> "application/wasm"
            "js" -> "text/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            else -> null
        }

    }

    private val myBrowser: JBCefBrowser =
        JBCefBrowserBuilder().setClient(ourCefClient).setEnableOpenDevToolsMenuItem(isDebugMode()).build()
    private val myRequestHandler: CefRequestHandler = CefResDirRequestHandler(PROTOCOL, HOST_NAME) { path: String ->
        javaClass.getResourceAsStream("/webview/$path")?.let {
            val mimeType = extensionToMime(File(path).extension)
            if (mimeType == null) {
                null
            } else {
                CefStreamResourceHandler(it, mimeType, this)
            }
        }
    }


    init {
        ourCefClient.addRequestHandler(myRequestHandler, myBrowser.cefBrowser)

        myBrowser.loadURL(VIEWER_URL)

        Disposer.register(this, myBrowser)
    }


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.title = PLUGIN_TITLE
        toolWindow.stripeTitle = PLUGIN_TITLE
        toolWindow.setIcon(IconLoader.getIcon("/webview/favicon.png", javaClass))

        val contentManager = toolWindow.contentManager
        if (JBCefApp.isSupported()) {
            val webContent = contentManager.factory.createContent(
//                myBrowser.component,
                createWebViewContent(),
                null,
                false
            )
            contentManager.addContent(webContent)
        }


        // Add listeners
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val editor = event.manager.selectedTextEditor ?: return
                    updateFileInfo(editor)


                    // Add caret listener to the editor
                    addCaretListenerIfNeeded(editor);
                }
            }
        )
    }

    private fun createCaretListener(): CaretListener {
        return object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                updateFileInfo(event.editor)
            }

            // Implement other methods if needed
            override fun caretAdded(event: CaretEvent) {}
            override fun caretRemoved(event: CaretEvent) {}
        }
    }

    private fun addCaretListenerIfNeeded(editor: Editor) {
        if (editor.getUserData(CARET_LISTENER_KEY) == null) {
            val caretListener = createCaretListener()
            editor.caretModel.addCaretListener(caretListener)
            editor.putUserData(CARET_LISTENER_KEY, caretListener)
        }
    }


    override fun dispose() {
//        ourCefClient.removeRequestHandler(myRequestHandler, myBrowser.cefBrowser)
//        ourCefClient.removeLoadHandler(myLoadHandler, myBrowser.cefBrowser)
//        myDocument.removeDocumentListener(this)
        ourCefClient.removeRequestHandler(myRequestHandler, myBrowser.cefBrowser)
//    ourCefClient.removeLoadHandler(myLoadHandler, myBrowser.cefBrowser)
    }



    private fun setCode(code: String, cursorOffset: Int, language: String) {
        //TODO: It is likely that some languages won't match up with the TS code,
        //      in which case we'll have to map them from the JB name to the TS name.
        val base64code = Base64.getEncoder().encodeToString(code.toByteArray())
        val jsToExecute = """
            (()=>{
        function base64ToBytes(base64) {
  const binString = atob(base64);
  return Uint8Array.from(binString, (m) => m.codePointAt(0));
}

const code = new TextDecoder().decode(base64ToBytes("$base64code"));
setCode(code, $cursorOffset, "$language");})();
        """;
        myBrowser.cefBrowser.executeJavaScript(jsToExecute, "", 0);
    }

    private fun setColors(colors: String) {
        val jsToExecute = """
            setColors(`$colors`);
        """
        myBrowser.cefBrowser.executeJavaScript(jsToExecute, "", 0);
    }

    private fun updateFileInfo(editor: Editor?) {
        if (editor != null) {
            val caret = editor.caretModel.primaryCaret
            val document = editor.document




            thisLogger().warn("Language displayName = ${editor.virtualFile.fileType.displayName}, name = ${editor.virtualFile.fileType.name}")
            setColors(DEFAULT_DARK_COLORS)

            setCode(document.text, caret.offset, editor.virtualFile.fileType.displayName)


        }
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun createButton(text: String, onClick: () -> Unit): JComponent {
        return JButton(text).apply { addActionListener { onClick() } }
    }

    private fun createWebViewContent(): JComponent {
//         TODO: Find a way to enable debugging and keep the scaling!
        return JBPanel<JBPanel<*>>().apply {
            add(createButton("Open Devtools") { myBrowser.openDevtools() })
            add(myBrowser.component)
        }
//        return myBrowser.component
    }


}

private val CARET_LISTENER_KEY = Key<CaretListener>("FileInfoCaretListener")
private val ourCefClient = JBCefApp.getInstance().createClient()
fun isDebugMode() = true || RegistryManager.getInstance().`is`("ide.browser.jcef.svg-viewer.debug")