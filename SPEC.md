# Drowz — Product & Technical Spec

**A reverse alarm.** Where an alarm wakes you up, Drowz helps you fall asleep: at a set
bedtime it nudges you to wind down and gently transforms the phone into a calm,
low-stimulation device — dimming the screen, desaturating color, silencing
interruptions, and fading in a soothing soundscape.

- **Status:** Draft spec (pre-implementation)
- **Platform:** Android first → iOS later
- **Goal:** A real, shippable product
- **Last updated:** 2026-07-03

---

## 1. Concept

At the user's chosen bedtime, Drowz runs a gradual "wind-down" ritual instead of
flipping settings all at once:

1. A gentle reminder — *"Time to wind down 🌙"*
2. Screen dims on a ramp; color is muted (warm tint by default)
3. Do Not Disturb turns on; media/ring volume drops
4. A soothing soundscape fades in, then fades out after N minutes
5. In the morning, **everything is automatically restored**

The morning restore is a first-class feature — leaving someone in DND + grayscale
at 9am is the fastest way to earn 1-star reviews.

---

## 2. Goals & non-goals

### Goals
- Reliable, scheduled bedtime trigger that works even when the app is closed / phone is idle.
- Gradual, calming transition (ramps, fades) — not an abrupt switch.
- Guaranteed morning restore of every setting Drowz touched.
- Friendly onboarding that requests each permission with clear rationale.
- A "default" experience that needs **no dangerous permissions**, plus an optional
  "power-user" mode for those who want true system grayscale.

### Non-goals (v1)
- Sleep tracking / stage detection / smart-wake.
- Cross-device sync, accounts, cloud.
- iOS (planned as a later phase — see §10).
- Replacing the OS bedtime features wholesale; Drowz differentiates on the *ritual*.

---

## 3. Feature feasibility (Android)

| Feature | Feasible on Android | Mechanism |
|---|---|---|
| Bedtime reminder | ✅ | Local notification |
| Trigger at set time, app closed / idle | ✅ | `AlarmManager.setExactAndAllowWhileIdle` + foreground service |
| Do Not Disturb | ✅ | `NotificationManager.setInterruptionFilter()` (needs `ACCESS_NOTIFICATION_POLICY`, granted once) |
| Dim brightness (system-wide) | ✅ | `Settings.System.SCREEN_BRIGHTNESS` (needs `WRITE_SETTINGS`) |
| Lower media/ring volume | ✅ | `AudioManager.setStreamVolume()` (no special permission) |
| Soothing music | ✅ | Media3 ExoPlayer, looped, with fade in/out |
| **Darken below panel minimum (default)** | ✅ | Deeper brightness dim + a neutral **dark overlay** via `SYSTEM_ALERT_WINDOW` |
| **True system grayscale (power-user)** | ⚠️ | `Settings.Secure` daltonizer — needs `WRITE_SECURE_SETTINGS`, **not** Play-grantable |

---

## 4. The grayscale decision (important)

True system-wide grayscale requires `WRITE_SECURE_SETTINGS`, which **cannot** be
granted to a normal Play Store app (only via ADB or root). So Drowz ships two paths:

### Default (everyone) — deeper dim + dark overlay
- Push system brightness toward its minimum, then lay a full-screen translucent
  **neutral (black) overlay** (`TYPE_APPLICATION_OVERLAY`) on top to go *below* the panel's
  own minimum brightness.
- Needs "Display over other apps" (overlay) + `WRITE_SETTINGS` (system brightness); the
  overlay alone still dims as a fallback if `WRITE_SETTINGS` isn't granted.
- **Neutral, not warm** — deliberately no amber tint (darker reads calmer than oranger for
  this app). The overlay is capped so the screen never goes fully black.

### Power-user — true grayscale
- Real monochrome via `Settings.Secure` accessibility daltonizer.
- Requires a one-time ADB grant:
  ```
  adb shell pm grant com.drowz.app android.permission.WRITE_SECURE_SETTINGS
  ```
- Surfaced as an optional toggle with clear instructions; hidden/disabled until the
  permission is detected.

---

## 5. Architecture

```
Scheduler (AlarmManager)
  ├─ BEDTIME alarm  ─▶ BedtimeReceiver ─▶ start SleepModeService (START)
  └─ WAKE alarm     ─▶ BedtimeReceiver ─▶ start SleepModeService (STOP/restore)
        (each firing reschedules the next day; BootReceiver reschedules after reboot)

SleepModeService (foreground, mediaPlayback)
  ├─ Orchestrator: sequenced ramps/fades reading SleepSettings
  ├─ SystemController: DND, brightness, volume, true-grayscale  (+ persists prior state)
  ├─ DimOverlay:       neutral dark overlay (dims below the panel's minimum)
  └─ SoundscapePlayer: ExoPlayer loop with fade in/out

Data
  ├─ SettingsRepository (DataStore): user preferences
  └─ Saved-state store (SharedPreferences): prior system values, for robust restore
                                            even across process death / reboot

UI (Jetpack Compose)
  └─ Onboarding + permissions + bedtime/effects settings + Preview/Restore controls
```

