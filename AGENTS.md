# Q Scan — Technical Development Guidelines

> 🚨 **100% MANDATORY STRICT CODE FREEZE & ZERO-UNSOLICITED MODIFICATION RULE:**
> **Code preservation policy for any AI model, agent (Gemini, Claude, Cursor, Copilot, ChatGPT, etc.), or developer:**
> 1. **Zero Unprompted Modifications:** Opening this project or file does NOT grant permission to automatically modify, update, format, or refactor any code, file structure, UI design, logic, function, or dependency.
> 2. **Exact State Preservation:** The app must be opened and run in its exact existing state. Introducing "suggested enhancements", "unnecessary refactoring", or "unsolicited package updates" is strictly forbidden.
> 3. **Explicit User Command Only:** Modifications are permitted strictly and exclusively when the user provides an explicit instruction in chat for a specific task. Do not touch any file outside the designated scope of the user's command.
> 4. **Ultra-Fast Screen Detection & Instant Auto-Trade Dispatch:** As per the user's permanent mandatory instruction, screen detection must always operate at the fastest possible rate with minimum latency, and trades must be dispatched immediately upon detection without unnecessary delay or artificial cooldowns.
> 5. **Read Complete Architectural Map First:** Before answering or doing any task, thoroughly read `APP_ARCHITECTURE_AND_SYSTEM_MAP.md` which contains the complete guide to every single module, file, algorithm, UI component, and quantitative metric (e.g., Tick Velocity Power 3X/3X, 50/50 Candle Split Clock, WebSocket Relay, Self-Healing Watchdogs).
> 6. **Automatic Documentation & Legacy Removal on Updates:** Whenever the user requests any feature, component, metric, or UI update, immediately remove the legacy code/docs and register/document the new state in `APP_ARCHITECTURE_AND_SYSTEM_MAP.md` automatically without requiring the user to give a separate reminder.
> 7. **Mandatory Instant Revert / Rollback on Demand:** If any function, design, UI element, or algorithm is updated and the user does not like the change or asks for the previous version back ("আগেরটা ফিরিয়ে দাও" / "পছন্দ হয় নাই"), the assistant MUST immediately and cleanly restore the exact previous implementation without hesitation, and re-synchronize all documentation accordingly.
> 8. **Mandatory Command Evaluation, Risk Filtering & Constructive Advisory Protocol:**
>    - **Internal Harm Evaluation:** On every user instruction, internally analyze if the command could cause any harm, regression, memory leak, camera stutter, latency increase, or stability loss to the app.
>    - **Alert on Risk:** If any harmful impact or side-effect is identified, do NOT apply it blindly; immediately inform and warn the user explaining the exact technical risk.
>    - **Proactive Improvement Suggestion:** If an update or a superior, safer, or more efficient alternative is identified, actively notify and advise the user.
>    - **Direct Flawless Execution When 100% Safe:** If the command has zero harmful side effects, causes no degradation, and needs no alteration, execute it directly, swiftly, and cleanly without unnecessary friction or delay.

**Objective:** This document serves as a mandatory technical reference for AI Studio and the development team. All rules are written specifically to prevent recurring issues (hardcoded API keys, camera re-bind loops, bitmap memory leaks). No step is optional — all requirements must be met before building or merging. For the full system map, see `APP_ARCHITECTURE_AND_SYSTEM_MAP.md`.

---

## 1. Architecture & Tech Stack

**Mandatory Stack:**
- UI: **Jetpack Compose** (Material 3) — Do not introduce XML layouts or View-based UI.
- Camera: **CameraX** (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) — Direct use of legacy `Camera1` or raw `Camera2` APIs is prohibited.
- Async/Concurrency: **Kotlin Coroutines + Flow** (`StateFlow`, `viewModelScope`) — Do not use `AsyncTask`, raw `Thread`, or `Handler`-based async code.
- Networking: **OkHttp + Moshi** — Do not add Gson or deprecated JSON libraries.
- Dependency Versions: Manage exclusively via `gradle/libs.versions.toml` (Version Catalog); never write hardcoded version strings directly in `build.gradle.kts`.
- Use **stable (non-alpha/beta)** dependency versions unless explicitly authorized.

**Forbidden Practices:**
- Deprecated or unmaintained third-party libraries (no updates for 2+ years).
- Lowering `minSdk` or `targetSdk`.
- Non-null assertion (`!!`) in Kotlin — use safe calls `?.let`, `requireNotNull()`, or explicit safe defaults.

---

## 2. Security & API Key Management

