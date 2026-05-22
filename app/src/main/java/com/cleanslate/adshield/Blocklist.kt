package com.cleanslate.adshield

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class Blocklist private constructor(
    private val exactBlockRules: Set<String>,
    private val suffixBlockRules: Set<String>,
    private val exactAllowRules: Set<String>,
    private val suffixAllowRules: Set<String>
) {
    data class Decision(val blocked: Boolean, val reason: String)

    fun decide(hostname: String): Decision {
        val host = normalize(hostname) ?: return Decision(false, "empty")
        if (matches(host, exactAllowRules, suffixAllowRules)) return Decision(false, "allowlist")
        if (host in exactBlockRules) return Decision(true, "exact")
        if (matchesSuffix(host, suffixBlockRules)) return Decision(true, "suffix")
        return Decision(false, "not-listed")
    }

    fun isBlocked(hostname: String): Boolean = decide(hostname).blocked

    val ruleCount: Int
        get() = exactBlockRules.size + suffixBlockRules.size + exactAllowRules.size + suffixAllowRules.size

    private fun matches(host: String, exactRules: Set<String>, suffixRules: Set<String>): Boolean =
        host in exactRules || matchesSuffix(host, suffixRules)

    private fun matchesSuffix(host: String, suffixRules: Set<String>): Boolean {
        if (suffixRules.isEmpty()) return false
        val labels = host.split('.')
        for (index in labels.indices) {
            val suffix = labels.drop(index).joinToString(".")
            if (suffix in suffixRules) return true
        }
        return false
    }

    companion object {
        fun load(context: Context): Blocklist {
            val exactBlock = linkedSetOf<String>()
            val suffixBlock = linkedSetOf<String>()
            val exactAllow = linkedSetOf<String>()
            val suffixAllow = linkedSetOf<String>()
            val settings = MainActivity.Settings.load(context)

            context.assets.open(AppConfig.BLOCKLIST_ASSET).use { input ->
                BufferedReader(InputStreamReader(input)).useLines { lines ->
                    lines.forEach { line ->
                        parseRule(line)?.let { rule ->
                            val targetExact = if (rule.allow) exactAllow else exactBlock
                            val targetSuffix = if (rule.allow) suffixAllow else suffixBlock
                            if (rule.suffix) targetSuffix += rule.domain else targetExact += rule.domain
                        }
                    }
                }
            }

            if (settings.privacyShield) {
                suffixBlock += PRIVACY_SHIELD_SUFFIX_RULES
                exactBlock += PRIVACY_SHIELD_EXACT_RULES
            }

            if (settings.strictMode) {
                suffixBlock += STRICT_MODE_SUFFIX_RULES
                exactBlock += STRICT_MODE_EXACT_RULES
            }

            return Blocklist(exactBlock, suffixBlock, exactAllow, suffixAllow)
        }

        private data class ParsedRule(val domain: String, val allow: Boolean, val suffix: Boolean)

        private val PRIVACY_SHIELD_SUFFIX_RULES = setOf(
            "adobedtm.com",
            "clarity.ms",
            "bat.bing.com",
            "permutive.com",
            "parsely.com",
            "gumgum.com",
            "lijit.com",
            "mxpnl.com",
            "segment.com",
            "mixpanel.com",
            "amplitude.com",
            "hotjar.com",
            "fullstory.com",
            "mouseflow.com",
            "crazyegg.com",
            "optimizely.com",
            "newrelic.com",
            "nr-data.net",
            "sentry.io",
            "bugsnag.com",
            "rollbar.com",
            "datadoghq.com",
            "cloudflareinsights.com",
            "chartbeat.com",
            "quantcast.com",
            "comscore.com"
        )

        private val PRIVACY_SHIELD_EXACT_RULES = setOf(
            "analytics.google.com",
            "www.google-analytics.com",
            "ssl.google-analytics.com"
        )

        private val STRICT_MODE_SUFFIX_RULES = setOf(
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "googleads.g.doubleclick.net",
            "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "static.doubleclick.net",
            "youtubeads.google.com",
            "video-stats.l.google.com"
        )

        private val STRICT_MODE_EXACT_RULES = setOf(
            "ads.youtube.com",
            "s.youtube.com"
        )

        private fun parseRule(raw: String): ParsedRule? {
            val noComment = raw.substringBefore('#').trim()
            if (noComment.isBlank()) return null
            val allow = noComment.startsWith("@@") || noComment.startsWith("!")
            val cleaned = noComment
                .removePrefix("@@")
                .removePrefix("!")
                .removePrefix("0.0.0.0 ")
                .removePrefix("127.0.0.1 ")
                .trim()
                .removePrefix("||")
                .removeSuffix("^")
                .trimStart('*')
                .trimStart('.')
                .trimEnd('.')
                .lowercase(Locale.US)

            val domain = normalize(cleaned) ?: return null
            val suffix = noComment.contains("*") || noComment.startsWith(".") || noComment.startsWith("||")
            return ParsedRule(domain, allow, suffix)
        }

        private fun normalize(hostname: String): String? {
            val host = hostname.trim().trim('.').lowercase(Locale.US)
            if (host.isBlank()) return null
            if (host.length > 253) return null
            if (!host.all { it.isLetterOrDigit() || it == '-' || it == '.' }) return null
            if (host.contains("..")) return null
            return host
        }
    }
}
