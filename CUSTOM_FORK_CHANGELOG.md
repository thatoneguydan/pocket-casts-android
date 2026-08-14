# Custom Fork Changelog

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

Awaiting first successful GitHub Actions compile and physical Android device test.
