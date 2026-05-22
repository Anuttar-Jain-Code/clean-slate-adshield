package com.cleanslate.adshield

object AppConfig {
    const val VPN_ADDRESS = "10.7.0.2"
    const val VPN_DNS = "10.7.0.1"
    const val DNS_PORT = 53
    const val DNS_TIMEOUT_MS = 2500
    const val MTU = 1500
    const val DNS_CACHE_TTL_MS = 60_000L
    const val DNS_CACHE_MAX_ENTRIES = 512
    const val BLOCKLIST_ASSET = "blocklist.txt"
    const val MAX_REMOTE_BLOCKLIST_BYTES = 5 * 1024 * 1024

    val UPSTREAM_DNS_SERVERS = listOf("1.1.1.1", "9.9.9.9")
}
