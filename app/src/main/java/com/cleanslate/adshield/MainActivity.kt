package com.cleanslate.adshield

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var reloadButton: Button
    private lateinit var privacySwitch: Switch
    private lateinit var strictSwitch: Switch
    private lateinit var blockedValue: TextView
    private lateinit var allowedValue: TextView
    private lateinit var cachedValue: TextView
    private lateinit var errorsValue: TextView
    private lateinit var totalValue: TextView
    private lateinit var rulesValue: TextView
    private lateinit var lastUpdatedValue: TextView

    private val dashboardHandler = Handler(Looper.getMainLooper())
    private val dashboardTicker = object : Runnable {
        override fun run() {
            refreshDashboard()
            dashboardHandler.postDelayed(this, DASHBOARD_REFRESH_MS)
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startProtection()
        else statusText.text = "VPN permission was denied. Protection is off."
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startVpnPermissionFlow() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        startButton.setOnClickListener { requestNotificationThenVpn() }
        stopButton.setOnClickListener { stopProtection() }
        reloadButton.setOnClickListener { reloadRules() }
        privacySwitch.setOnCheckedChangeListener { _, enabled ->
            Settings.setPrivacyShield(this, enabled)
            applySettingsChange()
        }
        strictSwitch.setOnCheckedChangeListener { _, enabled ->
            Settings.setStrictMode(this, enabled)
            applySettingsChange()
        }
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        dashboardHandler.removeCallbacks(dashboardTicker)
        dashboardHandler.post(dashboardTicker)
    }

    override fun onPause() {
        dashboardHandler.removeCallbacks(dashboardTicker)
        super.onPause()
    }

    private fun buildLayout(): ScrollView {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()
        val settings = Settings.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 40.dp(), 24.dp(), 24.dp())
        }

        root.addView(TextView(this).apply {
            text = "Clean Slate AdShield"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })

        statusText = TextView(this).apply {
            text = "Protection is off."
            textSize = 15f
            setPadding(0, 12.dp(), 0, 16.dp())
        }
        root.addView(statusText)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        startButton = Button(this).apply { text = "Start" }
        stopButton = Button(this).apply { text = "Stop" }
        reloadButton = Button(this).apply { text = "Reload" }
        row.addView(startButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(stopButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)
        root.addView(reloadButton)

        root.addView(TextView(this).apply {
            text = "Options"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 18.dp(), 0, 4.dp())
        })
        privacySwitch = Switch(this).apply {
            text = "Privacy Shield"
            textSize = 15f
            isChecked = settings.privacyShield
        }
        strictSwitch = Switch(this).apply {
            text = "Strict Mode"
            textSize = 15f
            isChecked = settings.strictMode
        }
        root.addView(privacySwitch)
        root.addView(strictSwitch)
        root.addView(TextView(this).apply {
            text = "Strict Mode blocks extra hostnames and may break some media playback."
            textSize = 12f
            setPadding(0, 2.dp(), 0, 10.dp())
        })

        root.addView(buildDashboard())
        return ScrollView(this).apply { addView(root) }
    }

    private fun buildDashboard(): LinearLayout {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()
        blockedValue = dashboardNumber("0")
        allowedValue = dashboardNumber("0")
        cachedValue = dashboardNumber("0")
        errorsValue = dashboardNumber("0")
        totalValue = dashboardNumber("0")
        rulesValue = dashboardNumber("0")
        lastUpdatedValue = TextView(this).apply {
            text = "Last updated: never"
            textSize = 13f
            setPadding(0, 12.dp(), 0, 0)
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 18.dp(), 0, 0)
            addView(TextView(context).apply {
                text = "Live dashboard"
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(dashboardRow(dashboardCard("Blocked", blockedValue), dashboardCard("Allowed", allowedValue)))
            addView(dashboardRow(dashboardCard("Cache", cachedValue), dashboardCard("Errors", errorsValue)))
            addView(dashboardRow(dashboardCard("Total", totalValue), dashboardCard("Rules", rulesValue)))
            addView(lastUpdatedValue)
        }
    }

    private fun dashboardRow(left: LinearLayout, right: LinearLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun dashboardCard(label: String, value: TextView): LinearLayout {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
            addView(TextView(context).apply { text = label; textSize = 13f })
            addView(value)
        }
    }

    private fun dashboardNumber(initialValue: String): TextView = TextView(this).apply {
        text = initialValue
        textSize = 24f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun refreshDashboard() {
        val stats = StatsStore.load(this)
        blockedValue.text = stats.blocked.toString()
        allowedValue.text = stats.allowed.toString()
        cachedValue.text = stats.cached.toString()
        errorsValue.text = stats.errors.toString()
        totalValue.text = stats.total.toString()
        rulesValue.text = stats.rulesLoaded.toString()
        statusText.text = if (stats.running) "Protection is active." else "Protection is off."
        lastUpdatedValue.text = if (stats.lastUpdatedMs == 0L) {
            "Last updated: never"
        } else {
            "Last updated: ${((System.currentTimeMillis() - stats.lastUpdatedMs) / 1000).coerceAtLeast(0)}s ago"
        }
    }

    private fun requestNotificationThenVpn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startVpnPermissionFlow()
    }

    private fun startVpnPermissionFlow() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) vpnPermissionLauncher.launch(prepareIntent)
        else startProtection()
    }

    private fun startProtection() {
        statusText.text = "Protection is starting..."
        val intent = Intent(this, AdShieldVpnService::class.java).setAction(AdShieldVpnService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
        refreshDashboard()
    }

    private fun stopProtection() {
        val intent = Intent(this, AdShieldVpnService::class.java).setAction(AdShieldVpnService.ACTION_STOP)
        startService(intent)
        statusText.text = "Protection is off."
        refreshDashboard()
    }

    private fun reloadRules() {
        val intent = Intent(this, AdShieldVpnService::class.java).setAction(AdShieldVpnService.ACTION_RELOAD)
        startService(intent)
        statusText.text = "Rules reloaded."
        refreshDashboard()
    }

    private fun applySettingsChange() {
        if (StatsStore.load(this).running) reloadRules() else refreshDashboard()
    }

    object Settings {
        data class Snapshot(val privacyShield: Boolean, val strictMode: Boolean)
        private const val PREFS = "adshield_settings"
        private const val KEY_PRIVACY = "privacy_shield"
        private const val KEY_STRICT = "strict_mode"

        fun load(context: Context): Snapshot {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Snapshot(
                privacyShield = prefs.getBoolean(KEY_PRIVACY, true),
                strictMode = prefs.getBoolean(KEY_STRICT, false)
            )
        }

        fun setPrivacyShield(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PRIVACY, enabled).apply()
        }

        fun setStrictMode(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_STRICT, enabled).apply()
        }
    }

    companion object {
        private const val DASHBOARD_REFRESH_MS = 1_000L
    }
}
