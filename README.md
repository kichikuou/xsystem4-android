# xsystem4-android
This is a work in progress Android port of [xsystem4](https://github.com/nunuhara/xsystem4).
This is in very early stage of development and has many limitations and bugs.

## Downloading
APKs can be found [here](https://github.com/kichikuou/xsystem4-android/releases).

## Building

### Prerequisites
- Linux or Mac
- Android SDK (Android Studio or Command line tools)
  https://developer.android.com/studio
- Android NDK >=r23 https://developer.android.com/ndk
- CMake >=3.21
- flex
- bison

### Build Steps
1. Set `ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME` environment variables. For
   example, if you have installed Android Studio on Mac:
   ```sh
   export ANDROID_SDK_ROOT=~/Library/Android/sdk
   export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/25.1.8937393
   ```
2. Clone this repository.
   ```sh
   git clone https://github.com/kichikuou/xsystem4-android.git
   cd xsystem4-android
   git submodule update --init --recursive
   ```
3. Run `./build-shared-libs.sh` in the repository root directory. This will build
   native libraries and install them under `project/app/src/main/jniLibs/`.
4. Run `./gradlew build` in the `project` directory. This will generate APK files
   under `project/app/build/outputs/apk/`.

## Running
Connect your device with a USB cable and run `./gradlew installDebug` in the
`project` directory.

Follow the on-screen instructions to install a System4 game. You will need to
transfer game files to the device over MTP. E.g. the directory structure for
Rance VI will look like this:

```
Android
  data
    io.github.kichikuou.xsystem4
      files
        dummy.txt
        Rance6       <- Create this folder!
          Rance6.ain
          Rance6BA.ald
          ...
          Data
            DungeonData.dlf
            ...
```

To simulate right-click during a game, tap the black bars on the left/light or
top/bottom of the screen.
