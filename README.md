# Clean Slate AdShield

Clean Slate AdShield is an Android DNS-filtering VPN app that helps users reduce ads, trackers, and telemetry domains across mobile apps and browsers.

> Important: no mobile app can honestly guarantee blocking every advertisement across every platform. Apps such as YouTube can serve ads from the same infrastructure as normal video content, so DNS-level blocking may reduce some tracking and ad-related requests but cannot reliably remove every YouTube ad without breaking content or violating service rules.

## What this project does

- Creates a local Android `VpnService` session with user consent.
- Routes DNS traffic to an in-app DNS filter.
- Blocks known ad/tracker hostnames by returning an NXDOMAIN response.
- For allowed domains, forwards DNS queries to an upstream resolver.
- Runs without root access.
- Keeps filtering local to the device; it does not collect browsing history.

## What this project does not do

- It does not bypass paid access, subscriptions, DRM, or platform protections.
- It does not modify YouTube, browsers, or third-party apps.
- It does not guarantee blocking every ad on every network or platform.
- It does not inspect encrypted app content.

## Project structure

```text
app/src/main/java/com/cleanslate/adshield/
  MainActivity.kt          Simple start/stop UI
  AdShieldVpnService.kt    Android VPN DNS-filter service
  Blocklist.kt             Domain matching rules
  DnsMessage.kt            DNS query parsing and blocked response builder
  Packet.kt                IPv4/UDP packet parser and response writer
```

## Build

1. Open the repository in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an Android device or emulator.
4. Tap **Start protection** and approve the Android VPN permission dialog.

## Development notes

This is a professional starter implementation. It focuses on a safe, transparent DNS-filtering architecture. A production release should add:

- signed blocklist updates,
- per-app allow/deny lists,
- IPv6 support,
- DNS-over-HTTPS or DNS-over-TLS upstream transport,
- automated tests for packet parsing,
- telemetry-free crash reporting or local-only diagnostics,
- Play Store privacy policy and VPN disclosure text.

## Disclaimer

Read [DISCLAIMER.md](DISCLAIMER.md) before using or distributing this project.

## License

MIT License. See [LICENSE](LICENSE).
