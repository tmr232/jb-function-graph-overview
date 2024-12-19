package com.github.tmr232.function_graph_overview.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.handler.CefRequestHandler
import java.io.File
import java.util.Base64

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

private fun safeString(text: String): String {
    val base64text = Base64.getEncoder().encodeToString(text.toByteArray())
    return """(()=>{
            function __kotlin_base64ToBytes(base64) {
                const binString = atob(base64);
                return Uint8Array.from(binString, (m) => m.codePointAt(0));
            }
            return new TextDecoder().decode(__kotlin_base64ToBytes("$base64text"));
            })()"""
}

private fun formatJSCall(
    name: String,
    vararg args: JSArg,
): String {
    val formattedArgs = args.joinToString { it.asJSArg() }
    return "$name($formattedArgs);"
}

interface JSArg {
    fun asJSArg(): String
}

fun jsStr(value: String): JSArg =
    object : JSArg {
        override fun asJSArg(): String = safeString(value)
    }

fun jsNum(value: Number): JSArg =
    object : JSArg {
        override fun asJSArg(): String = value.toString()
    }

fun jsBool(value:Boolean):JSArg =
    object: JSArg {
        override fun asJSArg(): String =
            value.toString()

    }

class LocalBrowser(
    val resourcePath: String,
) : Disposable {
    companion object {
        private const val HOST_NAME = "localhost"
        private const val PROTOCOL = "http"

        private const val VIEWER_PATH = "/index.html"

        private const val VIEWER_URL = "$PROTOCOL://$HOST_NAME$VIEWER_PATH"
    }

    private val myCefClient = JBCefApp.getInstance().createClient()
    private val myBrowser: JBCefBrowser =
        JBCefBrowserBuilder().setClient(myCefClient).setEnableOpenDevToolsMenuItem(Disposer.isDebugMode()).build()
    private val myRequestHandler: CefRequestHandler =
        CefResDirRequestHandler(PROTOCOL, HOST_NAME) { path: String ->
            javaClass.getResourceAsStream("$resourcePath/$path")?.let { stream ->
                extensionToMime(File(path).extension)?.let { mimeType ->
                    CefStreamResourceHandler(
                        stream,
                        mimeType,
                        this,
                    )
                }
            }
        }

    init {
        myCefClient.addRequestHandler(myRequestHandler, myBrowser.cefBrowser)
        myBrowser.loadURL(VIEWER_URL)

        Disposer.register(this, myBrowser)
        Disposer.register(this, myCefClient)
    }

    override fun dispose() {
        myCefClient.removeRequestHandler(myRequestHandler, myBrowser.cefBrowser)
    }

    fun call(
        name: String,
        vararg args: JSArg,
    ) {
        myBrowser.cefBrowser.executeJavaScript(formatJSCall(name, *args), "", 0)
    }

    fun injectFunction(
        name: String,
        vararg args: String,
        code: String,
    ) {
        myBrowser.cefBrowser.executeJavaScript(
            "window.$name = (${args.joinToString()}) => {$code};",
            myBrowser.cefBrowser.url,
            0,
        )
    }

    fun createJSQuery() = JBCefJSQuery.create(myBrowser as JBCefBrowserBase)

    val component get() = myBrowser.component

    fun openDevtools() = myBrowser.openDevtools()
}
