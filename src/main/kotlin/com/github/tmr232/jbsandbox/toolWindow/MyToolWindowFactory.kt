package com.github.tmr232.jbsandbox.toolWindow

import com.github.tmr232.jbsandbox.MyBundle
import com.github.tmr232.jbsandbox.services.MyProjectService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import org.cef.handler.*
import java.awt.GridLayout
import java.util.*
import javax.swing.JButton
import javax.swing.JComponent


class MyToolWindowFactory : ToolWindowFactory, Disposable {
    private lateinit var filePathLabel: JBLabel
    private lateinit var cursorPositionLabel: JBLabel
    private lateinit var selectedTextLabel: JBLabel
    private lateinit var devtoolsButton: JButton



    companion object {
        private const val DEFAULT_DARK_COLORS = "{\"version\":1,\"scheme\":[{\"name\":\"node.default\",\"hex\":\"#707070\"},{\"name\":\"node.entry\",\"hex\":\"#48AB30\"},{\"name\":\"node.exit\",\"hex\":\"#AB3030\"},{\"name\":\"node.throw\",\"hex\":\"#590c0c\"},{\"name\":\"node.yield\",\"hex\":\"#0a9aca\"},{\"name\":\"node.border\",\"hex\":\"#000000\"},{\"name\":\"node.highlight\",\"hex\":\"#dddddd\"},{\"name\":\"edge.regular\",\"hex\":\"#2592a1\"},{\"name\":\"edge.consequence\",\"hex\":\"#4ce34c\"},{\"name\":\"edge.alternative\",\"hex\":\"#ff3e3e\"},{\"name\":\"cluster.border\",\"hex\":\"#302e2e\"},{\"name\":\"cluster.with\",\"hex\":\"#7d007d\"},{\"name\":\"cluster.tryComplex\",\"hex\":\"#344c74\"},{\"name\":\"cluster.try\",\"hex\":\"#1b5f1b\"},{\"name\":\"cluster.finally\",\"hex\":\"#999918\"},{\"name\":\"cluster.except\",\"hex\":\"#590c0c\"},{\"name\":\"graph.background\",\"hex\":\"#2B2D30\"}]}"
        private val CARET_LISTENER_KEY = Key<CaretListener>("FileInfoCaretListener")


        private const val HOST_NAME = "localhost"
        private const val PROTOCOL = "http"


        private const val VIEWER_PATH = "/index.html"

        private const val VIEWER_URL = "$PROTOCOL://$HOST_NAME$VIEWER_PATH"

        private val ourCefClient = JBCefApp.getInstance().createClient()

        init {
            Disposer.register(ApplicationManager.getApplication(), ourCefClient)
        }

        @JvmStatic
        fun isDebugMode() = true || RegistryManager.getInstance().`is`("ide.browser.jcef.svg-viewer.debug")
    }

    private val myBrowser: JBCefBrowser =
        JBCefBrowserBuilder().setClient(ourCefClient).setEnableOpenDevToolsMenuItem(isDebugMode()).build()
    private val myRequestHandler: CefRequestHandler = CefResDirRequestHandler(PROTOCOL, HOST_NAME) { path: String ->
        javaClass.getResourceAsStream("/webview/$path")?.let {
            val mimeType = when (path.split(".").last()) {
                "html" -> "text/html"
                "png" -> "image/png"
                "wasm" -> "application/wasm"
                "js" -> "text/javascript"
                "css" -> "text/css"
                else -> null
            }
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
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
        val contentManager = toolWindow.contentManager
        if (JBCefApp.isSupported()) {
            val webContent = contentManager.factory.createContent(
//                myBrowser.component,
                createWebViewContent(),
                "Web Viewer",
                false
            )
            contentManager.addContent(webContent)
        }
        val fileInfoContent = contentManager.factory.createContent(
            createFileInfoPanel(project),
            "File Info",
            false
        )
        contentManager.addContent(fileInfoContent)

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


    private fun createFileInfoPanel(project: Project): JComponent {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = GridLayout(3, 1)
        panel.border = JBUI.Borders.empty(10)

        filePathLabel = JBLabel("File: ")
        cursorPositionLabel = JBLabel("Cursor: ")
        selectedTextLabel = JBLabel("Selected: ")
        devtoolsButton = JButton("Open Devtools")
        devtoolsButton.addActionListener {
            thisLogger().warn("Opening devtools!")
            myBrowser.openDevtools()
        }

        panel.add(devtoolsButton)
        panel.add(filePathLabel)
        panel.add(cursorPositionLabel)
        panel.add(selectedTextLabel)

        // Initial update
        updateFileInfo(FileEditorManager.getInstance(project).selectedTextEditor)

        return panel
    }

    private fun setCode(code: String, cursorOffset: Int, language:String) {
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

    private fun setColors(colors:String) {
        val jsToExecute = """
            setColors(`$colors`);
        """
        myBrowser.cefBrowser.executeJavaScript(jsToExecute, "", 0);
    }

    private fun updateFileInfo(editor: Editor?) {
        if (editor != null) {
            val file = editor.virtualFile
            val caret = editor.caretModel.primaryCaret
            val document = editor.document

            filePathLabel.text = "File: ${file?.path ?: "Unknown"}"

            val line = document.getLineNumber(caret.offset) + 1
            val column = caret.offset - document.getLineStartOffset(line - 1) + 1
            cursorPositionLabel.text = "Cursor: Line $line, Column $column"

            val selectedText = editor.selectionModel.selectedText
            selectedTextLabel.text = "Selected: ${selectedText?.take(20) ?: "No selection"}"

            thisLogger().warn("Language displayName = ${editor.virtualFile.fileType.displayName}, name = ${editor.virtualFile.fileType.name}")
            setColors(DEFAULT_DARK_COLORS)

            setCode(document.text, caret.offset, editor.virtualFile.fileType.displayName)


        } else {
            filePathLabel.text = "File: No file open"
            cursorPositionLabel.text = "Cursor: N/A"
            selectedTextLabel.text = "Selected: N/A"
        }
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun createButton(text: String, onClick: () -> Unit): JComponent {
        return JButton(text).apply { addActionListener { onClick() } }
    }

    private fun createWebViewContent(): JComponent =
        JBPanel<JBPanel<*>>().apply {
            add(createButton("Open Devtools") { myBrowser.openDevtools() })
            add(myBrowser.component)
        }


    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
