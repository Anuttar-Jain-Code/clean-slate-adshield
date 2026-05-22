package com.cleanslate.adshield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

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
    }

    private fun buildLayout(): LinearLayout {
        val density = resources.displayMetrics.density
        fun Int.dp() = (this * density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 40.dp(), 24.dp(), 24.dp())

            addView(TextView(context).apply {
                text = "Clean Slate AdShield"
                textSize = 28f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })

            addView(TextView(context).apply {
                text = "Local DNS filtering VPN for reducing ads, trackers, and telemetry across Android apps."
                textSize = 16f
                setPadding(0, 12.dp(), 0, 20.dp())
            })

            statusText = TextView(context).apply {
                text = "Protection is off. Tap Start and approve the Android VPN prompt."
                textSize = 15f
                setPadding(0, 0, 0, 20.dp())
            }
            addView(statusText)

            startButton = Button(context).apply {
                text = "Start protection"
            }
            addView(startButton)

            stopButton = Button(context).apply {
                text = "Stop protection"
            }
            addView(stopButton)

            addView(TextView(context).apply {
                text = "Disclaimer: this app cannot guarantee blocking every ad on every platform. YouTube and streaming apps may serve ads from the same systems used for normal content, so aggressive blocking can break playback."
                textSize = 13f
                setPadding(0, 24.dp(), 0, 0)
            })
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
        statusText.text = "Protection is stopping..."
        val intent = Intent(this, AdShieldVpnService::class.java).setAction(AdShieldVpnService.ACTION_STOP)
        startService(intent)
        statusText.text = "Protection is off."
    }
}
