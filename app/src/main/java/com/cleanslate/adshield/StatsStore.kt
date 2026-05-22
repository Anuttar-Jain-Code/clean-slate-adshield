package com.cleanslate.adshield

import android.content.Context

object StatsStore {
    data class Snapshot(
        val running: Boolean,
        val blocked: Long,
        val allowed: Long,
        val cached: Long,
        val errors: Long,
        val rulesLoaded: Int,
        val lastUpdatedMs: Long
    ) {
        val total: Long
            get() = blocked + allowed + cached + errors
    }

    private const val PREFS_NAME = "adshield_stats"
    private const val KEY_RUNNING = "running"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_ALLOWED = "allowed"
    private const val KEY_CACHED = "cached"
    private const val KEY_ERRORS = "errors"
    private const val KEY_RULES_LOADED = "rules_loaded"
    private const val KEY_LAST_UPDATED = "last_updated"

    fun save(context: Context, snapshot: Snapshot) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RUNNING, snapshot.running)
            .putLong(KEY_BLOCKED, snapshot.blocked)
            .putLong(KEY_ALLOWED, snapshot.allowed)
            .putLong(KEY_CACHED, snapshot.cached)
            .putLong(KEY_ERRORS, snapshot.errors)
            .putInt(KEY_RULES_LOADED, snapshot.rulesLoaded)
            .putLong(KEY_LAST_UPDATED, snapshot.lastUpdatedMs)
            .apply()
    }

    fun load(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Snapshot(
            running = prefs.getBoolean(KEY_RUNNING, false),
            blocked = prefs.getLong(KEY_BLOCKED, 0L),
            allowed = prefs.getLong(KEY_ALLOWED, 0L),
            cached = prefs.getLong(KEY_CACHED, 0L),
            errors = prefs.getLong(KEY_ERRORS, 0L),
            rulesLoaded = prefs.getInt(KEY_RULES_LOADED, 0),
            lastUpdatedMs = prefs.getLong(KEY_LAST_UPDATED, 0L)
        )
    }

    fun reset(context: Context, running: Boolean = false, rulesLoaded: Int = 0) {
        save(
            context,
            Snapshot(
                running = running,
                blocked = 0L,
                allowed = 0L,
                cached = 0L,
                errors = 0L,
                rulesLoaded = rulesLoaded,
                lastUpdatedMs = System.currentTimeMillis()
            )
        )
    }
}
