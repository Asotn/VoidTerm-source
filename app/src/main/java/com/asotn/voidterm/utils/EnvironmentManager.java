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

        // proot / busybox are shipped as jniLibs (lib<name>.so under the app's
        // nativeLibraryDir). Android guarantees these are extracted to disk
        // with the executable bit set at install time, on every Android
        // version — unlike files copied from assets/ at runtime, which can
        // be blocked by W^X / noexec restrictions on newer Android versions.
        String nativeDir = appContext.getApplicationInfo().nativeLibraryDir;
        PROOT_BIN        = nativeDir + "/libproot.so";
        BUSYBOX_BIN      = nativeDir + "/libbusybox.so";
        PROOT_LOADER_BIN = nativeDir + "/libprootloader.so";

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
}
