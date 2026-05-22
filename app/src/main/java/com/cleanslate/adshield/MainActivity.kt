package com.cleanslate.adshield

import android.Manifest
import android.app.AlertDialog
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
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
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

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Clean Slate AdShield"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(this).apply {
            text = "⋮"
            textSize = 22f
            contentDescription = "More options"
            setOnClickListener { showOverflowMenu(it) }
        }, LinearLayout.LayoutParams(56.dp(), LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(header)

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

    private fun showOverflowMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Privacy Policy")
            menu.add("Contact Us")
            menu.add("About")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Privacy Policy" -> showLongTextDialog("Privacy Policy", PRIVACY_POLICY_TEXT)
                    "Contact Us" -> showLongTextDialog("Contact Us", CONTACT_TEXT)
                    "About" -> showLongTextDialog("About", ABOUT_TEXT)
                }
                true
            }
            show()
        }
    }

    private fun showLongTextDialog(title: String, message: String) {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()
        val content = ScrollView(this).apply {
            addView(TextView(this@MainActivity).apply {
                text = message
                textSize = 14f
                setPadding(20.dp(), 12.dp(), 20.dp(), 12.dp())
            })
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(content)
            .setPositiveButton("OK", null)
            .show()
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

        private const val CONTACT_TEXT = """
Contact us

For support, privacy questions, bug reports, or app publishing questions, contact:

anuttar209@gmail.com
"""

        private const val ABOUT_TEXT = """
Clean Slate AdShield

A local Android DNS filtering VPN that helps reduce ads, trackers, analytics, and telemetry requests across apps and browsers.

The app runs without root access and shows local protection counters in the live dashboard.
"""

        private const val PRIVACY_POLICY_TEXT = """
Privacy Policy

Effective date: 2026-05-22

Clean Slate AdShield is a local DNS filtering VPN app for Android. This policy explains what the app does with data and how users can control the app.

What the app does
Clean Slate AdShield creates a local Android VPN session on the user's device after the user approves the Android VPN permission prompt. The app uses this local VPN to inspect DNS requests and decide whether to allow or block known advertising, analytics, and tracking hostnames.

Data collection
The app does not require account creation and does not collect names, email addresses, phone numbers, contacts, photos, messages, precise location, payment information, or device identifiers for external tracking.

DNS and browsing data
DNS hostnames may be processed locally on the device so the app can decide whether to allow or block a request. The app is designed to keep filtering local to the device. It does not upload browsing history, DNS history, or visited website lists to the developer.

Local app data
The app stores local counters such as blocked requests, allowed requests, cache hits, errors, loaded rule count, and the last update time. These counters are stored on the user's device to show the live dashboard and notification status.

Data sharing
The app does not sell user data. The app does not share browsing history or DNS history with advertisers or data brokers.

Allowed DNS requests may be forwarded to configured upstream DNS resolvers so normal internet browsing can work. DNS resolvers can see DNS queries they receive, subject to their own privacy policies.

Permissions
VPN permission is required to create the local DNS filtering VPN.
Notification permission is used to show the foreground service status notification on supported Android versions.
Internet permission is required to forward allowed DNS queries to upstream DNS resolvers.
Foreground service permission is required to keep protection running while the app filters DNS traffic.

Limitations
DNS filtering cannot guarantee that every advertisement or tracker will be blocked. Some platforms serve ads and normal content from the same infrastructure, so aggressive blocking can break video playback, login, or app functionality.

Children's privacy
The app is not designed to collect personal information from children.

User controls
Users can stop protection at any time using the app's Stop button, the notification action, or Android VPN settings. Users can uninstall the app to remove local app data.

Changes to this policy
This policy may be updated when app behavior changes. Any material changes should be reflected in the app, repository, and store listing before release.

Contact us
For support or privacy questions, contact: anuttar209@gmail.com
"""
    }
}
