# Drowz (Android)

A reverse alarm — at bedtime it runs a gradual wind-down (dim, warm-tint, DND, lower
volume, soundscape) and guarantees a morning restore. See [`SPEC.md`](SPEC.md) for the
full product & technical spec.

## Status

**Passes 1–3 + 5 complete — the reverse alarm works end to end.** Set a bedtime, grant the
effects you want in Setup, and at bedtime Drowz captures a baseline, ramps
dim/tint/DND/volume/grayscale down the timeline, holds overnight, and restores by morning.
**Settings → "Start wind-down now"** runs it on the spot for testing. Remaining: **Pass 4**
(soundscape audio). See [Roadmap](#roadmap).

## Architecture

Built **Android-first but KMP-ready** (the spec's KMP intent, without the iOS Gradle
wiring until iOS work actually starts ~Nov 2026). The seam is already in place:

```
domain/            ← pure Kotlin, ZERO Android imports → lifts to KMP commonMain as-is
  model/           ← SleepSettings, EffectSettings, TimeOfDay, Weekday
  winddown/        ← WindDownEngine (the heart): pure fn (elapsed, settings) → WindDownState
  effects/         ← SleepEffects interface = the platform seam (→ expect/actual on iOS)
data/
  settings/        ← SettingsRepository (interface, portable) + DataStore impl (Android)
platform/
  effects/         ← AndroidSleepEffects + one controller per subsystem (overlay, DND,
                     brightness, volume, grayscale) with persisted baseline + safe restore
ui/
  theme/           ← design system: palette, type, spacing (from the landing page)
  components/      ← NightSky (Canvas scene), glass surfaces, pill buttons, chips
  home/            ← HomeScreen + HomeViewModel + HomeUiState
di/                ← AppContainer (manual DI; explicit and build-simple)
```

**Why this shape**
- `WindDownEngine` is a *pure function* of elapsed time + settings. That makes the ritual
  deterministic, unit-testable to the second (`WindDownEngineTest`), and safe to recompute
  in a fresh process after the OS kills the service — no reliance on in-memory state.
- `SleepEffects` is the only place platform system-calls live. The engine and UI never
  touch Android system APIs, so the iOS `actual` later is an additive change, not a
  rewrite.
- The Home **preview** drives the *real* engine (time-compressed), so what you see is a
  faithful dry-run of bedtime, not a throwaway animation.

## Build & run

Requires **Android Studio (Ladybug or newer)** and JDK 17.

1. Open this folder in Android Studio; let it sync.
2. If you don't have the Gradle wrapper JAR yet, generate it once: `gradle wrapper`
   (or just let Android Studio provision Gradle — the `gradle/wrapper` config points at
   Gradle 8.9).
3. Run the `app` configuration on a device/emulator (minSdk 26).
4. Unit tests: `./gradlew :app:testDebugUnitTest`.

Versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml); if Studio flags
a newer stable AGP/Kotlin/Compose on first sync, bumping there is safe.

## Roadmap

- [x] **Pass 1** — project + design system + `WindDownEngine` + Home with live preview
- [x] **Pass 2** — real `AndroidSleepEffects`: overlay warm-tint (+ dim fallback), DND,
      brightness, volume, grayscale, with persisted baseline capture + safe restore and
      crash-recovery. Wired into the graph; *invoked* by the Pass 3 service.
- [x] **Pass 3** — scheduler (`AlarmManager` exact, `USE_EXACT_ALARM`) + foreground
      `SleepModeService` (`specialUse`): captures baseline → ramps effects → holds → restores
      at wake. Wake alarm owned by the session; boot re-arm; force-stop overdue-recovery.
- [ ] **Pass 4** — soundscape (Media3 ExoPlayer, looped, fades)
- [x] **Pass 5** — Setup (per-permission consent flows), Settings (edit bedtime/wake/days/
      effects + wind-down length), one app-scoped `DrowzViewModel`, and a "Start wind-down
      now" on-device test. Effects lacking permission are flagged, never hidden.
- [ ] **Later** — iOS via KMP/CMP (shared UI + logic; limited `actual` effects)
