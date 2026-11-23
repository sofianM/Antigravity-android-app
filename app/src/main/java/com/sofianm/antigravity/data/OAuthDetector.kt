package com.sofianm.antigravity.data

import java.net.URI
import java.util.Locale

/**
 * Detects Google OAuth / Sign-In URLs that Google intentionally blocks inside embedded Android
 * WebViews (Error 403: disallowed_useragent). Instead of spoofing or bypassing Google's security
 * controls, we surface a clean prompt to open the session in Chrome Custom Tabs ("Veilige Modus").
 */
object OAuthDetector {

    private val GOOGLE_OAUTH_HOSTS = setOf(
        "accounts.google.com",
        "accounts.youtube.com",
        "oauth2.googleapis.com",
        "myaccount.google.com"
    )

    private val EXTERNAL_SCHEMES = setOf(
        "mailto",
        "tel",
        "sms",
        "intent",
        "market",
        "geo"
    )

    fun isGoogleOAuthUrl(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase(Locale.ROOT) ?: return false
            if (host in GOOGLE_OAUTH_HOSTS) return true
            if (host.endsWith(".google.com")) {
                val path = uri.path?.lowercase(Locale.ROOT).orEmpty()
                if (path.contains("/o/oauth2") || path.contains("servicelogin")) {
                    return true
                }
            }
            false
        }.getOrDefault(false)
    }

    fun isExternalAppScheme(url: String): Boolean {
        return runCatching {
            val scheme = URI(url).scheme?.lowercase(Locale.ROOT) ?: return false
            scheme in EXTERNAL_SCHEMES
        }.getOrDefault(false)
    }
}
