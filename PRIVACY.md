# Privacy Policy

Effective date: 2026-05-22

Clean Slate AdShield is a local Android DNS-filtering VPN app designed to reduce ads, trackers, analytics, and telemetry domains on the user's device.

## Summary

- The app does not sell personal data.
- The app does not upload browsing history to our servers.
- DNS filtering runs locally on the device.
- The app uses Android's VpnService permission only to route DNS traffic through the local filter.
- The app displays local protection counters such as blocked, allowed, cached, and error counts.

## Data processed on device

The app may process DNS hostnames locally to decide whether a domain should be blocked or allowed. This processing is required for the core protection feature.

The app stores local protection counters on the device, including:

- blocked DNS request count
- allowed DNS request count
- cache hit count
- error count
- loaded rule count
- last updated time

These counters are stored locally in Android SharedPreferences and are used only to show the in-app dashboard and status notification.

## Data collection

This version of the app does not collect, transmit, sell, or share personal data with the developer or third parties.

## Internet access

The app may send allowed DNS requests to configured upstream DNS resolvers so normal websites and apps can continue working. Blocked requests are answered locally.

## VPN permission

The app uses Android VpnService to create a local VPN interface. This is required so the app can receive DNS traffic and apply user-controlled filtering rules. The app does not use VPN access to inspect encrypted content inside HTTPS traffic.

## Children

The app is not designed to knowingly collect personal information from children.

## Security

The app is designed to keep filtering local to the device. No security tool can guarantee complete protection against every tracker, ad, malicious site, or privacy risk.

## Limitations

Some platforms serve ads, analytics, and normal content from shared infrastructure. DNS-level filtering may reduce ad and tracking requests but cannot guarantee blocking every ad on every platform without breaking normal content.

## Contact

For support, issues, or privacy questions, open an issue in the GitHub repository:

https://github.com/Anuttar-Jain-Code/clean-slate-adshield/issues

## Changes

This privacy policy may be updated as the app changes. Material changes should be reflected in this file before publishing an updated app build.
