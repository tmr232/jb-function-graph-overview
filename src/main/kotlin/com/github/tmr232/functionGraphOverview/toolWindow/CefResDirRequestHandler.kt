package com.github.tmr232.functionGraphOverview.toolWindow

import com.intellij.openapi.diagnostic.thisLogger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.net.URI

private typealias LookupResourceProvider = (String) -> CefResourceHandler?

class CefResDirRequestHandler(
    private val myProtocol: String,
    private val myAuthority: String,
    private val myResourceProvider: LookupResourceProvider,
) : CefRequestHandlerAdapter() {
    private val rejectingResourceHandler: CefResourceHandler =
        object : CefResourceHandlerAdapter() {
            override fun processRequest(
                request: CefRequest,
                callback: CefCallback,
            ): Boolean {
                callback.cancel()
                return false
            }
        }

    private val resourceRequestHandler =
        object : CefResourceRequestHandlerAdapter() {
            override fun getResourceHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest,
            ): CefResourceHandler {
                val url = URI(request.url).toURL()
                thisLogger().warn(String.format("url: %s, protocol: %s, authority: %s", url, url.protocol, url.authority))
                url.protocol
                if (!url.protocol.equals(myProtocol) || !url.authority.equals(myAuthority)) {
                    return rejectingResourceHandler
                }
                return try {
                    myResourceProvider(url.path) ?: rejectingResourceHandler
                } catch (e: RuntimeException) {
                    rejectingResourceHandler
                }
            }
        }

    override fun getResourceRequestHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?,
    ): CefResourceRequestHandler = resourceRequestHandler
}
