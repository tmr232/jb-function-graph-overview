package com.github.tmr232.jbsandbox.toolWindow

import com.intellij.openapi.diagnostic.thisLogger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.net.URL

private typealias LookupResourceProvider = (String) -> CefResourceHandler?

class CefResDirRequestHandler(
    private val myProtocol: String,
    private val myAuthority: String,
    private val myResourceProvider: LookupResourceProvider
) : CefRequestHandlerAdapter() {

    private val REJECTING_RESOURCE_HANDLER: CefResourceHandler = object : CefResourceHandlerAdapter() {
        override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
            callback.cancel()
            return false
        }
    }

    private val RESOURCE_REQUEST_HANDLER = object : CefResourceRequestHandlerAdapter() {
        override fun getResourceHandler(browser: CefBrowser?, frame: CefFrame?, request: CefRequest): CefResourceHandler {
            val url = URL(request.url)
            thisLogger().warn(String.format("url: %s, protocol: %s, authority: %s", url, url.protocol, url.authority))
            url.protocol
            if (!url.protocol.equals(myProtocol) || !url.authority.equals(myAuthority)) {
                return REJECTING_RESOURCE_HANDLER
            }
            return try {
                myResourceProvider(url.path) ?: REJECTING_RESOURCE_HANDLER
            } catch (e: RuntimeException) {
                REJECTING_RESOURCE_HANDLER
            }
        }
    }



    override fun getResourceRequestHandler(browser: CefBrowser?,
                                           frame: CefFrame?,
                                           request: CefRequest?,
                                           isNavigation: Boolean,
                                           isDownload: Boolean,
                                           requestInitiator: String?,
                                           disableDefaultHandling: BoolRef?): CefResourceRequestHandler {
        return RESOURCE_REQUEST_HANDLER
    }
}