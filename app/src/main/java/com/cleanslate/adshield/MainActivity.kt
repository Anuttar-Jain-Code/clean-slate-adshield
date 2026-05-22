package com.cleanslate.adshield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var reloadButton: Button

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
    }

    private fun buildLayout(): ScrollView {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 40.dp(), 24.dp(), 24.dp())
        }

        root.addView(TextView(this).apply {
            text = "Clean Slate AdShield"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Advanced local DNS firewall for Android apps, browsers, trackers, and ad domains."
            textSize = 16f
            setPadding(0, 12.dp(), 0, 20.dp())
        })

        statusText = TextView(this).apply {
            text = "Protection is off. Tap Start and approve the Android VPN prompt."
            textSize = 15f
            setPadding(0, 0, 0, 20.dp())
        }
        root.addView(statusText)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        startButton = Button(this).apply { text = "Start" }
        stopButton = Button(this).apply { text = "Stop" }
        reloadButton = Button(this).apply { text = "Reload rules" }
        row.addView(startButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(stopButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)
        root.addView(reloadButton)

        root.addView(section("10x protection upgrades", listOf(
            "Multi-resolver DNS fallback for better reliability.",
            "In-memory DNS response cache for faster browsing.",
            "Exact, wildcard, suffix, hosts-file, and allowlist rule support.",
            "Local-only filtering; no browsing-history upload.",
            "Foreground status with blocked, allowed, cached, and error counters."
        )))

        root.addView(section("Important limitation", listOf(
            "No honest mobile app can guarantee every ad is blocked across every platform.",
            "YouTube and streaming apps can serve ads from shared content infrastructure.",
            "This app does not bypass subscriptions, DRM, app protections, or platform rules."
        )))

        return ScrollView(this).apply { addView(root) }
    }

    private fun section(title: String, bullets: List<String>): LinearLayout {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 22.dp(), 0, 0)
            addView(TextView(context).apply {
                text = title
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            })
            bullets.forEach { bullet ->
                addView(TextView(context).apply {
                    text = "• $bullet"
                    textSize = 14f
                    setPadding(0, 6.dp(), 0, 0)
                })
            }
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
    }

    private fun stopProtection() {
        val intent = Intent(this, AdShieldVpnService::class.java).setAction(AdShieldVpnService.ACTION_STOP)
        startService(intent)
        statusText.text = "Protection is off."
    }

    private fun reloadRules() {
        val intent = Intent(this, AdShieldVpnService::class.java).setAction(AdShieldVpnService.ACTION_RELOAD)
        startService(intent)
        statusText.text = "Rules reloaded. Restart protection if it was off."
    }
}
