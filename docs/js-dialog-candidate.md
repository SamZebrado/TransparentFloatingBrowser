# Candidate fix: heartbeat address dialog in the floating WebView

Status: candidate solution, **not tested on an Android device or emulator**.
Build success and JVM/mocked webpage tests do not establish that Android touch,
keyboard, or overlay behavior is fixed on a physical device. Keep this change in
a draft PR until device validation is recorded.

## Known bug and evidence

The default NewMarsHeartBeat page binds two `click` events less than 300 ms apart
on the red heart-rate number and calls JavaScript `prompt()` to configure the
WebSocket address. On confirmation it reconnects, writes `localStorage`, and
calls `alert()`. This entry uses native JavaScript dialogs, not a DOM dialog.

The floating service creates the WebView with a Service context. Its existing
WebChromeClient does not handle these JavaScript dialogs. Android's default
`JsDialogHelper` requires an Activity context and otherwise cancels the result:
[Android 14 AOSP source](https://android.googlesource.com/platform/frameworks/base/+/android-14.0.0_r1/core/java/android/webkit/JsDialogHelper.java).
The drag and resize touch listeners belong to their handle views, rather than
the webpage number or the whole container. Actual Android touch delivery still
needs device validation.

## Candidate behavior

Handle `prompt`, `alert`, and `confirm` through themed, focusable
`TYPE_APPLICATION_OVERLAY` dialogs using the already required overlay
permission. Show the requesting page's origin in the title. Preserve the
prompt's default text and return the entered text or cancellation to JavaScript.
Resolve results once even when button, dismissal, and lifecycle callbacks overlap.

Cancel pending dialogs when the owner is hidden/detached, navigated, destroyed,
or switched to DISPLAY mode. A failed dialog show/dismiss cancels safely.
Leave the WebView window's drag/resize and touch-pass-through flags unchanged.
This change adds no permission, JavaScript bridge, or network-policy bypass.

## Validation and boundaries

Use the project's existing JDK 17, Gradle 8.4, AGP 8.2.1, Kotlin plugin 1.9.22,
and Android SDK/Build Tools 34. Configure your own local SDK/JDK locations.

```sh
./gradlew --offline --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
node scripts/tests/heartbeat-flow-test.cjs ../NewMarsHeartBeat/index.html
```

The Node check requires a separate checkout of
[`SamZebrado/NewMarsHeartBeat`](https://github.com/SamZebrado/NewMarsHeartBeat).
It executes the real page function excerpts with mocked DOM, clock, dialogs, and
WebSocket adapters. It checks double-click timing, cancel/blank input, valid
`ws`/`wss` strings, repeated opening, saved-address restoration, and the existing
invalid-address boundary. It does not test WebView input dispatch or live networking.
The three JVM tests exercise single-result completion, cancellation versus late
confirmation, and independent repeated prompts; they do not instantiate Android UI.

On 2026-10-11 the isolated candidate based on the current remote `main` passed
the offline command above: three JVM tests, debug APK assembly, and lint
(`0 errors, 44 warnings`). The webpage check passed eight assertions against
NewMarsHeartBeat revision `8733ab98604bcd7cd5fba9da9d82de815f6d8d6d`.
The compiler reports the `SOFT_INPUT_ADJUST_RESIZE` deprecation; it remains in
use for the dialog's keyboard handling on the supported API 27+ range.

The original local working tree also contains unrelated uncommitted camera /
microphone service changes. Its lint run failed with
`PermissionImpliesUnsupportedChromeOsHardware`: a CAMERA permission lacks a
corresponding optional camera hardware declaration. Those changes are preserved
locally and excluded from this branch; that failure must not be described as a
pass or suppressed. The isolated branch's validation result is recorded in the PR.

The heartbeat webpage currently saves any non-empty address before the
WebSocket constructor validates it. A malformed address can therefore be saved
and then throw; cancel and whitespace-only input leave the previous value alone.
This separate webpage issue is not changed here. HTTP/HTTPS and `ws`/`wss`
network policy, reachable server status, and connection success are separate
from displaying the settings dialog.

The previously supplied debug test APK was built from the original local working
tree including its pre-existing changes. It is not claimed to be a byte-identical
build of this isolated PR branch. No APK, local build output, signing key,
or local environment file is added by this commit.

## Pending Android acceptance checks

1. Grant the normal overlay permission, open the heartbeat page, switch to
   **EDIT** mode, and double-tap the **red number**. Verify one prompt, populated
   default address, visible keyboard, and usable text editing.
2. Cancel with the button and Back; reopen and verify the address did not change.
   Confirm a reachable valid `ws://` or `wss://` address and verify the saved
   alert, reconnect, repeated opening, and page/service restart restoration.
3. Check whitespace-only input and the known malformed-address limitation;
   do not interpret an address-saved alert as proof of network connectivity.
4. Close/hide the owner, navigate, stop the service, or switch to DISPLAY during
   a prompt. Verify cancellation, no leaked dialog, no crash, and no stalled JS.
5. Verify drag/resize and zoom in EDIT, and existing pass-through in DISPLAY.
   DISPLAY intentionally does not accept webpage taps. Test multiple windows,
   overlay-permission revocation, IME/Back behavior, and the target Android/OEM
   version before considering the candidate verified.
