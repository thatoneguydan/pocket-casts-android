# Pocket Cantrips

## Purpose

Pocket Cantrips is a personal Pocket Casts Android fork focused on keeping high-frequency playback controls directly on the main player while remaining close to upstream Pocket Casts.

The custom branch is `custom-player`. The fork's `main` branch should remain close to `Automattic/pocket-casts-android:main` so upstream updates can be incorporated with minimal conflict.

## Current stage

**Stage:** v0.7 stable — home-LAN D&D playback routing physically validated

**Upstream baseline when the custom branch was created:** `bf26d7aae4378997c05712ae444af9d1b7235e42`

A known-good v0.1 rollback state is preserved on `custom-player-v0.1`. Ongoing development continues on `custom-player`.

## Main-player behavior

Speed and Trim Silence are shown directly above the stock transport controls on screens with enough vertical room for comfortable touch targets.

### Speed

- `-` decreases playback speed by `0.1x`.
- `+` increases playback speed by `0.1x`.
- Tapping the current speed cycles `1.0x -> 1.5x -> 2.0x -> 1.0x`.
- Pocket Casts' existing playback-speed limits remain in use.
- Speed minus/plus retain 56dp-wide touch areas.

### Trim Silence

Trim Silence uses one permanent four-button row:

- **Off**
- **Mild**
- **Medium**
- **Mad Max**

There is no separate explanatory/header/switch row. Each choice is directly selectable and remains 48dp high.

The controls operate on whichever effects scope is currently active: **All podcasts** or **This podcast**.

## Playback Effects sheet

The Playback Effects bottom sheet keeps:

- All podcasts / This podcast scope selector
- Volume Boost

Speed and Trim Silence are omitted from that sheet because they live on the main player.

## Density / artwork strategy

The player should prioritize podcast artwork without making controls harder to hit.

Current approach:

- Remove redundant UI before shrinking interactive elements.
- Collapse Trim Silence from a switch/header plus choices into one 48dp choice row.
- Keep the stock 80dp skip-button touch targets unchanged.
- Keep speed and trim touch targets large.
- Reduce only passive spacing where it is visually safe; the current gap between the effects card and transport controls is 18dp.
- Hide the custom effects block below 600dp screen height rather than crushing the controls on short layouts.
- Keep the custom effects card visually subordinate to the primary transport controls through restrained outlines, fills, and label contrast.

## Home-LAN D&D playback routing

The self-hosted D&D feed uses the public HTTPS hostname `dndkids.ddnsgeek.com`. On the AT&T BGW320 home network, repeated tests showed the public hairpin/NAT-loopback path spending about 15 seconds establishing TCP even though direct LAN HTTPS service from Gigachomper is effectively immediate.

Pocket Cantrips therefore applies one narrow DNS preference to the dedicated `@Player` OkHttp client:

- Target hostname: `dndkids.ddnsgeek.com`
- Preferred home-LAN address: `192.168.1.250`
- Home-LAN fingerprint: active Wi-Fi, a `192.168.1.x` device address, and default gateway `192.168.1.254`
- Fallback: normal Android/system DNS

The request URL is never rewritten. HTTPS continues to use `dndkids.ddnsgeek.com` for SNI and certificate verification. The public/system DNS result remains available behind the LAN address as a fail-safe. Away from the matched home LAN, and for every other hostname, the player client uses ordinary system DNS only.

This routing is intentionally limited to the player client. It does not modify Android DNS, Private DNS, DHCP, Wi-Fi configuration, router configuration, or Pocket Casts' feed/sync/login/artwork/discovery clients.

## Branding

- App name: **Pocket Cantrips**.
- `debugProd`, which is the installable side-by-side development build, uses Pocket Casts' normal production launcher icon rather than the alternate debug icon.
- The Android package/application ID behavior remains Pocket Casts' existing `debugProd` behavior so the fork can coexist with the Play Store app during testing.

## Design goals

- Match Pocket Casts' existing player design rather than adding an unrelated overlay.
- Keep controls comfortably thumb-friendly.
- Reuse Pocket Casts' playback-effect models, persistence, playback updates, limits, and analytics plumbing.
- Avoid modifications to playback-engine code when a narrower integration point exists.
- Keep customization concentrated in a small, reviewable file set.
- Prefer reclaiming dead/redundant space over reducing hit areas.
- Keep home-network optimization fail-safe and scoped to the exact private podcast hostname.

## Custom / modified files

### Added

- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/nowplaying/PlaybackEffectsControls.kt`
- `modules/services/servers/src/main/java/au/com/shiftyjelly/pocketcasts/servers/di/GigachomperPlaybackDns.kt`
- `modules/services/servers/src/test/kotlin/au/com/shiftyjelly/pocketcasts/servers/di/GigachomperPlaybackDnsTest.kt`
- `.github/workflows/custom-player-build.yml`
- `CUSTOM_FORK.md`
- `CUSTOM_FORK_CHANGELOG.md`

### Modified upstream files

- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/nowplaying/PlayerControls.kt`
- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/EffectsFragment.kt`
- `modules/features/player/src/main/res/layout/fragment_effects.xml`
- `modules/services/servers/src/main/java/au/com/shiftyjelly/pocketcasts/servers/di/NetworkModule.kt`
- `app/src/main/res/values/titles.xml`
- `app/build.gradle.kts`

## Build automation

Every push to `custom-player` runs the **Build Pocket Cantrips APK** GitHub Actions workflow.

The workflow builds Pocket Casts' `debugProd` variant and uploads the APK as:

`pocket-cantrips-apk`

### Installing a build

1. Open the latest successful **Build Pocket Cantrips APK** run in the repository's Actions tab.
2. Download `pocket-cantrips-apk` from the run's Artifacts section.
3. Extract the artifact ZIP.
4. Transfer/open the APK on the Android device and install it.

For development with ADB:

```text
adb install -r <apk-file>
```

## Upstream update strategy

Do not develop custom features directly on the fork's `main` branch.

For an upstream refresh:

1. Sync the fork's `main` with `Automattic/pocket-casts-android:main`.
2. Merge or rebase refreshed `main` into `custom-player`.
3. Resolve conflicts in the small custom file set above.
4. Let GitHub Actions compile the merged result before installation.
5. Test player layout, effects persistence, per-podcast/global scope switching, playback, Volume Boost, app branding, launcher icon, and the scoped D&D LAN route.

If upstream substantially redesigns the player, adapt the custom Compose component to the new player instead of preserving obsolete layout code.

## Validation status

v0.7 is physically validated on the target Android device as of 2026-08-28. The user reported the installed build works correctly, including the home-LAN playback-delay fix, with no regressions reported during validation.

Validated contract:

- App installs and launches normally.
- Existing Pocket Cantrips player controls remain functional.
- Fresh D&D playback on the known home LAN no longer pays the measured ~15 second public hairpin connection delay.
- The scoped resolver preserves the public HTTPS hostname and normal TLS validation.
- Routing remains confined to the dedicated player client and exact D&D hostname.
- Normal Android/system DNS remains the fallback away from the matched home LAN.

## Known limitations

- Adaptive layout still suppresses the custom effects block below 600dp screen height.
- The v0.7 LAN route is intentionally tied to the known home network fingerprint (`Wi-Fi + 192.168.1.x + gateway 192.168.1.254`) and the exact D&D hostname.
- The current icon fix intentionally reuses Pocket Casts' production launcher art; a distinct Pocket Cantrips icon can be designed later if desired.

## Roadmap

v0.7 validation is complete. Continue the normal upstream-sync process for future Pocket Casts updates and preserve the scoped LAN-routing contract when touching player networking.
