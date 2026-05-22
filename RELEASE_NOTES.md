# Clean Slate AdShield Release Notes

## v1.0.0

First working debug APK build for testing.

### Included

- Local Android DNS filtering VPN
- Simple Start, Stop, and Reload controls
- Live dashboard for Blocked, Allowed, Cache, Errors, Total, and Rules
- Privacy Shield and Strict Mode toggles
- In-app three-dot menu with Privacy Policy, Contact Us, and About
- Privacy policy with contact email: anuttar209@gmail.com

### Important limitation

DNS filtering can reduce ads, trackers, analytics, and telemetry requests, but it cannot guarantee removal of every advertisement on every platform. Some video platforms serve ads and normal content from shared infrastructure.

### Testing APK

The GitHub Actions workflow builds the debug APK and uploads it as an artifact named `clean-slate-adshield-debug-apk`.
