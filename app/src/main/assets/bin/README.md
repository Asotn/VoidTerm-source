# VoidTerm - Required Binary Assets

Before building the APK, you must place the following **static** pre-compiled
binaries for each target ABI in this directory:

## Required files

| File | Description |
|---|---|
| `proot` | proot binary (no-root chroot alternative) |
| `busybox` | BusyBox with tar support (for rootfs extraction) |

## Where to get them

### proot
Download a pre-built static binary from the official proot GitHub releases or
from the Termux bootstrap packages:

```
https://github.com/proot-me/proot/releases
```

Or build from source for each ABI:
```bash
git clone https://github.com/proot-me/proot
cd proot/src
NDK=/path/to/ndk make -f GNUmakefile CROSS_COMPILE=aarch64-linux-android- proot
```

### busybox
Static busybox binaries for Android:
```
https://busybox.net/downloads/binaries/1.35.0-x86_64-linux-musl/busybox
https://github.com/meefik/busybox/releases
```

## ABI structure

Place binaries in ABI-specific subdirectories:
```
assets/bin/
├── arm64-v8a/
│   ├── proot
│   └── busybox
├── armeabi-v7a/
│   ├── proot
│   └── busybox
├── x86_64/
│   ├── proot
│   └── busybox
└── x86/
    ├── proot
    └── busybox
```

Then update `EnvironmentManager.extractAsset()` to select the correct ABI
binary at runtime using `Build.SUPPORTED_ABIS[0]`.

## Notes

- These binaries must be **statically linked** (no external .so dependencies)
- They must be executable on Android (no SELinux restrictions in the app's private directory)
- They are **not committed to git** (see .gitignore)

---

Developer: Asotn | https://github.com/Asotn | s.pi@outlook.sa
