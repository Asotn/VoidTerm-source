⚠️ This version is archived.
The new version is here: [link]
# KaliDroid 

**Kali Linux Terminal for Android**

A real, open-source Kali Linux terminal emulator for Android. Runs the full Kali Linux environment via proot — no root required. Supports `apt`, `sudo`, and every terminal command in the world.

| | |
|---|---|
| **Developer** | Rotlqe |
| **GitHub** | https://github.com/Rotlqe |
| **Email** | s.pi@outlook.sa |
| **License** | GPL-3.0 |
| **F-Droid** | Ready |
| **Min SDK** | Android 8.0 (API 26) |
| **Architectures** | arm64-v8a, armeabi-v7a, x86_64, x86 |

---

## What is KaliDroid?

KaliDroid is not a fake terminal. It is a real application with:

- A **C/C++ PTY engine** that creates real pseudo-terminal sessions
- A **real bash shell** running inside a proot Kali Linux rootfs
- **Full apt-get support** — install any tool from the Kali repositories
- **sudo** — all commands run as root inside the proot environment
- **Background notifications** — download progress shown even when the app is minimized
- **Storage permission** — type `./files -0 & permission` and it immediately opens the storage permission dialog
- **Pure black UI** — no extra colors, no emoji, no clutter

---

## Screenshots

