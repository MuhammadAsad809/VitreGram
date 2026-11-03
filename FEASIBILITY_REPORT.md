# VitreGram — Instagram Integration Feasibility Report

**Document Version:** 1.0  
**Target Platform:** Android TV & Google TV  
**Application Architecture:** Clean Architecture (MVVM + Multi-Adapter Pattern)  

---

## 1. Executive Summary

This report evaluates technical feasibility, authentication pathways, security constraints, and platform limitations for **VitreGram**, a third-party Instagram client built for Android TV.

Instagram does not offer an open, consumer-facing public REST API for standard personal feeds. This document outlines the adapter design implemented in VitreGram to deliver real content consumption on TVs while adhering strictly to security standards.

---

## 2. Integration Method Analysis

| Method | Feasibility | Strengths | Limitations / Showstoppers |
|---|---|---|---|
| **Official Instagram Graph API** | Partial (Professional only) | Stable, Meta-sanctioned, official OAuth2 | Requires business/creator accounts, Meta App Review, cannot read personal friends' home feeds. |
| **Instagram Basic Display API** | **Retired** | Previously allowed personal media | **Officially shut down by Meta in Dec 2024.** Not viable. |
| **Authenticated Web Session Adapter** | **High (Implemented in VitreGram)** | Delivers authentic personal home feeds, vertical Reels, Stories, Search | Subject to session invalidation and rate limits; requires secure cookie-based session passing. |
| **Direct Web Scraping without Auth** | Infeasible | None | Blocked by anti-bot challenge walls, CAPTCHAs, and zero personalization. |

---

## 3. Implemented Architecture: Multi-Adapter Pattern

VitreGram isolates integration strategies behind a unified domain repository interface (`InstagramRepository`):

```
                        InstagramRepository
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
 OfficialApiAdapter      WebSessionAdapter      UnsupportedFeatureHandler
 (Graph API for pages)   (Timeline, Reels, TV)  (Explicit UI error banner)
```

### Key Adapter Responsibilities:
1. **`WebSessionAdapter`**:
   - Communicates with Instagram web endpoints using authentic session cookies (`sessionid`, `ds_user_id`, `csrftoken`).
   - Uses mobile TV user agent headers and Meta's official web client application ID (`X-IG-App-ID: 936619743392459`).
   - Parses Timeline, Reels Tray, and Search endpoints without hardcoded secrets.
2. **`UnsupportedFeatureHandler`**:
   - Explicitly returns `NetworkResult.Unsupported` for actions that Instagram does not expose (such as Live Video Broadcasting and WebRTC Voice/Video Calling).
   - Guarantees compliance with PRD Section 3: *“Features must be marked unavailable when Instagram's supported interfaces do not expose them. Do not simulate successful actions.”*

---

## 4. Security & TV Authentication UX

1. **TV-Optimized WebView Cookie Extractor**:
   - Manually entering 50+ character session cookies on a television remote control is error-prone and impractical for typical users.
   - VitreGram implements `InstagramTvWebViewLogin`: an embedded, remote-navigable Android TV WebView that loads the official Instagram login interface with D-pad navigation highlights (`#E1306C` glow).
   - Once the user logs in (supporting username/password, Facebook login, 2FA, and checkpoint challenges), Android's `CookieManager` detects the issuance of `sessionid` and `ds_user_id`.
   - The cookies are automatically extracted, flushed, and routed directly into the encrypted Keystore session manager without requiring any manual cookie copy-pasting.

2. **Zero Plaintext Passwords**:
   - VitreGram never solicits, collects, or logs Instagram passwords directly in native code.
   - Authentication happens exclusively through Instagram's own web layer.
3. **Hardware-Backed Encryption**:
   - All session credentials, cookies, and tokens are stored using `androidx.security.crypto.EncryptedSharedPreferences` backed by the **Android Keystore (AES-256 SIV + AES-256 GCM)**.
4. **Logout & Remote Clearing**:
   - Triggering "Log Out" purges all Keystore secrets, clears WebView cookies via `CookieManager.removeAllCookies()`, and wipes Room database caches immediately.

---

## 5. Feature Feasibility Matrix

| Feature | Status in VitreGram | Implementation Details |
|---|---|---|
| **Home Timeline Feed** | Supported | Retrieved via `WebSessionAdapter`, cached in Room `PostEntity`. |
| **Reels Player** | Supported | Streamed via Media3 ExoPlayer with hardware acceleration and 9:16 layout. |
| **Stories Tray & Playback** | Supported | Retrieved via `feed/reels_tray/`, timed progression bar. |
| **Search Creators & Content** | Supported | Debounced query against blended search API. |
| **Explore Recommendations** | Supported | Grid-rendered explore recommendations. |
| **Like / Unlike** | Supported | Authenticated POST with CSRF protection token. |
| **Direct Messaging** | Marked Unavailable | End-to-end encryption keys are not accessible in web sessions. |
| **Live Broadcast / Calls** | Marked Unavailable | Proprietary WebRTC and RTMP ingest restricted by Meta. |

---

## 6. Recommendations for Ongoing Maintenance

- **Monitor Session Invalidation**: Implement graceful prompts directing users to refresh their TV session if Instagram flags a checkpoint.
- **TV Media Preloading**: Media3 ExoPlayer pre-caches only the immediate next Reel to stay within Android TV memory budgets (<200MB memory footprint).
