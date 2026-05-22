package com.cleanslate.adshield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class AdShieldVpnService : VpnService() {
    private val running = AtomicBoolean(false)
    private val blockedQueries = AtomicLong(0)
    private val allowedQueries = AtomicLong(0)
    private val executor = Executors.newSingleThreadExecutor()

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var blocklist: Blocklist

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun startVpn() {
        if (!running.compareAndSet(false, true)) return
        blocklist = Blocklist.load(this)
        startForeground(NOTIFICATION_ID, buildNotification())

        executor.execute {
            try {
                val builder = Builder()
                    .setSession("Clean Slate AdShield")
                    .setMtu(1500)
                    .addAddress(VPN_ADDRESS, 32)
                    .addDnsServer(VPN_DNS)
                    .addRoute(VPN_DNS, 32)
                    .allowFamily(android.system.OsConstants.AF_INET)

                vpnInterface = builder.establish()
                val descriptor = vpnInterface?.fileDescriptor ?: return@execute
                FileInputStream(descriptor).use { input ->
                    FileOutputStream(descriptor).use { output ->
                        val buffer = ByteArray(32767)
                        while (running.get()) {
                            val length = input.read(buffer)
                            if (length <= 0) continue
                            handlePacket(buffer.copyOf(length), length, output)
                        }
                    }
                }
            } catch (_: Exception) {
                stopVpn()
            }
        }
    }

    private fun handlePacket(frame: ByteArray, length: Int, output: FileOutputStream) {
        val udp = Packet.parseUdpDatagram(frame, length) ?: return
        if (udp.destinationPort != DNS_PORT) return
        val query = DnsMessage.parseQuery(udp.payload) ?: return

        val dnsResponse = if (blocklist.isBlocked(query.hostname)) {
            blockedQueries.incrementAndGet()
            DnsMessage.buildBlockedResponse(udp.payload)
        } else {
            allowedQueries.incrementAndGet()
            forwardDnsQuery(udp.payload)
        } ?: return

        val responsePacket = Packet.buildUdpResponse(udp, dnsResponse)
        output.write(responsePacket)
        output.flush()
        maybeUpdateNotification()
    }

    private fun forwardDnsQuery(payload: ByteArray): ByteArray? = try {
        DatagramSocket().use { socket ->
            protect(socket)
            socket.soTimeout = DNS_TIMEOUT_MS
            val upstream = InetSocketAddress(UPSTREAM_DNS, DNS_PORT)
            socket.send(DatagramPacket(payload, payload.size, upstream))
            val responseBuffer = ByteArray(4096)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(response)
            response.data.copyOf(response.length)
        }
    } catch (_: Exception) {
        null
    }

    private fun stopVpn() {
        if (!running.getAndSet(false)) return
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun maybeUpdateNotification() {
        val total = blockedQueries.get() + allowedQueries.get()
        if (total % 25L != 0L) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        createChannelIfNeeded()
        val stopIntent = Intent(this, AdShieldVpnService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Clean Slate AdShield is active")
            .setContentText("Blocked ${blockedQueries.get()} DNS requests · Allowed ${allowedQueries.get()}")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AdShield protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows local DNS filtering VPN status."
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.cleanslate.adshield.START"
        const val ACTION_STOP = "com.cleanslate.adshield.STOP"
        private const val CHANNEL_ID = "adshield_vpn"
        private const val NOTIFICATION_ID = 1001
        private const val VPN_ADDRESS = "10.7.0.2"
        private const val VPN_DNS = "10.7.0.1"
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val DNS_TIMEOUT_MS = 3000
    }
}
