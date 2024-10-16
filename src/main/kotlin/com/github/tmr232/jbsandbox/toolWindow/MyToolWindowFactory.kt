package com.github.tmr232.jbsandbox.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.tmr232.jbsandbox.MyBundle
import com.github.tmr232.jbsandbox.services.MyProjectService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JButton
import javax.swing.JComponent
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.util.ui.JBUI
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.awt.GridLayout


class MyToolWindowFactory : ToolWindowFactory, Disposable {
    private lateinit var filePathLabel: JBLabel
    private lateinit var cursorPositionLabel: JBLabel
    private lateinit var selectedTextLabel: JBLabel
    private lateinit var browser: JBCefBrowser

    companion object {
        private val CARET_LISTENER_KEY = Key<CaretListener>("FileInfoCaretListener")

        private const val NAME = "SvgViewer"

        private const val HOST_NAME = "localhost"
        private const val PROTOCOL = "http"

        private const val OVERLAY_SCROLLBARS_CSS_PATH = "/overlayscrollbars.css"
        private const val OVERLAY_SCROLLBARS_JS_PATH = "/overlayscrollbars.browser.es6.js"

        private const val VIEWER_PATH = "/index.html"
        private const val IMAGE_PATH = "/image"
        private const val SCROLLBARS_CSS_PATH = "/scrollbars.css"
        private const val CHESSBOARD_CSS_PATH = "/chessboard.css"
        private const val GRID_CSS_PATH = "/pixel_grid.css"

        private const val VIEWER_URL = "$PROTOCOL://$HOST_NAME$VIEWER_PATH"
        private const val IMAGE_URL = "$PROTOCOL://$HOST_NAME$IMAGE_PATH"
        private const val SCROLLBARS_STYLE_URL = "$PROTOCOL://$HOST_NAME$SCROLLBARS_CSS_PATH"
        private const val CHESSBOARD_STYLE_URL = "$PROTOCOL://$HOST_NAME$CHESSBOARD_CSS_PATH"
        private const val GRID_STYLE_URL = "$PROTOCOL://$HOST_NAME$GRID_CSS_PATH"

        private val ourCefClient = JBCefApp.getInstance().createClient()

        init {
            Disposer.register(ApplicationManager.getApplication(), ourCefClient)
        }

        @JvmStatic
        fun isDebugMode() = RegistryManager.getInstance().`is`("ide.browser.jcef.svg-viewer.debug")
    }

    private val myBrowser: JBCefBrowser =
        JBCefBrowserBuilder().setClient(ourCefClient).setEnableOpenDevToolsMenuItem(isDebugMode()).build()
    private val myRequestHandler: CefRequestHandler
    private val myLoadHandler: CefLoadHandler


    init {
//        myRequestHandler = CefLocalRequestHandler(PROTOCOL, HOST_NAME)
//        myRequestHandler.addResource(VIEWER_PATH) {
//            javaClass.getResourceAsStream("/webview/index.html")?.let {
//                CefStreamResourceHandler(it, "text/html", this)
//            }
//        }
        myRequestHandler = CefResDirRequestHandler(PROTOCOL, HOST_NAME) { path: String ->
            javaClass.getResourceAsStream("/webview/$path")?.let {
                val mimeType = when (path.split(".").last()) {
                    "html" -> "text/html"
                    "png" -> "image/png"
                    "wasm"->"application/wasm"
                    "js"->"text/javascript"
                    "css"->"text/css"
                    else -> null
                }
                if (mimeType == null) {
                    null
                } else {
                    CefStreamResourceHandler(it, mimeType, this)
                }
            }
        }
        ourCefClient.addRequestHandler(myRequestHandler, myBrowser.cefBrowser)

        myLoadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
//                if (frame.isMain) {
//                    reloadStyles()
//                    execute("sendInfo = function(info_text) {${myViewerStateJSQuery.inject("info_text")};}")
//                    execute("setImageUrl('$IMAGE_URL');")
//                    isGridVisible = myEditorState.isGridVisible
//                    isTransparencyChessboardVisible = myEditorState.isBackgroundVisible
//                    setBorderVisible(isBorderVisible())
//                }
            }
        }
        ourCefClient.addLoadHandler(myLoadHandler, myBrowser.cefBrowser)

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
//                createWebViewerPanel(),
                myBrowser.component,
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

    private fun createWebViewerPanel(): JComponent {

        val myRequestHandler = CefLocalRequestHandler("file", "")
        myRequestHandler.addResource("/pic.png") {
            javaClass.getResourceAsStream("/webview/pic.png")?.let {
                CefStreamResourceHandler(it, "image/png", this)
            }
        }

        val myHandler = object : CefRequestHandlerAdapter() {
            override fun getResourceRequestHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?,
                isNavigation: Boolean,
                isDownload: Boolean,
                requestInitiator: String?,
                disableDefaultHandling: BoolRef?
            ): CefResourceRequestHandler {
                return super.getResourceRequestHandler(
                    browser,
                    frame,
                    request,
                    isNavigation,
                    isDownload,
                    requestInitiator,
                    disableDefaultHandling
                )
            }
        }
//        val browser = JBCefBrowser("http://localhost/index.html")
////        val browser = JBCefBrowser("https://tmr232.github.io/function-graph-overview/")
//        browser.jbCefClient.cefClient.addRequestHandler(myRequestHandler)
        val client = JBCefApp.getInstance().createClient()
        client.cefClient.addRequestHandler(myRequestHandler)

//        client.cefClient.addRequestHandler(MyRequestHandler())
        browser = JBCefBrowserBuilder().setClient(client).setEnableOpenDevToolsMenuItem(true).setUrl("XXXX").build()
//        browser.jbCefClient.cefClient.addRequestHandler(MyRequestHandler())
//        thisLogger().warn(javaClass.classLoader.getResource("webview/index.html")?.toString())
//        thisLogger().warn(javaClass.getResource("/webview/index.html")?.toString())
//        val url = javaClass.getResource("/webview/index.html")?.toExternalForm()
//        url?.let{browser.loadURL(url)}
//        val html = javaClass.getResourceAsStream("/webview/index.html")?.readBytes()
//        html?.let{browser.loadHTML(html.toString(Charsets.UTF_8))}

        return browser.component
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

        panel.add(filePathLabel)
        panel.add(cursorPositionLabel)
        panel.add(selectedTextLabel)

        // Initial update
        updateFileInfo(FileEditorManager.getInstance(project).selectedTextEditor)

        return panel
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

            browser.openDevtools()
            myBrowser.openDevtools()

        } else {
            filePathLabel.text = "File: No file open"
            cursorPositionLabel.text = "Cursor: N/A"
            selectedTextLabel.text = "Selected: N/A"
        }
    }

    override fun shouldBeAvailable(project: Project) = true

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
