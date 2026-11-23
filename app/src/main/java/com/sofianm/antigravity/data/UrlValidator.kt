package com.sofianm.antigravity.data

import java.net.URI
import java.util.Locale

object UrlValidator {

    private val ALLOWED_SCHEMES = setOf("http", "https")
    private val URL_IN_TEXT_REGEX = Regex("""https?://[^\s"'<>\]\)]+""", RegexOption.IGNORE_CASE)
    private val JSON_URL_REGEX = Regex(
        """["'](?:url|href|link|target)["']\s*:\s*["'](https?://[^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val IPV4_REGEX = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    /**
     * Normalizes user-entered URL input into a valid http/https URL, or returns null if invalid/unsafe.
     * Automatically prefers http:// for localhost and private LAN addresses, and https:// otherwise.
     */
    fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase(Locale.ROOT)
        // Reject explicitly dangerous or non-web schemes right away
        if (lower.startsWith("javascript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("data:") ||
            lower.startsWith("content:") ||
            lower.startsWith("vbscript:") ||
            lower.startsWith("about:")
        ) {
            return null
        }

        val withScheme = if (trimmed.contains("://")) {
            trimmed
        } else {
            val hostCandidate = trimmed.substringBefore('/').substringBefore('?').substringBefore(':')
            val defaultScheme = if (isLocalHost(hostCandidate)) "http://" else "https://"
            "$defaultScheme$trimmed"
        }

        return runCatching {
            val uri = URI(withScheme)
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
            if (scheme !in ALLOWED_SCHEMES) return null

            val host = uri.host?.trim()
            if (host.isNullOrBlank()) return null
            if (!isValidHost(host)) return null

            // Preserve full URI safely
            uri.toASCIIString()
        }.getOrNull()
    }

    /**
     * Extracts and normalizes a valid web URL from a scanned QR code payload.
     * Supports raw URLs, JSON payloads with url/href/link fields, or text containing a URL.
     */
    fun extractUrlFromQrPayload(payload: String): String? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null

        // 1. JSON payload check (e.g. {"url": "https://..."})
        JSON_URL_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { candidate ->
            normalizeUrl(candidate)?.let { return it }
        }

        // 2. Embedded http(s) URL inside text
        URL_IN_TEXT_REGEX.find(trimmed)?.value?.let { candidate ->
            val cleaned = candidate.trimEnd('.', ',', ';', '!', '?')
            normalizeUrl(cleaned)?.let { return it }
        }

        // 3. Direct domain/URL without whitespace
        if (!trimmed.any { it.isWhitespace() }) {
            normalizeUrl(trimmed)?.let { return it }
        }

        return null
    }

    /**
     * Suggests a concise human-readable profile name from a normalized URL.
     */
    fun suggestProfileName(url: String): String {
        return runCatching {
            val host = URI(url).host.orEmpty().removePrefix("www.")
            if (host.isBlank()) return "Antigravity"
            if (host.contains("antigravity", ignoreCase = true)) return "Antigravity"
            val primaryLabel = host.substringBefore('.')
            primaryLabel.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
            }.ifBlank { host }
        }.getOrDefault("Antigravity")
    }

    internal fun isLocalHost(host: String): Boolean {
        val normalized = host.lowercase(Locale.ROOT).trim('[', ']')
        if (normalized == "localhost" || normalized.endsWith(".local") || normalized == "::1") {
            return true
        }
        val match = IPV4_REGEX.matchEntire(normalized) ?: return false
        val octets = match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return when {
            octets[0] == 127 -> true
            octets[0] == 10 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            else -> false
        }
    }

    private fun isValidHost(host: String): Boolean {
        if (host.any { it.isWhitespace() }) return false
        if (isLocalHost(host)) return true
        // Require at least one dot for non-local hostnames (e.g. example.com)
        if (!host.contains('.')) return false
        if (host.startsWith('.') || host.endsWith('.')) return false
        val labels = host.split('.')
        return labels.all { label ->
            label.isNotEmpty() &&
                !label.startsWith('-') &&
                !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}