```
  _  __     _ _ ____            _     _
 | |/ /__ _| (_)  _ \ _ __ ___ (_) __| |
 | ' // _` | | | | | | '__/ _ \| |/ _` |
 | . \ (_| | | | |_| | | | (_) | | (_| |
 |_|\_\__,_|_|_|____/|_|  \___/|_|\__,_|

  Kali Linux Terminal for Android
  Developer : github.com/Rotlqe  |  s.pi@outlook.sa
  Version   : 1.0.0  |  License: GPL-3.0
  Type kalidroid-help for usage, or start with apt install
```

---

## Building the APK

### Requirements

- Android Studio Hedgehog or newer
- NDK version 26.3.11579264
- CMake 3.22.1
- JDK 17
- Android SDK with API 34

### Steps

```bash
# Clone the repository
git clone https://github.com/Rotlqe/KaliDroid
cd KaliDroid

# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned)
./gradlew assembleRelease

# The APK is at:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release-unsigned.apk
```

### Signing the release APK

```bash
# Generate keystore (one time)
keytool -genkey -v -keystore kalidroid.jks \
  -alias kalidroid -keyalg RSA -keysize 2048 -validity 10000

# Sign the APK
apksigner sign \
  --ks kalidroid.jks \
  --ks-key-alias kalidroid \
  --out kalidroid-release.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Project Structure

```
KaliDroid/
├── app/
│   ├── build.gradle                      # App build config (NDK, dependencies)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml           # Permissions, activities, services
│       ├── assets/
│       │   ├── bin/                      # proot + busybox binaries (add before build)
│       │   └── scripts/
│       │       └── bootstrap.sh          # First-run Kali environment setup
│       ├── cpp/                          # C/C++ native engine
│       │   ├── CMakeLists.txt
│       │   ├── terminal/
│       │   │   ├── pty_manager.c/h       # PTY creation, fork/exec, I/O
│       │   │   ├── escape_parser.c/h     # ANSI/VT100 escape code parser
│       │   │   ├── vt100.c/h             # VT100 screen buffer emulator
│       │   │   ├── io_buffer.c/h         # Lock-free ring buffer
│       │   │   └── process_runner.c/h    # Shell process execution
│       │   ├── shell/
│       │   │   ├── command_tokenizer.c/h # Shell command tokenizer
│       │   │   ├── history_manager.c/h   # Command history (persistent)
│       │   │   ├── alias_engine.c/h      # Alias expansion
│       │   │   └── env_manager.c/h       # Environment variable store
│       │   ├── package/
│       │   │   ├── apt_wrapper.c/h       # apt-get wrapper with progress parsing
│       │   │   ├── dpkg_helper.c/h       # dpkg database queries
│       │   │   └── repo_manager.c/h      # APT repository management
│       │   ├── crypto/
│       │   │   ├── sha256.c/h            # SHA-256 (package verification)
│       │   │   ├── md5.c/h               # MD5 (legacy apt checksums)
│       │   │   └── pgp_verify.c/h        # PGP signature verification
│       │   ├── net/
│       │   │   ├── http_client.c/h       # HTTP download with progress
│       │   │   └── progress_tracker.c/h  # Multi-file download tracking
│       │   ├── fs/
│       │   │   ├── fs_utils.c/h          # File/directory operations
│       │   │   ├── path_resolver.c/h     # Guest/host path translation
│       │   │   └── permission_helper.c/h # Filesystem permission checks
│       │   └── jni/
│       │       ├── jni_bridge.cpp        # Main JNI bridge (PTY)
│       │       ├── terminal_jni.cpp      # VT100, history, alias JNI
│       │       ├── package_jni.cpp       # APT, dpkg, repo JNI
│       │       └── fs_jni.cpp            # FS, path, crypto JNI
│       ├── java/com/rotlqe/kalidroid/
│       │   ├── KaliDroidApp.java         # Application class, notification channels
│       │   ├── engine/
│       │   │   ├── NativeTerminal.java   # JNI wrapper for C terminal engine
│       │   │   └── PackageEngine.java    # APT/dpkg Java interface
│       │   ├── terminal/
│       │   │   ├── TerminalSession.java  # Shell session lifecycle + I/O threads
│       │   │   └── CommandProcessor.java # Built-in command interception
│       │   ├── service/
│       │   │   ├── PackageService.java   # Foreground service for downloads
│       │   │   ├── BootstrapService.java # First-run rootfs download + setup
│       │   │   └── BootReceiver.java     # Boot broadcast receiver
│       │   ├── ui/
│       │   │   ├── TerminalActivity.java # Main terminal screen
│       │   │   ├── SettingsActivity.java # Settings screen
│       │   │   ├── AboutActivity.java    # About screen
│       │   │   └── PackageInstallActivity.java
│       │   └── utils/
│       │       ├── AppPreferences.java   # SharedPreferences wrapper
│       │       ├── EnvironmentManager.java # Path constants, env builder
│       │       └── NativeFs.java         # JNI wrapper for fs/crypto
│       └── res/
│           ├── layout/                   # Pure black XML layouts
│           ├── values/                   # Strings, colors, themes, arrays
│           ├── drawable/                 # Vector icons (terminal prompt style)
│           ├── menu/                     # Options menu (Ctrl-C, clear, etc.)
│           └── xml/                      # Preferences, file_paths, backup rules
├── metadata/                             # F-Droid metadata
├── fastlane/                             # Store metadata
├── .github/workflows/build.yml           # GitHub Actions CI
├── build.gradle                          # Root build config
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── LICENSE                               # GPL-3.0
├── CHANGELOG.md
└── README.md
```

---

## Adding proot and busybox Binaries

Before building, place pre-compiled static binaries for the target architecture in:

```
app/src/main/assets/bin/proot
app/src/main/assets/bin/busybox
```

You can obtain them from:
- proot: https://github.com/proot-me/proot/releases
- busybox: https://busybox.net/downloads/binaries/

These are not included in the repository due to binary file policies. The bootstrap process extracts them on first run.

---

## Terminal Commands

| Command | Description |
|---|---|
| `apt update` | Update package lists from Kali repos |
| `apt install <pkg>` | Install a Kali Linux package |
| `apt remove <pkg>` | Remove a package |
| `apt search <term>` | Search for packages |
| `apt upgrade` | Upgrade installed packages |
| `sudo <cmd>` | Run as root (already root in proot) |
| `./files -0 & permission` | Grant file access permission |
| `clear` | Clear the terminal screen |
| `kalidroid-help` | Show built-in help |
| `kalidroid-about` | Show developer info |
| `exit` | Close the terminal |

All standard Unix/Linux commands are supported. This is a real bash shell.

---

## Notification System

KaliDroid shows download progress in the status bar even when the app is minimized:

- **Package Downloads** — shows percentage as apt downloads packages
- **Bootstrap Setup** — shows progress during first-run Kali rootfs setup
- **Terminal Session** — persistent indicator while a session is active

Notification permission is requested on first launch.

---

## F-Droid

KaliDroid is built to be fully F-Droid compatible:

- No proprietary SDKs
- No tracking libraries
- No ads
- GPL-3.0 licensed
- Reproducible builds via GitHub Actions
- F-Droid metadata in `metadata/` directory

---

## Contributing

Pull requests are welcome. Please follow the existing code style and include tests where applicable.

**Issues:** https://github.com/Rotlqe/KaliDroid/issues

---

## License

```
KaliDroid - Kali Linux Terminal for Android
Copyright (C) 2024 Rotlqe <s.pi@outlook.sa>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

See [LICENSE](LICENSE) for the full text.
