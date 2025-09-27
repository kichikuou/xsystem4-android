# xsystem4-android
This is a work-in-progress Android port of
[xsystem4](https://github.com/nunuhara/xsystem4).

Android 5.0 or higher is required.

## Downloading
You can get the app in one of two ways:

### Direct Download
APKs are available
[here](https://github.com/kichikuou/xsystem4-android/releases).

### Obtainium
You can use [Obtainium](https://github.com/ImranR98/Obtainium) to get notified
of new releases and install them easily.

Click the badge below to add the app to Obtainium:

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="60">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.kichikuou.xsystem4%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fkichikuou%2Fxsystem4-android%22%2C%22author%22%3A%22kichikuou%22%2C%22name%22%3A%22xsystem4%22%2C%22categories%22%3A%5B%22games%22%5D%7D)

## Building
You can skip this section if you just want to run the app.

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
   export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/26.1.10909125
   ```
2. Clone this repository recursively:
   ```sh
   git clone --recursive https://github.com/kichikuou/xsystem4-android.git
   ```
3. Run `./build-shared-libs.sh` in the repository's root directory. This will
   build the native libraries and place them in
   `project/app/src/main/jniLibs/`.
4. Run `./gradlew build` in the `project` directory. This will generate APK
   files under `project/app/build/outputs/apk/`.
5. To install the APK on your device, run `./gradlew installDebug` in the
   `project` directory.

## Installing Games
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

If your device supports SD cards, you can place the game files under the
`Android/data/io.github.kichikuou.xsystem4/files/game_name` directory on the SD
card.

Alternatively, you can install a game from a ZIP archive on your device. Select
"Install from ZIP" from the menu in the upper right corner.

Note: On some devices or Android versions, you may not be able to access the
`/Android/data` directory via MTP. In such cases, you have to either use the
"Install from ZIP" method or write directly to the SD card using a card reader
on your PC.

## Touch Gestures
The following operations can be simulated using touch gestures:

| Operation | Touch Gesture |
| --------- | ------------- |
| Left-click      | Tap |
| Right-click     | Tap the black bars outside the game screen |
| Ctrl key        | Touch and hold |
| Scroll up/down  | Two-finger swipe down/up |