> ⚠️ **Reference Incident:** In previous reviews, an active Gemini API key was found hardcoded in source code. This is treated as a critical severity issue.

**Mandatory Rules:**
1. No API keys, secrets, or tokens may exist as literal strings in source code (Kotlin files, XML, comments) — not even as fallback or placeholder values.
2. Keys must only be loaded via **Secrets Gradle Plugin + `.env`** via `BuildConfig.GEMINI_API_KEY`. The `.env` file must remain in `.gitignore`.
3. If a key is missing or blank, the app must not silently fall back to an embedded key; it must display an explicit UI prompt asking the user to configure their key (`ApiKeyDialog` pattern).
4. Full API keys must never be logged to Logcat — use masked format only (e.g., `AIza...w3u8`).
5. **Production Warning:** Remember that keys in `BuildConfig` can be retrieved via reverse-engineering (`apktool`/`jadx`). For production releases, a lightweight proxy backend is recommended.
6. If any key is exposed in chat logs, screenshots, or git history, revoke and regenerate it immediately.

**Definition of Done Check:**
```
grep -rn "AIza" app/src/main/java/   # Must return 0 hits
```

---

## 3. Memory & Lifecycle Optimization

### 3.1 Bitmap Handling
- Any function creating a **new** `Bitmap` (`Bitmap.createScaledBitmap`, `Bitmap.createBitmap`, etc.) must recycle it using `try { } finally { bitmap.recycle() }`.
- Never recycle caller-owned or system-owned bitmaps (`PreviewView.bitmap`, CameraX internal buffers).
- Always verify `!bitmap.isRecycled` before recycling.
- Reuse buffers/pools where possible to minimize allocations.

### 3.2 CameraPreviewView Lifecycle
- Camera rebind must **only** occur when the lens facing (`selectedCameraLens`) has actually changed, guarded by a remembered state comparison.
```kotlin
update = { pView ->
    if (boundLensFacing == uiState.selectedCameraLens) return@AndroidView
    // rebind logic...
}
```
- When Composable is disposed (`DisposableEffect(Unit) { onDispose { ... } }`), call `cameraProvider.unbindAll()` to release camera hardware.
- Avoid passing the entire `uiState` as a key to `LaunchedEffect`; use specific fields (`uiState.isTorchOn`, `uiState.selectedCameraLens`).
- Maintain explicit `Job` references (`scanLoopJob`, `cooldownJob`) in `MainViewModel` and cancel them properly.
- Use `Mutex`/`tryLock()` (`apiMutex`) to prevent overlapping asynchronous processing calls.

---

## 4. Error Handling & Quota Protection

### 4.1 CancellationException
- In any `try/catch (e: Exception)` block, check and re-throw `CancellationException` first:
```kotlin
catch (e: Exception) {
    if (e is kotlinx.coroutines.CancellationException) throw e
    // handle actual errors here
}
```

### 4.2 HTTP 429 (Quota Exceeded) Handling
- Detect 429 directly via HTTP status code (`response.code == 429`), not relying solely on error message string parsing.
- Use dynamic cooldown if a retry delay header is provided, or a safe default bound (15-25 seconds).
- Scanning must pause during cooldown and auto-resume when completed, without overriding manual pauses requested by the user.

### 4.3 Scan Interval & Ultra-Fast Detection
- Under user instructions: Default `scanIntervalMs` is set to ultra-fast **20ms (0.02s / Max Hardware 50 FPS)** for on-device OCR detection, with local backoff optimized to 1ms.
- Frame guards and backoff prevent spin-loops when camera buffers are unready or in cloud mode.
- In `ApiKeyDialog`, streamlined interval options include: 20ms (50 FPS • Ultra), 50ms (20 FPS • Fast), 100ms (10 FPS • Balanced), and 250ms (4 FPS • Economy).
- Deterministic decision matrices (M001-M165) and 5m/60m/1D confluence calculations provide real-time quantitative validation.

---

## 5. Final Definition-of-Done Checklist

- [ ] `grep -rn "AIza\|sk-\|api_key\s*=\s*\"" app/src/main/java/` → 0 hits
- [ ] `.env` is in `.gitignore` and never committed
- [ ] Every newly created bitmap has a corresponding `try/finally { recycle() }`
- [ ] CameraPreviewView rebinds only on lens facing change
- [ ] `DisposableEffect` calls `unbindAll()`
- [ ] All `catch (e: Exception)` blocks re-throw `CancellationException`
- [ ] 429 detection is based on HTTP status code
- [ ] Default scan interval is synchronized across ViewModel, UI dialog, and documentation
