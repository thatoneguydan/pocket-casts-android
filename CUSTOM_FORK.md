# Pocket Casts Custom Player Fork

## Purpose

This fork keeps Pocket Casts as close to upstream as practical while relocating the high-frequency playback effects controls to the main player.

The custom branch is `custom-player`. The fork's `main` branch should remain close to `Automattic/pocket-casts-android:main` so upstream updates can be brought in with minimal conflict.

## Current stage

**Stage:** first functional Android build / UI validation

**Upstream baseline when the custom branch was created:** `bf26d7aae4378997c05712ae444af9d1b7235e42`

The initial implementation is intentionally limited to the player module plus build/continuity files.

## Required behavior

### Main player

Speed and Trim Silence are shown directly above the normal transport controls on screens with enough vertical room for comfortable touch targets.

Speed retains Pocket Casts' existing behavior:

- `-` decreases playback speed by `0.1x`.
- `+` increases playback speed by `0.1x`.
- Tapping the current speed cycles `1.0x -> 1.5x -> 2.0x -> 1.0x`.
- Pocket Casts' existing `0.5x` to `5.0x` clamping remains in use.

Trim Silence retains Pocket Casts' existing behavior:

- A switch enables/disables Trim Silence.
- Enabling from Off starts at Mild.
- Mild / Medium / Mad Max remain one-tap touch controls while Trim Silence is enabled.

The controls operate on whichever effects scope is currently active: **All podcasts** or **This podcast**.

### Playback Effects sheet

The Playback Effects bottom sheet keeps:

- All podcasts / This podcast scope selector
- Volume Boost

Speed and Trim Silence are removed from this sheet because they live on the main player.

## Design goals

- Match Pocket Casts' existing player design rather than adding an unrelated overlay.
- Use large touch targets.
- Reuse existing playback effect models, persistence, playback updating, limits, and analytics behavior.
- Avoid modifications to playback-engine code.
- Keep customization concentrated in the player feature module.
- Hide the added block on cramped-height layouts instead of making transport controls unusably small.

## Custom files / modified upstream files

### Added

- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/nowplaying/PlaybackEffectsControls.kt`
- `.github/workflows/custom-player-build.yml`
- `CUSTOM_FORK.md`
- `CUSTOM_FORK_CHANGELOG.md`

### Modified

- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/nowplaying/PlayerControls.kt`
- `modules/features/player/src/main/java/au/com/shiftyjelly/pocketcasts/player/view/EffectsFragment.kt`
- `modules/features/player/src/main/res/layout/fragment_effects.xml`

## Build automation

Every push to `custom-player` runs the **Build custom player APK** GitHub Actions workflow.

The workflow builds Pocket Casts' `debugProd` variant and uploads the resulting APK as the artifact:

`pocket-casts-custom-player-apk`

`debugProd` uses the fork's debug application ID suffix, allowing the custom build to coexist with the normal Play Store Pocket Casts installation during testing.

### Installing a build

1. Open the latest successful **Build custom player APK** run in the repository's Actions tab.
2. Download `pocket-casts-custom-player-apk` from the run's Artifacts section.
3. Extract the artifact ZIP.
4. Transfer/open the APK on the Android device and install it.

For development with ADB, the extracted APK can instead be installed with:

```text
adb install -r <apk-file>
```

## Upstream update strategy

Do not develop custom features directly on the fork's `main` branch.

For an upstream refresh:

1. Sync the fork's `main` with `Automattic/pocket-casts-android:main`.
2. Merge or rebase the refreshed `main` into `custom-player`.
3. Resolve conflicts only in the small custom file set above.
4. Let the GitHub Action compile the merged result before installing it.
5. Test main-player layout, effects persistence, per-podcast/global scope switching, playback, and Volume Boost.

If upstream substantially redesigns the player, prefer adapting the custom Compose component to the new player rather than preserving old layout code.

## Validation checklist

Before treating a build as stable:

- App launches and signs in normally.
- Official Pocket Casts can remain installed side-by-side during testing.
- Main player shows Speed and Trim Silence in normal portrait use.
- Speed minus changes by `0.1x`.
- Speed plus changes by `0.1x`.
- Speed value cycles `1.0 -> 1.5 -> 2.0 -> 1.0`.
- Speed cannot exceed Pocket Casts' normal limits.
- Trim switch enables Mild when previously Off.
- Mild / Medium / Mad Max update playback immediately.
- Disabling Trim Silence returns it to Off.
- All podcasts / This podcast scope changes are reflected by main-player controls.
- Playback Effects sheet contains the scope selector and Volume Boost, but not Speed or Trim Silence.
- Volume Boost still works.
- Player remains usable on smaller screens and landscape layouts.
- Downloads, sync, queue, bookmarks, Android media controls, and ordinary playback still behave normally.

## Known limitations / open validation

- The initial custom build has not yet been physically tested on the target phone.
- Adaptive layout currently suppresses the custom effects block when screen height is below 600dp to avoid crowding on short landscape layouts.
- GitHub Actions is the compile gate for the first build. Any compiler error should be fixed in the branch before device testing.

## Roadmap

1. Get the first GitHub Actions build green.
2. Install side-by-side on the target phone.
3. Validate actual spacing, artwork sizing, scrolling, touch targets, and effect behavior.
4. Adjust visual density based on screenshots from the device.
5. Validate tablet/landscape behavior.
6. Establish a repeatable upstream-sync procedure after the first Pocket Casts upstream update.
7. Once stable, optionally add a simpler one-command/latest-build installation workflow.
