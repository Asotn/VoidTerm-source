# Building VoidTerm from Source

Complete instructions for building the VoidTerm APK from source.

**Developer:** Asotn | https://github.com/Asotn | s.pi@outlook.sa

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Android Studio | Hedgehog 2023.1.1+ | or any IDE with Android Gradle plugin 8.3+ |
| JDK | 17 | Must be Java 17; newer may cause issues |
| Android SDK | API 34 | Install via SDK Manager |
| Android NDK | 26.3.11579264 | **Exact version required for reproducibility** |
| CMake | 3.22.1 | Install via SDK Manager |
| Git | Any | For cloning |

---

## Step 1 — Clone the repository

```bash
git clone https://github.com/Asotn/VoidTerm-source.git
cd VoidTerm
```

---

## Step 2 — Install SDK components

Open Android Studio → SDK Manager → SDK Tools tab, install:

- Android NDK (Side by side) → version **26.3.11579264**
- CMake → version **3.22.1**
- Android SDK Build-Tools 34

Or via command line:

```bash
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
  "ndk;26.3.11579264" \
  "cmake;3.22.1" \
  "build-tools;34.0.0" \
  "platforms;android-34"
```

---

## Step 3 — Add required binary assets

VoidTerm requires statically compiled **proot** and **busybox** binaries
for each target ABI. These are not included in the repository.

Place them at:
```
app/src/main/assets/bin/<ABI>/proot
app/src/main/assets/bin/<ABI>/busybox
```

Where `<ABI>` is one of: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`

**Getting proot:**
```bash
# Download pre-built from proot releases:
# https://github.com/proot-me/proot/releases

# Or from Termux bootstrap (arm64):
wget https://packages.termux.dev/apt/termux-main/pool/main/p/proot/proot_5.4.0_aarch64.deb
dpkg-deb -x proot_5.4.0_aarch64.deb /tmp/proot-extracted
cp /tmp/proot-extracted/data/data/com.termux/files/usr/bin/proot \
   app/src/main/assets/bin/arm64-v8a/proot
```

**Getting busybox:**
```bash
# Download from busybox.net or GitHub:
wget https://busybox.net/downloads/binaries/1.35.0-aarch64-linux-musl/busybox \
  -O app/src/main/assets/bin/arm64-v8a/busybox
chmod +x app/src/main/assets/bin/arm64-v8a/busybox
```

---

## Step 4 — Build the APK

### Debug build (easiest, no signing needed)

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install directly:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release build (unsigned)

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Step 5 — Sign the release APK

```bash
# 1. Create a keystore (one time only)
keytool -genkey -v \
  -keystore voidterm-release.jks \
  -alias voidterm \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass yourpassword \
  -keypass yourpassword \
  -dname "CN=Asotn, OU=VoidTerm, O=Asotn, L=Unknown, S=Unknown, C=US"

# 2. Sign the APK
apksigner sign \
  --ks voidterm-release.jks \
  --ks-key-alias voidterm \
  --ks-pass pass:yourpassword \
  --key-pass pass:yourpassword \
  --out voidterm-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# 3. Verify
apksigner verify --verbose voidterm-release-signed.apk
```

---

## Step 6 — Install on device

```bash
adb install -r voidterm-release-signed.apk
```

Or sideload: copy the APK to the device and open it in Files.

---

## Building with Android Studio (GUI)

1. Open Android Studio
2. File → Open → select the `VoidTerm` directory
3. Wait for Gradle sync to complete
4. Place the binary assets as described in Step 3
5. Build → Make Project (`Ctrl+F9`)
6. Build → Build Bundle(s) / APK(s) → Build APK(s)
7. Click "locate" in the notification to find the APK

---

## F-Droid build

VoidTerm is ready for F-Droid. The build recipe is in `metadata/com.asotn.voidterm/en-US.yml`.

F-Droid builds with:
```yaml
gradle:
  - release
ndk: 26.3.11579264
```

**Note:** F-Droid will not include the proot/busybox binaries automatically.
The binary assets must be built from source as part of the F-Droid build process.
A future update will include NDK-based build scripts for proot and busybox.

---

## Troubleshooting

### "NDK not configured"
Set `local.properties`:
```
sdk.dir=/path/to/Android/sdk
ndk.dir=/path/to/Android/sdk/ndk/26.3.11579264
```

### CMake errors
Ensure CMake 3.22.1 is installed via SDK Manager, not the system CMake.

### "Cannot find proot" at runtime
The app looks for `proot` in `<filesDir>/bin/proot`. Ensure the binary was
extracted from assets correctly. Check logcat for `VoidTerm-Env` tag.

### Build fails with "Unsupported class file major version"
You are using a JDK newer than 17. Switch to JDK 17:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew assembleDebug
```

---

## Project structure overview

```
VoidTerm/
├── app/src/main/
│   ├── cpp/              Native C/C++ engine (PTY, APT, crypto, net, fs)
│   ├── java/             Java layer (Activities, Services, Engine wrappers)
│   ├── assets/scripts/   Shell scripts (bootstrap, setup, tool installer)
│   ├── assets/bin/       proot + busybox binaries (add before build)
│   └── res/              Layouts, themes, strings, icons
├── metadata/             F-Droid metadata
├── .github/workflows/    GitHub Actions CI (builds APK on every push)
└── README.md             Full documentation
```

---

*Questions or issues? Open an issue at https://github.com/Asotn/VoidTerm-source/issues*
*Email: s.pi@outlook.sa*
