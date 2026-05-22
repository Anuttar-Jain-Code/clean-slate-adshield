package com.cleanslate.adshield

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class Blocklist private constructor(private val rules: Set<String>) {
    fun isBlocked(hostname: String): Boolean {
        val host = hostname.trim('.').lowercase(Locale.US)
        if (host.isBlank()) return false
        if (host in rules) return true

        val labels = host.split('.')
        for (index in 1 until labels.size) {
            val suffix = "." + labels.drop(index).joinToString(".")
            if (suffix in rules) return true
        }
        return false
    }

    companion object {
        fun load(context: Context): Blocklist {
            val entries = linkedSetOf<String>()
            context.assets.open("blocklist.txt").use { input ->
                BufferedReader(InputStreamReader(input)).useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .map { it.lowercase(Locale.US) }
                        .forEach { rule ->
                            val clean = rule.removePrefix("0.0.0.0 ")
                                .removePrefix("127.0.0.1 ")
                                .trim()
                                .trimEnd('.')
                            if (clean.isNotBlank()) entries += clean
                        }
                }
            }
            return Blocklist(entries)
        }
    }
}
