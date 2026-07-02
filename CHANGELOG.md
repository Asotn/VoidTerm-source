# VoidTerm Changelog

All notable changes to VoidTerm are documented here.

Developer: Asotn | https://github.com/Asotn/VoidTerm-source | s.pi@outlook.sa

VoidTerm uses year-based versioning: `YY.N`, where `YY` is the last two
digits of the release year and `N` is the release number within that year
(e.g. `26.2` = 2nd release of 2026).

---

## [26.2] - 2026

### Rebrand
- Project renamed from KaliDroid to VoidTerm; all remaining old-name
  references, package IDs (`com.asotn.voidterm`), and JNI symbols updated
  to match
- Developer credited as Asotn; official source repository moved to
  https://github.com/Asotn/VoidTerm-source
- Switched to year-based versioning (`26.2`) starting with this release

### Security
- Fixed shell command-injection vulnerabilities where mirror URLs, package
  names, search queries, and file paths were interpolated unescaped into
  `system()`/`popen()` calls (added a shared `shell_quote`/
  `shell_is_safe_token` helper and applied it across the APT wrapper,
  dpkg helper, repo manager, HTTP client, and PGP verifier)
- Fixed a path-traversal issue in the guest→host path resolver that could
  let a crafted guest path (`../../..`) escape the rootfs sandbox
- Enforced HTTPS-only package mirrors end-to-end (native default mirror,
  APT wrapper, repo manager connectivity check, and the Settings mirror
  field all reject plaintext `http://` now)
- Added a network security config that disables cleartext traffic app-wide
- Hardened `sources.list` writing against newline/config injection that
  could otherwise smuggle in an unsigned, untrusted repository entry
- Tightened default file permissions granted by the executable-bit helper
  (owner/group only, no longer world-executable)
- Hardened the `curl`-based downloads with `--proto '=https'` and
  `--tlsv1.2` pinning
- Corrected a misleading comment implying InRelease fetches were
  cryptographically verified in native code; verification is actually
  performed by `apt-get`'s own GPG trust against the Kali keyring

## [1.0.0] - 2024

### Added
- Real PTY-backed terminal using native C/C++ engine
- Full Kali Linux proot environment (no root required)
- apt-get / apt / sudo support via proot
- Background download progress notifications
- `./files -0 & permission` command for instant storage permission grant
- Command history (up/down arrow) backed by native C history manager
- Shell aliases (ll, la, apt, install, search, update, etc.)
- VT100/VT220/xterm-256color escape code parser
- SHA-256 and MD5 package verification
- Settings: font size, Kali mirror URL, keep screen on
- About screen with GitHub and email links
- F-Droid metadata and build reproducibility
- GitHub Actions CI for automated APK builds
- GPL-3.0 license
- Support for arm64-v8a, armeabi-v7a, x86_64, x86 ABIs
- Notification channels: download progress, bootstrap, session
- Pure black terminal UI — no extra colors, no ads, no trackers
- Static terminal prompt icon (no emoji)

### Architecture
- Java layer: Activities, Services, PackageEngine, TerminalSession
- Native layer: PTY manager, VT100 emulator, escape parser, I/O ring buffer
- Shell layer: history manager, alias engine, env manager, command tokenizer
- Package layer: apt wrapper, dpkg helper, repo manager
- Crypto layer: SHA-256, MD5, PGP verification
- Net layer: HTTP client with progress, progress tracker
- FS layer: fs utils, path resolver, permission helper
- JNI bridges: jni_bridge.cpp, terminal_jni.cpp, package_jni.cpp, fs_jni.cpp