**Restore robustness:** before changing any system value, `SystemController` saves the
prior value to persistent storage, so the morning WAKE alarm (which may run in a fresh
process) can reverse everything correctly. If prior state is missing, fall back to sane
defaults (DND off, auto-brightness, overlay removed, grayscale off).

---

## 6. Permissions & onboarding UX

Each toggle has a *different* consent flow. Onboarding should walk through them once,
up front — never surprise the user at bedtime. Each effect degrades gracefully if its
permission is missing (the effect is skipped, not the whole routine).

| Permission | Needed for | How it's requested |
|---|---|---|
| `POST_NOTIFICATIONS` | Reminders | Runtime prompt (Android 13+) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Exact bedtime firing | `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (Android 12+) |
| `ACCESS_NOTIFICATION_POLICY` | DND | `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` |
| `WRITE_SETTINGS` | System brightness | `ACTION_MANAGE_WRITE_SETTINGS` |
| `SYSTEM_ALERT_WINDOW` | Tint overlay (default) | `ACTION_MANAGE_OVERLAY_PERMISSION` |
| `WRITE_SECURE_SETTINGS` | True grayscale (power-user) | ADB only — instructions shown in app |

*(Brightness fallback with zero permission: dim only within a full-screen overlay
inside the app, rather than system-wide.)*

---

## 7. Wind-down orchestrator (the heart of it)

On the BEDTIME trigger, the foreground service runs a **gradual** sequence rather than
flipping everything at once — this is Drowz's main differentiator vs. the built-in
abrupt Bedtime mode:

```
T-0     Gentle notification: "Time to wind down 🌙"
0–60s   Fade soundscape in; begin dimming brightness on a ramp
~30s    Enable DND; lower media/ring volume gradually
~60s    Apply the neutral dark overlay (+ true grayscale if power-user enabled)
+N min  Soundscape fades to silence (N = windDownMinutes, configurable)
morning WAKE alarm → RESTORE everything Drowz changed
```

Each effect is individually toggleable in settings.

---

## 8. Restore routine (non-negotiable)

Reverse, in order: remove overlay → disable grayscale → restore brightness (+ mode) →
restore volumes → disable DND → stop/fade music → stop foreground service.

Triggers:
- Morning WAKE alarm.
- User taps **"I'm awake — restore"** on the ongoing notification or in-app.

---

## 9. Tech stack

**Decision (locked 2026-07-03): Kotlin + Jetpack Compose, structured as a Kotlin
Multiplatform (KMP) / Compose Multiplatform (CMP) project from day one** — so iOS is an
additive step later, not a rewrite. Native Kotlin gives the most reliable Android system
control (the app's riskiest part), and Compose covers the full visual range (gradients,
Canvas, animation) needed for the night-sky look.

**Why not Flutter / React Native:** every Drowz system feature (overlay, DND, brightness,
secure-settings grayscale, exact alarms, foreground service) would become a native
platform-channel written in Kotlin *anyway* — so we'd pay the cross-platform tax on
exactly the hard parts while gaining little. KMP/CMP shares the parts that port cleanly
(UI + logic) and keeps native depth where it matters. The landing-page aesthetic is
achievable in any framework, so visuals are not the deciding axis — system control and
the iOS path are.

### Libraries / APIs
- **Jetpack Compose** — UI. Visuals via `Brush` gradients, `Canvas`/`drawBehind` (the
  celestial scene), and animation (`rememberInfiniteTransition`, `Animatable`,
  `animateFloatAsState`). Keep shared visuals in **portable Compose drawing** so they
  render identically on iOS via CMP (Skia-backed). Avoid Android-only AGSL
  `RuntimeShader` in shared code.
- **Media3 ExoPlayer** — looped soundscapes, gapless, volume fades
- **AlarmManager** (exact) + **foreground service** for reliability
- **DataStore** for preferences; **SharedPreferences** for saved system state
- `minSdk 26`, `targetSdk`/`compileSdk` latest
- Package id: `com.drowz.app` *(renamed from `com.winddown.app` to match the brand)*

### Module structure (KMP)
- `commonMain` — shared UI (Compose/CMP), design system, navigation, settings/state, the
  wind-down orchestration state machine, bedtime logic.
- `androidMain` — the real system effects (overlay, DND, brightness, grayscale, alarms, FGS).
- `iosMain` — stub for now; the limited iOS effects later.
- A thin `expect`/`actual` **`SleepEffects`** interface is the seam between the shared UI
  and platform-specific system calls.

### What is / isn't shareable to iOS
| Layer | Shared? |
|---|---|
| UI, design system, night-sky visuals, animations | ✅ shared (CMP) |
| Scheduling logic, settings, wind-down state machine | ✅ shared |
| System effects (overlay, DND, brightness, grayscale, alarms, FGS) | ⚠️ `expect`/`actual` — Android full, iOS limited |

### Practical notes
- Building/shipping the iOS target needs a **Mac + Xcode** (KMP compiles Kotlin to an iOS
  framework; packaging and signing go through Xcode).
- iOS `actual` effects do *less* — an Apple limitation, not a framework one (see §10).

---

## 10. iOS (later phase)

Delivered as the **`iosMain` target of the same KMP/CMP project** — the shared UI and
logic come across for free; only the effects layer is new (and limited).

iOS cannot control system grayscale / DND / system brightness programmatically. Planned
approach when we get there:
- Shared Compose UI (via CMP) + local notifications + in-app dimming
  (`UIScreen.brightness` while foregrounded).
- System-level effects delivered via a **Shortcuts automation** the app helps the user
  install (Set Focus, Color Filters, brightness, play audio on a bedtime trigger).
- Or lean on Apple's built-in **Sleep Focus + Wind Down**.
- Requires a **Mac + Xcode** to build and ship.

---

## 11. MVP scope & differentiation

**v1 includes:** bedtime scheduler, gradual wind-down orchestrator, DND, dim, volume,
warm-tint default + power-user grayscale, one bundled soundscape with fade, guaranteed
restore, onboarding/permissions flow, Preview/Restore controls.

**Cut from v1:** multiple soundscapes/library, sleep tracking, smart wake, themes,
accounts/sync, widgets.

**Edge over Digital Wellbeing Bedtime mode:** the *gradual ritual* (ramps + fades),
curated soundscape with fade, calmer visual design, and clear per-effect control.

---

## 12. Emotional engagement & retention (locked 2026-07-04 — build A · B · D · F; no companion)

Guiding principle: **a sleep app must remove pressure, not add it.** Mechanics that work
for daytime, voluntary habits (Forest's dying tree, brittle streaks, guilt-y mascots)
backfire at bedtime, where stress directly harms the goal. So Drowz's rules:

- **Reward the action the user controls (starting the wind-down), never the outcome they
  don't (actually falling asleep).**
- **Additive, never punitive** — progress accumulates and gently goes *dormant*; nothing
  dies, nothing resets to zero, no loss-aversion streaks.
- The reward for good sleep should itself be **calm**.

### Proposed system (one integrated loop)
- **A growing night-world** *(the core loop)* — the city/scene gets cozier each night
  you wind down (a new building, a lit street, a boat); goes *quiet* (dimmer) when you're
  away and revives with a single wind-down. The Forest analog, reusing the celestial scene.
- **Constellations** — each wind-down adds a star; over weeks they connect into named
  constellations you discover, each with a line of calm lore.
- **Forgiving consistency** — a moon-phase calendar of "nights you chose to rest," plus a
  gentle weekly "wind-down recap" (reflection, not a scoreboard). Built-in rest days; no
  streak that can "break."
- **Cosmetic unlocks** — consistency unlocks new scenes (beach, forest, desert, snow),
  soundscapes, and companion looks. Collectible, calming, keeps the app fresh over months.
**Considered & cut — a sleepy companion (mascot):** a creature you "tuck in" each night.
Cut because caring for a companion adds *responsibility stress* — the exact bedtime
pressure the guiding principle rules out. The growing world (A) delivers the same
"something is thriving because of me" warmth without anything *depending* on the user.

**Explicitly rejected:** the companion (above), hard day-count streaks, competitive
leaderboards, and guilt/loss-aversion notifications — all raise bedtime anxiety.

**Reward the controllable signal:** "wound down tonight" = the user started the ritual
(a real, logged event from the wind-down engine) — not any guess about whether they slept.

---

## 13. Open questions

- **Retention:** which of A·B·D·F ship in v1 vs. later (a lean v1 could be the growing
  world + moon calendar, with constellations + unlocks following)? Is progression purely
  cosmetic, or ever gated behind anything?
- Bundle our own royalty-free soundscape(s), or ship silent v1 + let users pick a track?
- Should "warm tint" intensity be user-adjustable (slider) in v1?
- Weekend vs. weekday bedtimes, or a single schedule for v1?
- How aggressive should default dimming be (target brightness)?
