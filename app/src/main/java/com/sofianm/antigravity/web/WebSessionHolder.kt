package com.sofianm.antigravity.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.sofianm.antigravity.data.OAuthDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PaneSlot {
    PRIMARY,
    SECONDARY
}

data class WebPaneState(
    val slot: PaneSlot,
    val profileId: String? = null,
    val currentUrl: String = "",
    val pageTitle: String = "Antigravity",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val errorMessage: String? = null,
    val detectedGoogleOAuthUrl: String? = null
)

interface WebViewHostCallbacks {
    fun onFileChooserRequested(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean

    fun onWebPermissionRequested(request: PermissionRequest)
    fun onDownloadRequested(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?)
    fun onExternalSchemeRequested(url: String)
}

/**
 * Retains up to two live WebView instances across Pixel 9 Pro Fold folding/unfolding transitions
 * and Compose recompositions so sessions and typed inputs are NEVER lost or reloaded.
 */
class WebSessionHolder(
    private val appContext: Context,
    private val notificationBridge: WebNotificationBridge
) {
    private val webViews = mutableMapOf<PaneSlot, WebView>()
    private val contextWrappers = mutableMapOf<PaneSlot, MutableContextWrapper>()

    private val _primaryState = MutableStateFlow(WebPaneState(PaneSlot.PRIMARY))
    val primaryState: StateFlow<WebPaneState> = _primaryState.asStateFlow()

    private val _secondaryState = MutableStateFlow(WebPaneState(PaneSlot.SECONDARY))
    val secondaryState: StateFlow<WebPaneState> = _secondaryState.asStateFlow()

    @Volatile
    private var hostCallbacks: WebViewHostCallbacks? = null

    fun attachHost(activityContext: Context, callbacks: WebViewHostCallbacks) {
        hostCallbacks = callbacks
        contextWrappers.values.forEach { wrapper ->
            wrapper.baseContext = activityContext
        }
    }

    fun detachHost() {
        hostCallbacks = null
        contextWrappers.values.forEach { wrapper ->
            wrapper.baseContext = appContext
        }
    }

    fun getOrCreateWebView(slot: PaneSlot, activityContext: Context): WebView {
        val existing = webViews[slot]
        if (existing != null) {
            contextWrappers[slot]?.baseContext = activityContext
            // Detach cleanly from previous parent before re-embedding in Compose AndroidView
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

        val wrapper = MutableContextWrapper(activityContext)
        contextWrappers[slot] = wrapper

        val webView = createConfiguredWebView(wrapper, slot)
        webViews[slot] = webView
        return webView
    }

    fun loadProfileIfChanged(slot: PaneSlot, profileId: String, url: String) {
        val stateFlow = stateFlowFor(slot)
        val current = stateFlow.value
        if (current.profileId == profileId && current.currentUrl.isNotBlank()) {
            return
        }
        loadUrl(slot, url, profileId)
    }

    fun loadUrl(slot: PaneSlot, url: String, profileId: String? = stateFlowFor(slot).value.profileId) {
        val stateFlow = stateFlowFor(slot)
        stateFlow.update {
            it.copy(
                profileId = profileId,
                currentUrl = url,
                errorMessage = null,
                detectedGoogleOAuthUrl = if (OAuthDetector.isGoogleOAuthUrl(url)) url else null
            )
        }
        webViews[slot]?.loadUrl(url)
    }

    fun reload(slot: PaneSlot) {
        stateFlowFor(slot).update { it.copy(errorMessage = null, detectedGoogleOAuthUrl = null) }
        webViews[slot]?.reload()
    }

    fun goBack(slot: PaneSlot): Boolean {
        val webView = webViews[slot] ?: return false
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(slot: PaneSlot) {
        val webView = webViews[slot] ?: return
        if (webView.canGoForward()) {
            webView.goForward()
        }
    }

    fun dismissOAuthBanner(slot: PaneSlot) {
        stateFlowFor(slot).update { it.copy(detectedGoogleOAuthUrl = null) }
    }

    fun applyDisplaySettings(
        textZoomPercent: Int,
        wideLayout: Boolean,
        isDarkTheme: Boolean
    ) {
        webViews.values.forEach { webView ->
            webView.settings.apply {
                textZoom = textZoomPercent.coerceIn(75, 150)
                useWideViewPort = wideLayout
                loadWithOverviewMode = wideLayout
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
            }
        }
    }

    fun clearCacheAndCookies() {
        webViews.values.forEach { it.clearCache(true) }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    fun destroyAll() {
        webViews.values.forEach { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        webViews.clear()
        contextWrappers.clear()
    }

    private fun stateFlowFor(slot: PaneSlot): MutableStateFlow<WebPaneState> {
        return when (slot) {
            PaneSlot.PRIMARY -> _primaryState
            PaneSlot.SECONDARY -> _secondaryState
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(context: Context, slot: PaneSlot): WebView {
        val stateFlow = stateFlowFor(slot)

        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = true
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            val webView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }

            addJavascriptInterface(notificationBridge, WebNotificationBridge.JS_INTERFACE_NAME)

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                hostCallbacks?.onDownloadRequested(url, userAgent, contentDisposition, mimeType)
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val targetUrl = request?.url?.toString() ?: return false

                    if (OAuthDetector.isExternalAppScheme(targetUrl)) {
                        hostCallbacks?.onExternalSchemeRequested(targetUrl)
                        return true
                    }

                    // Do NOT spoof or bypass Google OAuth; instead detect Google Sign-In cleanly
                    // and offer 1-tap switch to Veilige Modus (Chrome Custom Tabs).
                    if (OAuthDetector.isGoogleOAuthUrl(targetUrl)) {
                        stateFlow.update {
                            it.copy(
                                isLoading = false,
                                detectedGoogleOAuthUrl = targetUrl
                            )
                        }
                        return true
                    }

                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    stateFlow.update {
                        it.copy(
                            currentUrl = url ?: it.currentUrl,
                            isLoading = true,
                            errorMessage = null,
                            canGoBack = view?.canGoBack() == true,
                            canGoForward = view?.canGoForward() == true
                        )
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                    view?.evaluateJavascript(WebNotificationBridge.INJECTED_NOTIFICATION_JS, null)
                    stateFlow.update {
                        it.copy(
                            currentUrl = url ?: it.currentUrl,
                            pageTitle = view?.title?.takeIf { t -> t.isNotBlank() } ?: it.pageTitle,
                            isLoading = false,
                            progress = 100,
                            canGoBack = view?.canGoBack() == true,
                            canGoForward = view?.canGoForward() == true
                        )
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        stateFlow.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error?.description?.toString()
                                    ?: "Kan pagina niet laden. Controleer je internetverbinding."
                            )
                        }
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    stateFlow.update { it.copy(progress = newProgress, isLoading = newProgress < 100) }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) {
                        stateFlow.update { it.copy(pageTitle = title) }
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return hostCallbacks?.onFileChooserRequested(filePathCallback, fileChooserParams)
                        ?: false
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request == null) return
                    val callbacks = hostCallbacks
                    if (callbacks != null) {
                        callbacks.onWebPermissionRequested(request)
                    } else {
                        request.deny()
                    }
                }
            }
        }
    }
}
