# App Store Publishing Notes

Use this file as a checklist before publishing Clean Slate AdShield.

## Short description

Clean Slate AdShield is a local Android DNS filtering VPN that helps reduce ads, trackers, analytics, and telemetry requests across apps and browsers.

## Full description draft

Clean Slate AdShield helps protect your Android device from common advertising, analytics, and tracking domains using a local DNS filtering VPN. The app runs without root access, keeps filtering local to the device, and provides a live dashboard showing blocked requests, allowed requests, cache hits, errors, and loaded rules.

The app is designed for privacy-focused DNS filtering. It does not require an account, does not collect personal information, and does not upload browsing history to the developer.

Important limitation: DNS filtering cannot guarantee that every ad on every platform will be removed. Some video and social platforms serve ads from the same infrastructure used for normal content, so overly aggressive blocking can break playback or app functionality.

## Essential store listing sections

### Data safety

Suggested answers based on the current app design:

- Personal data collected: No
- Data shared with third parties by the developer: No
- Data encrypted in transit: Not applicable for developer collection because the app is designed not to upload user data to the developer
- Data deletion request: Users can uninstall the app to remove local app data
- App functionality data: Local counters are stored on device for the dashboard

### Permissions explanation

- VPN permission: required to run the local DNS filtering VPN
- Notification permission: required to show foreground protection status
- Internet permission: required to forward allowed DNS queries
- Foreground service permission: required to keep protection running

### Content rating notes

The app is a utility/privacy tool and does not include user-generated content, gambling, violence, or adult content.

### Ads declaration

The app itself does not include in-app advertising.

### Privacy policy URL

Publish `PRIVACY_POLICY.md` on GitHub Pages or another public HTTPS page, then paste that URL into the Play Console privacy policy field.

## Pre-release checklist

- Verify the app starts and stops protection
- Verify the VPN permission prompt appears
- Verify Chrome browsing still works with protection active
- Verify blocked, allowed, cache, and errors update in the dashboard
- Verify Privacy Policy is publicly accessible by URL
- Verify screenshots show the simple UI and live dashboard
- Verify the store listing does not promise impossible blocking such as every ad on every platform
