# Pocket Cantrips Changelog

## v0.7-dev — 2026-08-28

Networking pass for the private D&D podcast host after reproducing a home-LAN NAT-loopback delay on the AT&T BGW320 gateway.

### Fixed

- Fresh D&D episode playback no longer has to hairpin through the public WAN address while the phone is on the known home Wi-Fi LAN.
- The dedicated Pocket Casts player OkHttp client now prefers Gigachomper at `192.168.1.250` only for `dndkids.ddnsgeek.com` when Android reports Wi-Fi on `192.168.1.x` with default gateway `192.168.1.254`.
- The original `https://dndkids.ddnsgeek.com/...` URL remains unchanged, preserving normal TLS hostname/SNI/certificate validation.
- System DNS results remain behind the LAN address as a fail-safe, and system DNS remains the only behavior away from the matched home LAN or for every other hostname.

### Measured basis

- Repeated public/hairpin connections to `76.222.77.191` spent about 15 seconds establishing TCP.
- The same HTTPS hostname forced directly to Gigachomper completed in roughly 6-24 ms.
- Public DNS returned only an IPv4 A record, ruling out IPv6 fallback as the cause.

### Preserved

- Android DNS, DHCP, Private DNS, and Wi-Fi settings are unchanged.
- No router configuration changes are required.
- Feed, sync, login, artwork, discovery, download, and other non-player OkHttp clients are unchanged.
- No URL rewriting, cleartext downgrade, certificate bypass, or custom trust behavior is introduced.
- Existing player UI, playback effects, persistence, and transport behavior are unchanged.

### Validation status

- DNS routing logic has unit coverage for home-LAN preference, off-LAN fallback, unrelated hosts, detector failure fallback, and home-network matching.
- GitHub Actions `debugProd` build run #24 passed after merge to `custom-player`.
- Physical-device validation succeeded on 2026-08-28; the user reported the installed v0.7 build works correctly and the home-LAN playback-delay fix is effective, with no regressions reported.

## v0.6-dev — 2026-08-15

Physical-device hierarchy pass focused on making the always-visible playback effects read as secondary controls beneath the primary transport controls.

### Changed

- Replaced the solid white selected Trim Silence segment with a subtle translucent highlight so it no longer competes visually with the primary play/pause button.
- Lowered unselected trim-label contrast while keeping the selected label fully legible.
- Softened the playback-effects card outline, section divider, and internal trim dividers.
- Reduced visual emphasis on the Speed icon, label, minus/plus glyphs, and current-speed value.
- Added only a very faint fill to the current-speed box and softened its outline.

### Preserved

- All v0.5 dimensions and touch targets are unchanged.
- Trim choices remain 48dp high and edge-to-edge within the card.
- Speed minus/plus remain 56dp wide with 48dp-high touch areas.
- Stock transport controls, artwork, title, seek bar, and shelf layout remain unchanged.
- Playback behavior, persistence, effect scope, and Volume Boost behavior remain untouched.

### Validation status

- Source changes applied to `custom-player` after physical validation of v0.5.
- GitHub Actions `debugProd` compile gate pending.
- Physical-device validation required for the quieter secondary-control hierarchy.

## v0.5-dev — 2026-08-15

Physical-device polish pass focused on the trim selector and transport spacing.

### Changed

- The four Trim Silence choices now visually fill the playback-effects card from edge to edge instead of sitting as a second inset outlined control.
- The effects card's own left/right/bottom outline and the divider above the trim row form the trim selector's outer outline; only the three internal segment dividers are drawn inside it.
- Selected edge segments follow the effects card's lower corner radius so the control reads as one integrated surface.
- Removed the trim row's 12dp horizontal and 4dp vertical inset padding while preserving 48dp-high trim touch targets.
- Reduced the speed row minimum height from 56dp to 52dp and the visible current-speed box from 48dp to 44dp; the minus/plus controls retain their 48dp-high touch areas.
- Increased the gap between the effects card and stock transport row from 10dp to 18dp so rewind/play/forward have more breathing room above them.

### Preserved

- Stock transport controls remain unchanged, including 80dp skip touch targets.
- Trim choices remain 48dp high.
- Speed minus/plus remain 56dp wide with 48dp-high touch areas.
- Playback behavior, persistence, effect scope, and Volume Boost behavior remain untouched.
- Artwork/title/seek/shelf layouts remain unchanged from v0.4.

### Validation status

- Source changes staged on `custom-player`.
- GitHub Actions `debugProd` compile gate pending.
- Physical-device validation required for trim-row visual integration and transport spacing balance.

## v0.4-dev — 2026-08-15

Follow-up physical-device pass based on the v0.3 player screenshot.

### Fixed

