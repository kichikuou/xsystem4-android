# xsystem4-android
This is a work-in-progress Android port of
[xsystem4](https://github.com/nunuhara/xsystem4). It is in the very early
stages of development and has many limitations and bugs.

## Downloading
APKs are available
[here](https://github.com/kichikuou/xsystem4-android/releases).

## Building

### Prerequisites
- Linux or Mac
- Android SDK (Android Studio or Command Line Tools)
  https://developer.android.com/studio
- Android NDK, version r23 or higher
  https://developer.android.com/ndk
- CMake, version 3.21 or higher
- flex
- bison

### Build Steps
1. Set the `ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME` environment variables. For
   example, if you have installed Android Studio on a Mac:
   ```sh
   export ANDROID_SDK_ROOT=~/Library/Android/sdk
   export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/25.1.8937393
   ```
2. Clone this repository:
   ```sh
   git clone https://github.com/kichikuou/xsystem4-android.git
   cd xsystem4-android
   git submodule update --init --recursive
   ```
3. Run `./build-shared-libs.sh` in the repository's root directory. This will
   build the native libraries and place them in
   `project/app/src/main/jniLibs/`.
4. Run `./gradlew build` in the `project` directory. This will generate APK
   files under `project/app/build/outputs/apk/`.

## Running
Connect your device via USB and run `./gradlew installDebug` in the `project`
directory.

Follow the on-screen instructions to install a System4 game. You will need to
transfer the game files to your device using MTP. For example, the directory
structure for Rance VI should look like this:

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

To simulate a right-click during the game, tap the black bars on the left/right
or top/bottom of the screen. You can also simulate pressing the Ctrl key (to
skip messages) by touching and holding.
