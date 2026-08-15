# Pocket Cantrips

## Purpose

Pocket Cantrips is a personal Pocket Casts Android fork focused on keeping high-frequency playback controls directly on the main player while remaining close to upstream Pocket Casts.

The custom branch is `custom-player`. The fork's `main` branch should remain close to `Automattic/pocket-casts-android:main` so upstream updates can be incorporated with minimal conflict.

## Current stage

**Stage:** v0.3 player-density / branding validation

**Upstream baseline when the custom branch was created:** `bf26d7aae4378997c05712ae444af9d1b7235e42`

A known-good v0.1 rollback state is preserved on `custom-player-v0.1`. Ongoing development continues on `custom-player`.

## Main-player behavior

Speed and Trim Silence are shown directly above the stock transport controls on screens with enough vertical room for comfortable touch targets.

### Speed

- `-` decreases playback speed by `0.1x`.
- `+` increases playback speed by `0.1x`.
- Tapping the current speed cycles `1.0x -> 1.5x -> 2.0x -> 1.0x`.
- Pocket Casts' existing playback-speed limits remain in use.
- Speed minus/plus retain 56dp-wide touch areas; the current-speed control remains 48dp high with an 84dp minimum width.

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
- Reduce only passive spacing where it is visually safe; the gap between the effects card and transport controls is currently 10dp.
- Hide the custom effects block below 600dp screen height rather than crushing the controls on short layouts.

## Branding

- App name: **Pocket Cantrips**.
- `debugProd`, which is the installable side-by-side development build, uses Pocket Casts' normal production launcher icon rather than the alternate debug icon.
- The Android package/application ID behavior remains Pocket Casts' existing `debugProd` behavior so the fork can coexist with the Play Store app during testing.

## Design goals

- Match Pocket Casts' existing player design rather than adding an unrelated overlay.
- Keep controls comfortably thumb-friendly.
- Reuse Pocket Casts' playback-effect models, persistence, playback updates, limits, and analytics plumbing.
- Avoid modifications to playback-engine code.
- Keep customization concentrated in a small, reviewable file set.
- Prefer reclaiming dead/redundant space over reducing hit areas.

## Custom / modified files

### Added

- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/nowplaying/PlaybackEffectsControls.kt`
- `.github/workflows/custom-player-build.yml`
- `CUSTOM_FORK.md`
- `CUSTOM_FORK_CHANGELOG.md`

### Modified upstream files

- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/nowplaying/PlayerControls.kt`
- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/EffectsFragment.kt`
- `modules/features/player/src/main/res/layout/fragment_effects.xml`
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
5. Test player layout, effects persistence, per-podcast/global scope switching, playback, Volume Boost, app branding, and launcher icon.

If upstream substantially redesigns the player, adapt the custom Compose component to the new player instead of preserving obsolete layout code.

## Validation checklist

Before treating v0.3 as stable:

- App installs and launches normally.
- App launcher label is Pocket Cantrips.
- Launcher icon renders correctly.
- Official Pocket Casts can remain installed side-by-side.
- Main player shows the custom Speed controls in normal portrait use.
- Trim row shows Off / Mild / Medium / Mad Max at all times.
- Off disables Trim Silence immediately.
- Mild / Medium / Mad Max update playback immediately.
- Trim selection persists and follows All podcasts / This podcast scope.
- Speed minus changes by `0.1x`.
- Speed plus changes by `0.1x`.
- Speed value cycles `1.0 -> 1.5 -> 2.0 -> 1.0`.
- Artwork is visibly larger / has more breathing room than the prior build.
- Stock skip/play/skip controls remain comfortable to hit.
- Playback Effects sheet contains the scope selector and Volume Boost, but not Speed or Trim Silence.
- Volume Boost still works.
- Player remains usable on smaller screens and landscape layouts.
- Downloads, sync, queue, bookmarks, Android media controls, and ordinary playback behave normally.

## Known limitations / open validation

- Adaptive layout still suppresses the custom effects block below 600dp screen height.
- The v0.3 density and branding changes require physical-device validation after the GitHub Actions compile gate succeeds.
- The current icon fix intentionally reuses Pocket Casts' production launcher art; a distinct Pocket Cantrips icon can be designed later if desired.

## Roadmap

1. Pass the v0.3 GitHub Actions compile gate.
2. Install on the target phone and validate artwork size, four-button trim fit, name, and launcher icon.
3. Fine-tune passive vertical spacing from an on-device screenshot if useful.
4. Validate tablet/landscape behavior.
5. Preserve v0.3 as a rollback branch once physical validation is successful.
6. Continue using the upstream-sync procedure for future Pocket Casts updates.
