package com.github.tmr232.function_graph_overview.toolWindow

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.misc.BoolRef
import org.cef.network.CefRequest

class MyRequestHandler: CefRequestHandlerAdapter() {
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