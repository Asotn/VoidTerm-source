/*
 * VoidTerm - EnvironmentManager
 * Central place for filesystem paths, architecture detection, and the
 * proot/Kali environment. Every other class (VoidTermApp, TerminalSession,
 * CommandProcessor, BootstrapService, PackageEngine, TabCompletionEngine)
 * calls into this class, so it must be initialized first in
 * VoidTermApp#onCreate().
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class EnvironmentManager {

    private static final String TAG = "VoidTerm-Env";
    private static final String PREFS_NAME = "voidterm_env";
    private static final String KEY_BOOTSTRAPPED = "bootstrapped";
    private static final String KEY_DISTRO_ID = "distro_id";
    private static final String KEY_DISTRO_NAME = "distro_name";

    // Populated by init()
    private static Context appContext;
    private static SharedPreferences prefs;

    // -------------------------------------------------------------------
    // Public path constants (filled in by init())
    // -------------------------------------------------------------------
    public static String FILES_DIR;
    public static String BIN_DIR;
    public static String TMP_DIR;
    public static String HOME_DIR;
    public static String KALI_ROOTFS_DIR;
    public static String PROOT_BIN;
    public static String PROOT_LOADER_BIN;
    public static String BUSYBOX_BIN;

    private EnvironmentManager() { }

    // -------------------------------------------------------------------
    // init — MUST be called once from VoidTermApp#onCreate()
    // -------------------------------------------------------------------
    public static synchronized void init(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        FILES_DIR       = appContext.getFilesDir().getAbsolutePath();
        BIN_DIR          = FILES_DIR + "/bin";
        TMP_DIR          = FILES_DIR + "/tmp";
        KALI_ROOTFS_DIR = FILES_DIR + "/kali-rootfs";
        HOME_DIR         = KALI_ROOTFS_DIR + "/root";

        // proot / busybox ship as plain assets (never touched by AGP's
        // native-library packaging/strip pipeline, which was silently
        // corrupting them when they lived under jniLibs). They are
        // extracted once to BIN_DIR and chmod +x'd — see
        // ensureRuntimeBinaries() below, called from BootstrapService
        // before first use.
        PROOT_BIN        = BIN_DIR + "/proot";
        BUSYBOX_BIN      = BIN_DIR + "/busybox";
        PROOT_LOADER_BIN = BIN_DIR + "/prootloader";

        for (String dir : new String[]{BIN_DIR, TMP_DIR, KALI_ROOTFS_DIR}) {
            File f = new File(dir);
            if (!f.exists()) {
                //noinspection ResultOfMethodCallIgnored
                f.mkdirs();
            }
        }

        Log.i(TAG, "EnvironmentManager initialized. filesDir=" + FILES_DIR);
    }

    // -------------------------------------------------------------------
    // Bootstrap state
    // -------------------------------------------------------------------
    public static boolean isBootstrapped() {
        if (prefs == null) return false;
        return prefs.getBoolean(KEY_BOOTSTRAPPED, false) && new File(HOME_DIR).exists();
    }

    public static void markBootstrapped(String distroId, String distroDisplayName) {
        if (prefs == null) return;
        prefs.edit()
            .putBoolean(KEY_BOOTSTRAPPED, true)
            .putString(KEY_DISTRO_ID, distroId)
            .putString(KEY_DISTRO_NAME, distroDisplayName)
            .apply();
    }

    public static String getInstalledDistroName() {
        if (prefs == null) return null;
        return prefs.getString(KEY_DISTRO_NAME, null);
    }

    public static String getInstalledDistroId() {
        if (prefs == null) return null;
        return prefs.getString(KEY_DISTRO_ID, null);
    }

    // -------------------------------------------------------------------
    // Architecture helpers
    // -------------------------------------------------------------------
    public static String getPrimaryAbi() {
        String[] abis = Build.SUPPORTED_ABIS;
        return (abis != null && abis.length > 0) ? abis[0] : "unknown";
    }

    /** Maps the device ABI to the Kali/Debian architecture name used by apt repos. */
    public static String getKaliArch() {
        String abi = getPrimaryAbi();
        switch (abi) {
            case "arm64-v8a":
                return "arm64";
            case "armeabi-v7a":
            case "armeabi":
                return "armhf";
            case "x86_64":
                return "amd64";
            case "x86":
                return "i386";
            default:
                return "arm64";
        }
    }

    // -------------------------------------------------------------------
    // Shell launch helpers
    // -------------------------------------------------------------------

    /** Builds argv for launching bash inside the proot Kali rootfs. */
    public static String[] buildShellArgs(boolean loginShell) {
        List<String> args = new ArrayList<>();
        args.add(PROOT_BIN);
        args.add("-0"); // fake root inside proot
        args.add("-r");
        args.add(KALI_ROOTFS_DIR);
        args.add("-b");
        args.add("/dev");
        args.add("-b");
        args.add("/proc");
        args.add("-b");
        args.add("/sys");
        args.add("-w");
        args.add("/root");
        args.add("/usr/bin/env");
        args.add("-i");
        args.add("HOME=/root");
        args.add("TERM=xterm-256color");
        args.add("/bin/sh");
        return args.toArray(new String[0]);
    }

    public static String[] buildEnvironment() {
        return new String[]{
            "HOME=" + HOME_DIR,
            "TERM=xterm-256color",
            "USER=root",
            "LOGNAME=root",
            "LANG=en_US.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:" + BIN_DIR,
            "DEBIAN_FRONTEND=noninteractive",
            "PROOT_LOADER=" + PROOT_LOADER_BIN,
            "PROOT_TMP_DIR=" + TMP_DIR
        };
    }

    // -------------------------------------------------------------------
    // Distro rootfs downloads are resolved per-distro via DistroCatalog,
    // matched to this device's architecture through getKaliArch().
    // See BootstrapService.runBootstrap().
    // -------------------------------------------------------------------

    /**
     * proot/busybox no longer ship inside the APK at all. Every packaging
     * strategy we tried (assets extraction, jniLibs "rename as .so") was
     * either blocked or silently corrupted these binaries somewhere in
     * Android's build pipeline. Instead, BootstrapService downloads them
     * directly from their upstream source over HTTPS on first run — the
     * exact same bytes verified working, with zero Gradle/AGP involvement.
     *
     * Returns {"proot": url, "busybox": url, "prootloader": url} for this
     * device's CPU architecture, or null if unsupported (x86/x86_64).
     */
    public static java.util.Map<String, String> runtimeBinaryUrls() {
        boolean isArm64 = false, isArm32 = false;
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.equals("arm64-v8a")) isArm64 = true;
            if (abi.equals("armeabi-v7a") || abi.equals("armeabi")) isArm32 = true;
        }
        if (!isArm64 && !isArm32) return null;

        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        // proot/loader are 32-bit ARM but run fine on both arm and arm64
        // Android devices (kernel-level 32-bit compat) — one build covers both.
        m.put("proot", "https://raw.githubusercontent.com/ZhymabekRoman/proot-static/main/bin/proot");
        m.put("prootloader", "https://raw.githubusercontent.com/ZhymabekRoman/proot-static/main/bin/loader");
        m.put("busybox", isArm64
                ? "https://raw.githubusercontent.com/ARM-software/devlib/master/devlib/bin/arm64/busybox"
                : "https://raw.githubusercontent.com/ARM-software/devlib/master/devlib/bin/armeabi/busybox");
        return m;
    }

    /** True once proot/busybox/loader are present on disk and executable. */
    public static boolean runtimeBinariesReady() {
        File proot = new File(PROOT_BIN);
        File busybox = new File(BUSYBOX_BIN);
        File loader = new File(PROOT_LOADER_BIN);
        return proot.exists() && busybox.exists() && loader.exists()
                && proot.canExecute() && busybox.canExecute() && loader.canExecute();
    }
}