- The side-by-side `debugProd` build now overrides its own source-set label from **Pocket Debug** to **Pocket Cantrips**, fixing the launcher/home-screen name that was still winning over the main app label.
- Removed the no-ad 16dp artwork spacer. The 16dp spacing is now reserved only when an ad is actually present, allowing the portrait player to devote that height to podcast artwork.

### Preserved

- No transport or playback-effects touch target was reduced.
- Speed and Trim Silence controls remain unchanged from v0.3.
- The stock title, seek bar, transport, and shelf layouts remain unchanged.
- Ads retain their existing 16dp separation from artwork.
- Playback behavior and persistence remain untouched.

### Validation status

- Source changes applied to `custom-player` after physical validation of v0.3.
- GitHub Actions compile gate pending for v0.4.
- Physical validation required for the corrected launcher label and the larger no-ad artwork presentation.

## v0.3-dev — 2026-08-15

Player-density and branding pass focused on giving podcast artwork more room without shrinking thumb targets.

### Changed

- Renamed the forked app to **Pocket Cantrips**.
- Replaced the `debugProd` alternate/debug launcher icon with Pocket Casts' normal production launcher asset so the side-by-side test build has a proper app icon.
- Removed the dedicated Trim Silence switch/header row.
- Trim Silence is now one always-visible four-button row: **Off / Mild / Medium / Mad Max**.
- Kept all trim choices at 48dp high for comfortable thumb use.
- Reduced the passive gap between the playback-effects card and stock transport controls from 18dp to 10dp.
- Renamed the GitHub Actions workflow and installable artifact to Pocket Cantrips / `pocket-cantrips-apk`.

### Preserved

- Speed minus/plus remain 56dp-wide touch areas with a 48dp-high current-speed control.
- Stock skip and play/pause transport controls remain unchanged, including the 80dp skip touch targets.
- Playback effects persistence and global/per-podcast scope behavior.
- Existing speed limits, 0.1x stepping, and speed-value tap cycle.
- Trim Silence persistence and Mild / Medium / Mad Max behavior.
- Volume Boost remains in the Playback Effects sheet.
- Pocket Casts playback engine remains untouched.

### Validation status

- GitHub Actions compile succeeded.
- Physical-device validation confirmed the compact trim row and overall player layout.
- Follow-up screenshot exposed the `debugProd` launcher label override and additional reclaimable no-ad artwork space; those are addressed in v0.4.

## v0.2-dev — 2026-08-14

Thumb-friendliness and visual-polish pass based on the first successful physical device test.

### Preserved rollback point

- The complete working v0.1 state is preserved in the GitHub branch `custom-player-v0.1`.
- Ongoing development continues on `custom-player`.

### Changed

- Speed minus and plus controls now use wider 56dp touch areas while keeping their visual treatment simple.
- Current-speed button increased to a 48dp height with an 84dp minimum width.
- Playback effects card corner radius reduced from 16dp to 14dp.
- Divider between Speed and Trim Silence is visually softer.
- Entire Trim Silence header row toggles Trim Silence, while the switch remains functional.
- Mild / Medium / Mad Max controls receive additional horizontal space for easier thumb targeting.
- Gap between the custom playback-effects card and stock transport controls increased from 12dp to 18dp.

### Preserved

- Playback effects persistence and global/per-podcast scope behavior.
- Existing speed limits, 0.1x stepping, and speed-value tap cycle.
- Existing Trim Silence behavior and effect levels.
- Volume Boost remains in the Playback Effects sheet.
- Pocket Casts playback engine remains untouched.

### Validation status

- v0.1 compiled successfully in GitHub Actions and was physically tested successfully on Android.
- v0.2 advanced the player controls based on that physical test.

## v0.1-dev — 2026-08-14

Initial custom-player branch based on upstream commit `bf26d7aae4378997c05712ae444af9d1b7235e42`.

### Added

- Main-player playback effects control surface.
- Direct Speed minus / current-speed / plus controls.
- Direct Trim Silence switch and Mild / Medium / Mad Max controls.
- Adaptive suppression of the added effects block on screens shorter than 600dp.
- GitHub Actions `debugProd` APK build and artifact upload.
- Persistent fork handoff/roadmap documentation.

### Changed

- Main transport area places playback effects above the normal skip/play/skip controls when space allows.
- Playback Effects bottom sheet focuses on effect scope and Volume Boost.

### Preserved

- Pocket Casts playback effect persistence.
- Global versus per-podcast effect scope behavior.
- Existing playback speed limits and 0.1x stepping.
- Existing speed-value tap cycle: `1.0x -> 1.5x -> 2.0x -> 1.0x`.
- Existing Trim Silence semantics.
- Existing Volume Boost behavior.
- Existing Pocket Casts playback engine.

### Validation status

- GitHub Actions compile succeeded.
- Physical Android device test succeeded for launch, login, player rendering, speed controls, and Trim Silence controls.
