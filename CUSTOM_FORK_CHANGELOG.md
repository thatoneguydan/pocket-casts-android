# Custom Fork Changelog

## v0.2-dev — 2026-08-14

Thumb-friendliness and visual-polish pass based on the first successful physical device test.

### Preserved rollback point

- The complete working v0.1 state is preserved in the GitHub branch `custom-player-v0.1` at commit `5d6a0ab4d6650fd13b51ee7d09d19b16be8cff1d`.
- Ongoing development continues on `custom-player`.

### Changed

- Speed minus and plus controls now use wider 56dp touch areas while keeping their visual treatment simple.
- Current-speed button increased to a 48dp height with an 84dp minimum width.
- Playback effects card corner radius reduced from 16dp to 14dp.
- Divider between Speed and Trim Silence is visually softer.
- Entire Trim Silence header row now toggles Trim Silence, while the existing switch remains functional.
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
- v0.2 awaits GitHub Actions compile and physical UI validation.

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

- Main transport area now places playback effects above the normal skip/play/skip controls when space allows.
- Playback Effects bottom sheet now focuses on effect scope and Volume Boost.

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
