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
    private val cachedQueries = AtomicLong(0)
    private val errorQueries = AtomicLong(0)
    private val executor = Executors.newSingleThreadExecutor()
    private val dnsCache = DnsCache()

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var blocklist: Blocklist

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            ACTION_RELOAD -> reloadBlocklist()
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
        dnsCache.clear()
        publishStats()
        startForeground(NOTIFICATION_ID, buildNotification())

        executor.execute {
            try {
                val builder = Builder()
                    .setSession("Clean Slate AdShield")
                    .setMtu(AppConfig.MTU)
                    .addAddress(AppConfig.VPN_ADDRESS, 32)
                    .addDnsServer(AppConfig.VPN_DNS)
                    .addRoute(AppConfig.VPN_DNS, 32)
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
                errorQueries.incrementAndGet()
                publishStats()
                stopVpn()
            }
        }
    }

    private fun handlePacket(frame: ByteArray, length: Int, output: FileOutputStream) {
        val udp = Packet.parseUdpDatagram(frame, length) ?: return
        if (udp.destinationPort != AppConfig.DNS_PORT) return
        val query = DnsMessage.parseQuery(udp.payload) ?: return
        val decision = blocklist.decide(query.hostname)

        val dnsResponse = if (decision.blocked) {
            blockedQueries.incrementAndGet()
            DnsMessage.buildBlockedResponse(udp.payload)
        } else {
            val cacheKey = cacheKey(query)
            dnsCache.get(cacheKey)?.let { cached ->
                cachedQueries.incrementAndGet()
                DnsMessage.copyResponseWithQueryId(cached, udp.payload)
            } ?: forwardDnsQuery(udp.payload)?.also {
                allowedQueries.incrementAndGet()
                dnsCache.put(cacheKey, it)
            }
        } ?: run {
            errorQueries.incrementAndGet()
            publishStats()
            return
        }

        val responsePacket = Packet.buildUdpResponse(udp, dnsResponse)
        output.write(responsePacket)
        output.flush()
        publishStats()
        maybeUpdateNotification()
    }

    private fun cacheKey(query: DnsMessage.Query): String =
        "${query.hostname}|${query.qType}|${query.qClass}"

    private fun forwardDnsQuery(payload: ByteArray): ByteArray? {
        for (server in AppConfig.UPSTREAM_DNS_SERVERS) {
            val response = queryUdpResolver(server, payload)
            if (response != null) return response
        }
        return null
    }

    private fun queryUdpResolver(server: String, payload: ByteArray): ByteArray? = try {
        DatagramSocket().use { socket ->
            protect(socket)
            socket.soTimeout = AppConfig.DNS_TIMEOUT_MS
            val upstream = InetSocketAddress(server, AppConfig.DNS_PORT)
            socket.send(DatagramPacket(payload, payload.size, upstream))
            val responseBuffer = ByteArray(4096)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(response)
            response.data.copyOf(response.length)
        }
    } catch (_: Exception) {
        null
    }

    private fun reloadBlocklist() {
        if (!::blocklist.isInitialized) return
        blocklist = Blocklist.load(this)
        dnsCache.clear()
        publishStats()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun stopVpn() {
        if (!running.getAndSet(false)) return
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        dnsCache.clear()
        publishStats(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishStats(isRunning: Boolean = running.get()) {
        StatsStore.save(
            this,
            StatsStore.Snapshot(
                running = isRunning,
                blocked = blockedQueries.get(),
                allowed = allowedQueries.get(),
                cached = cachedQueries.get(),
                errors = errorQueries.get(),
                rulesLoaded = if (::blocklist.isInitialized) blocklist.ruleCount else 0,
                lastUpdatedMs = System.currentTimeMillis()
            )
        )
    }

    private fun maybeUpdateNotification() {
        val total = blockedQueries.get() + allowedQueries.get() + cachedQueries.get() + errorQueries.get()
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
            .setContentText("Blocked ${blockedQueries.get()} · Allowed ${allowedQueries.get()} · Cached ${cachedQueries.get()}")
            .setStyle(Notification.BigTextStyle().bigText("Blocked ${blockedQueries.get()} DNS requests, allowed ${allowedQueries.get()}, cache hits ${cachedQueries.get()}, errors ${errorQueries.get()}. Rules loaded: ${if (::blocklist.isInitialized) blocklist.ruleCount else 0}."))
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
        const val ACTION_RELOAD = "com.cleanslate.adshield.RELOAD"
        private const val CHANNEL_ID = "adshield_vpn"
        private const val NOTIFICATION_ID = 1001
    }
}
