# RealChat – Voice-First AI Chat for Android

RealChat is a modern Android application that lets you hold **hands-free, back-and-forth conversations** with OpenAI's GPT models.  Speak naturally, see real-time audio visualisations, and hear the assistant reply with realistic speech.

---

## ✨ Features

• **Voice interaction loop** – automatic recording, silence detection, transcription (Whisper), chat completion (GPT-3.5 Turbo), and text-to-speech (TTS-1) in a continuous cycle.

• **Real-time visualisers** – animated *Yarn-Ball* while the user speaks and a *Waveform* while the AI speaks, both written with Jetpack Compose.

• **Offline-safe recording** – records to app-private storage and cleans up unused files.

• **Permission handling & rationale UI** – graceful UX for microphone permission, including permanent-denial flow.

• **Modern Android stack**:
  - Jetpack Compose 1.7+ UI
  - Kotlin 2.0 with Compose Multiplatform plugin
  - Hilt for dependency injection
  - MVVM with `ViewModel` + Kotlin Coroutines + `StateFlow`
  - Retrofit 2 & OkHttp 4 clients
  - Room 2.7 (placeholder – ready for future chat history persistence)

---

## 🗂️ Project Structure

```
RealChat/
  ├── app/                     # Android application module
  │   ├── src/main/java/…
  │   │   ├── data/            # Network + repository layer
  │   │   ├── ui/              # Compose screens, visualisers, view-models
  │   │   └── MainActivity.kt  # Single-activity host
  │   └── build.gradle.kts
  ├── gradle/libs.versions.toml # Centralised dependency versions
  ├── apikeys.properties        # (Locally created) OpenAI API key
  ├── build.gradle.kts          # Top-level Gradle config
  └── settings.gradle.kts
```

---

## 🚀 Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/<you>/RealChat.git
   cd RealChat
   ```

2. **Obtain an OpenAI API key**

   Create one at <https://platform.openai.com/account/api-keys>.  Charges apply according to OpenAI pricing.

3. **Add the key to the project** (choose one):

   • *Recommended* – create a file named `apikeys.properties` in the project root:

     ```properties
     OPENAI_API_KEY=sk-...
     ```

   • **OR** export an environment variable before building:

     ```bash
     setx OPENAI_API_KEY "sk-..."          # Windows PowerShell
     # OR
     export OPENAI_API_KEY="sk-..."        # macOS/Linux
     ```

4. **Open in Android Studio** (Hedgehog / Iguana or newer) ‑ the IDE will sync Gradle automatically.

5. **Run the app** on a device/emulator running **Android 7.0 (API 24)** or higher.

> 📢 The first launch will request microphone permission. Grant it so the app can record your voice.

---

## ⚙️ Build From Command Line

```
./gradlew :app:installDebug   # Linux/macOS
gradlew.bat :app:installDebug # Windows
```

---

## 📖 How It Works

1. **Recording** – `MediaRecorder` captures the user's speech and writes it to an `.m4a` file.
2. **Voice Activity Detection** – simple amplitude threshold & silence window decide when the user finished.
3. **Transcription** – the recorded file is sent to OpenAI Whisper (`audio/transcriptions`).
4. **Chat Completion** – user text forms the prompt for GPT-3.5 Turbo (`chat/completions`).
5. **Text-to-Speech** – the assistant's reply is converted to MP3 via `audio/speech` (TTS-1).
6. **Playback** – `MediaPlayer` streams the generated MP3 while displaying the waveform visualiser.

All networking happens inside `VoiceChatRepository`, which injects the `Authorization: Bearer <key>` header into each request.

---

## 🛡️ Permissions

| Permission | Why it's needed | When it's requested |
|------------|-----------------|---------------------|
| `RECORD_AUDIO` | Capture microphone input to send to OpenAI Whisper. | On first launch of voice chat. |

No other dangerous permissions are used.  All recordings are stored in the app's private directory and removed when no longer needed.

---

## 🧩 Roadmap / Ideas

- Use **Speech Recognition** APIs for on-device fallback.
- Persist chat history with Room and Paging.
- Settings screen to pick voices / models.
- Adaptive UI for tablets / Wear OS.

Contributions & feature requests welcome!

---

## 📝 License

```
MIT License

Copyright (c) 2025 <Your Name>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

Made with ❤️ + 🪄 by *RealChat*. 