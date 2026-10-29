/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.accessibility

/** What kind of social media page a browser is showing. */
internal enum class BrowserSocialKind { SHORT_VIDEO, GENERAL }

/**
 * Reads the address bar of common browsers and classifies social media pages.
 * Browsers cannot report individual reels, so callers convert time on these pages into fatigue.
 */
internal object BrowserSocialDetector {
    /** Browser package to the view ID of its address bar text. */
    val urlBarIds: Map<String, String> = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.chrome.beta" to "com.chrome.beta:id/url_bar",
        "com.chrome.dev" to "com.chrome.dev:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.opera.browser" to "com.opera.browser:id/url_field",
    )

    private val socialDomains = listOf(
        "facebook.com", "fb.com", "fb.watch", "youtube.com", "youtu.be", "instagram.com",
        "tiktok.com", "snapchat.com", "likee.video", "x.com", "twitter.com", "threads.net",
    )

    fun isBrowser(packageName: String?): Boolean = packageName != null && packageName in urlBarIds

    /** Classifies address bar text such as "m.youtube.com/shorts/abc" or "https://www.facebook.com/reel/1". */
    fun classify(url: String?): BrowserSocialKind? {
        val text = url?.trim()?.lowercase()?.substringAfter("://") ?: return null
        val host = text.substringBefore('/').substringBefore('?').substringBefore(':')
        val path = text.substringAfter('/', "")
        val domain = socialDomains.firstOrNull { host == it || host.endsWith(".$it") } ?: return null

        val isShortVideo = when (domain) {
            "youtube.com" -> path.startsWith("shorts")
            "facebook.com", "instagram.com" -> path.startsWith("reel")
            "tiktok.com", "likee.video" -> true
            "snapchat.com" -> path.startsWith("spotlight")
            else -> false
        }
        return if (isShortVideo) BrowserSocialKind.SHORT_VIDEO else BrowserSocialKind.GENERAL
    }
}
