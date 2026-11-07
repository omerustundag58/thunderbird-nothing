# Thunderbird for Android — Nothing OS edition (unofficial)

An unofficial fork of [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android)
with a Nothing OS inspired visual language and a fully on-device AI assistant.

Not affiliated with, endorsed by, or supported by MZLA Technologies Corporation,
the Thunderbird project, or Nothing Technology Limited.

## What is different

### Nothing OS theme
- True black (`#000000`) surfaces in dark mode for AMOLED displays.
- Fixed monochrome palette with a single accent: Nothing red `#D71921`.
- Doto (dot matrix) for display/headline/title text, Inter for body text.
- Flat cards with 1 dp outlines, short corner radii, no elevation.
- Material You / dynamic color removed; the palette does not follow the wallpaper.
- Adaptive launcher icon on a black background.

### On-device AI (experimental)
- Gemma 3 1B (int4) runs locally through MediaPipe LLM Inference.
- **No cloud calls.** The AI package contains no HTTP client, URL, or telemetry.
- Rule-first classification into nine categories:
  Finance, Gaming, Subscription, Shopping, Travel, Verification, Social,
  Newsletter, Other.
- Deterministic rules use sender domain, subject keywords (Turkish + English)
  and the `List-Unsubscribe` header. The model is only consulted for
  ambiguous messages, capped per scan.
- Results are cached locally. No message is moved on the IMAP server.

### OTP convenience
- Verification-code emails get a **Copy code** action directly on the
  notification. It extracts 4–8 digit codes locally and copies them to the
  clipboard without opening the app.
- Codes expire from the action after 10 minutes. Codes are redacted before
  any text is passed to the local model and are never logged.

### Battery optimization shortcut
- Settings → General → Network → Battery optimization opens the system
  exemption dialog for Thunderbird only, so background sync is not delayed.

## Model setup

The model is **not bundled** in the APK (it is ~554 MB).

1. Download `gemma3-1b-it-int4.task` (Gemma 3 1B IT, int4) from a trusted
   source and accept the Gemma Terms of Use.
2. Open Thunderbird → Settings → On-device AI → **Import model** and pick the
   `.task` file.
3. Enable **On-device AI**, then open **Categories** and tap **Scan mail**.

Alternatively push it with adb (debug build):

```bash
adb push gemma3-1b-it-int4.task /data/local/tmp/
adb shell run-as net.thunderbird.android.debug mkdir -p files/models
adb shell "run-as net.thunderbird.android.debug cp /data/local/tmp/gemma3-1b-it-int4.task files/models/"
```

Rules-only classification works without the model.

## Build

Requirements: JDK 21, Android SDK 37, `ANDROID_HOME` set.

```bash
./gradlew :app-thunderbird:assembleFossDebug
```

APK: `app-thunderbird/build/outputs/apk/foss/debug/app-thunderbird-foss-debug.apk`

The `foss` flavor avoids Google services. The debug package id is
`net.thunderbird.android.debug`, so it can coexist with the official app.

## Privacy

- The AI engine runs entirely on the device.
- Email bodies are never sent to the model; classification uses sender and
  subject only, with verification codes redacted.
- No analytics, crash reporting, or network access was added by these changes.
- The model file and category cache live in the app's private files directory.
- See `LICENSES/` for the SIL Open Font License texts of Doto and Inter.

## Known limitations

- Classification is manual (Scan mail); there is no background classification
  when a message arrives yet.
- At most 400 cached messages per account are scanned, including sent/drafts.
- No category chips in the message list yet.
- The MediaPipe Android LLM API is marked deprecated upstream.
- Model loading/inference is experimental and may be slow on mid-range devices.
- Only the `fossDebug` variant has been exercised; release/R8 is untested.

## Upstream and license

Based on Thunderbird for Android (Apache License 2.0). Upstream copyright and
license notices are retained. Theme and AI modifications are released under the
same license. Fonts are under the SIL Open Font License 1.1 (see `LICENSES/`).
The Gemma model is governed by the Gemma Terms of Use and is not distributed
with this repository.
