# Micropolis for Wear OS

A native Wear OS (Android) port of the Micropolis (open-source SimCity) engine.

The portable C++ engine in [`packages/micropolis-engine/`](../../packages/micropolis-engine/)
is compiled for Android ARM via the NDK and driven from a Kotlin / Jetpack Compose
UI through a small JNI bridge. Rendering, input, and audio are written fresh for
the watch; the simulation logic is shared, in-tree, with the web app.

## Status

Built incrementally, with an installable/testable build at each phase:

- [x] **Phase 0** — Toolchain + "Hello Wear OS" Compose app
- [x] **Phase 1** — C++ engine compiled via NDK + JNI smoke test (generate city, read a tile)
- [x] **Phase 2** — Render the map (bulk tile copy + classic tileset atlas, fit-to-screen)
- [x] **Phase 3** — Live simulation loop + load real .cty city (haight) + HUD (date/funds/pop)
- [x] **Phase 4** — Camera + input: crown cycles tool, tap builds, drag pans, double-tap zooms
- [x] **Phase 5** — Watch-native polish
  - [x] 5a Navigation + menu (long-press opens menu; back returns; crown scrolls screens)
  - [x] 5b Budget screen (tax + road/fire/police funding, net)
  - [x] 5c Evaluation/scorecard (approval, score, class, top problems)
  - [x] 5d Save/load + city picker (5 bundled scenarios, quicksave)
  - [x] 5e Audio (engine sounds via SoundPool, JNI up-call)
  - [x] 5f Ambient/battery (sim ticks only while screen is active)
  - [x] 5g On-screen messages (notice banner)

## Controls

- **Crown** — cycle the active tool (bulldoze, road, rail, zones, …)
- **Tap** — apply the active tool at that tile
- **Drag** — pan the map
- **Double-tap** — cycle zoom (medium → close → overview)
- **Long-press** — open the menu (Budget, Evaluation, Cities)
- **Swipe right / back** — leave a screen

## Prerequisites (macOS, installed via Homebrew)

- OpenJDK 17 — `brew install openjdk@17`
- Android command-line tools — `brew install --cask android-commandlinetools`
- SDK packages — `sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.0.12077973" "cmake;3.22.1" "emulator" "system-images;android-34;android-wear;arm64-v8a"`

This project does **not** require Android Studio; the command-line toolchain is enough.
The Gradle wrapper is pinned (8.11.1), so no system Gradle is needed.

### Environment

These are not persisted globally — export them in your shell (or add to your profile):

```sh
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_SDK_ROOT="/opt/homebrew/share/android-commandlinetools"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
```

`local.properties` points Gradle at the SDK (`sdk.dir=...`); it is git-ignored.

## Build

```sh
cd apps/wearos-android
./gradlew :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.
The native engine (`libmicropolis.so`, arm64-v8a) is built by CMake from
[`app/src/main/cpp/CMakeLists.txt`](app/src/main/cpp/CMakeLists.txt), which globs the
engine sources and excludes the Emscripten-only `emscripten.cpp` and `callback.cpp`.

## Run on the emulator

```sh
# One-time: create a round Wear OS AVD
avdmanager create avd -n micropolis_wear \
  -k "system-images;android-34;android-wear;arm64-v8a" -d wearos_small_round

# Launch, install, start
emulator -avd micropolis_wear -no-snapshot -no-boot-anim &
adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.micropolis.wear/.MainActivity

# Screenshot for verification
adb exec-out screencap -p > /tmp/shot.png
```

## Run on a physical watch

Enable Developer options → Wireless debugging on the watch, then:

```sh
adb pair <watch-ip>:<pair-port>      # enter the pairing code shown on the watch
adb connect <watch-ip>:<port>
adb -s <watch-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Layout

```
apps/wearos-android/
  app/src/main/
    cpp/                 JNI bridge + CMake (compiles the shared C++ engine)
      CMakeLists.txt
      micropolis_jni.cpp
    java/com/micropolis/wear/
      MainActivity.kt
      engine/MicropolisEngine.kt   Kotlin wrapper over the JNI handle
    res/                 icons, strings
    AndroidManifest.xml  standalone watch app
```
