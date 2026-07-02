/*
 * VoidTerm - Environment Manager
 * Handles bootstrap installation of the Kali Linux rootfs (proot/chroot),
 * environment variable setup, and first-run initialization.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentManager {

    private static final String TAG = "VoidTerm-Env";

    // Paths inside the app's private data directory
    public static String HOME_DIR;
    public static String FILES_DIR;
    public static String PROOT_DIR;
    public static String KALI_ROOTFS_DIR;
    public static String BIN_DIR;
    public static String TMP_DIR;
    public static String APT_CACHE_DIR;
    public static String DOWNLOADS_DIR;

    // The proot binary path (extracted from assets)
    public static String PROOT_BIN;
    public static String BUSYBOX_BIN;

    // Kali repos
    public static final String KALI_MIRROR    = "https://http.kali.org/kali";
    public static final String KALI_DIST      = "kali-rolling";
    public static final String KALI_COMPONENTS = "main contrib non-free non-free-firmware";

    private static Context appContext;
    private static boolean initialized = false;

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------
    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();

        File filesDir = appContext.getFilesDir();
        FILES_DIR       = filesDir.getAbsolutePath();
        HOME_DIR        = FILES_DIR + "/home";
        PROOT_DIR       = FILES_DIR + "/proot";
        KALI_ROOTFS_DIR = FILES_DIR + "/kali-rootfs";
        BIN_DIR         = FILES_DIR + "/bin";
        TMP_DIR         = FILES_DIR + "/tmp";
        APT_CACHE_DIR   = FILES_DIR + "/apt/cache";
        DOWNLOADS_DIR   = FILES_DIR + "/downloads";
        PROOT_BIN       = BIN_DIR + "/proot";
        BUSYBOX_BIN     = BIN_DIR + "/busybox";

        // Create directory structure
        ensureDirs(HOME_DIR, PROOT_DIR, KALI_ROOTFS_DIR, BIN_DIR, TMP_DIR,
                   APT_CACHE_DIR, DOWNLOADS_DIR);

        initialized = true;
        Log.i(TAG, "Environment manager initialized. Root: " + FILES_DIR);
    }

    // -------------------------------------------------------------------------
    // isBootstrapped
    // Returns true if the Kali rootfs has been set up.
    // -------------------------------------------------------------------------
    public static boolean isBootstrapped() {
        File marker = new File(KALI_ROOTFS_DIR + "/.bootstrapped");
        return marker.exists();
    }

    // -------------------------------------------------------------------------
    // markBootstrapped
    // -------------------------------------------------------------------------
    public static void markBootstrapped() {
        try {
            File marker = new File(KALI_ROOTFS_DIR + "/.bootstrapped");
            marker.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "Failed to create bootstrap marker: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // buildEnvironment
    // Returns the environment variable map for the shell session.
    // -------------------------------------------------------------------------
    public static String[] buildEnvironment() {
        Map<String, String> env = new HashMap<>();

        String abi = getPrimaryAbi();

        env.put("TERM",       "xterm-256color");
        env.put("COLORTERM",  "truecolor");
        env.put("LANG",       "en_US.UTF-8");
        env.put("LC_ALL",     "en_US.UTF-8");
        env.put("HOME",       "/root");
        env.put("USER",       "root");
        env.put("LOGNAME",    "root");
        env.put("SHELL",      "/bin/bash");
        env.put("PREFIX",     KALI_ROOTFS_DIR);
        env.put("PROOT_DIR",  PROOT_DIR);
        env.put("TMP",        "/tmp");
        env.put("TMPDIR",     "/tmp");
        env.put("ANDROID_DATA",        appContext.getApplicationInfo().dataDir);
        env.put("ANDROID_ROOT",        "/system");
        env.put("EXTERNAL_STORAGE",    "/sdcard");
        env.put("ANDROID_ABI",         abi);

        env.put("PATH",
            "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" +
            ":" + BIN_DIR +
            ":/usr/local/games:/usr/games"
        );

        // Kali-specific
        env.put("KALI_ROLLING", "1");
        env.put("DEBIAN_FRONTEND", "noninteractive");
        env.put("APT_KEY_DONT_WARN_ON_DANGEROUS_USAGE", "1");

        // Build string array
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }
        return result.toArray(new String[0]);
    }

    // -------------------------------------------------------------------------
    // buildShellArgs
    // Returns the proot command line to launch a Kali bash session.
    // -------------------------------------------------------------------------
    public static String[] buildShellArgs(boolean useLogin) {
        List<String> args = new ArrayList<>();

        // proot binary
        args.add(PROOT_BIN);

        // Bind /proc, /sys, /dev
        args.add("--bind=/proc");
        args.add("--bind=/dev");
        args.add("--bind=/sys");

        // Bind /sdcard if accessible
        args.add("--bind=/sdcard");

        // Bind /data/data app directory
        args.add("--bind=" + FILES_DIR + ":/voidterm");

        // proot root = Kali rootfs
        args.add("-r");
        args.add(KALI_ROOTFS_DIR);

        // Work dir
        args.add("-w");
        args.add("/root");

        // Fix proc/uid
        args.add("--link2symlink");
        args.add("--kill-on-exit");

        // Fake root (UID 0)
        args.add("--root-id");

        // Shell
        if (useLogin) {
            args.add("/bin/bash");
            args.add("--login");
        } else {
            args.add("/bin/bash");
        }

        return args.toArray(new String[0]);
    }

    // -------------------------------------------------------------------------
    // getPrimaryAbi
    // -------------------------------------------------------------------------
    public static String getPrimaryAbi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
                return Build.SUPPORTED_ABIS[0];
            }
        }
        return Build.CPU_ABI;
    }

    // -------------------------------------------------------------------------
    // getKaliArch
    // Maps Android ABI to Kali/Debian architecture string.
    // -------------------------------------------------------------------------
    public static String getKaliArch() {
        String abi = getPrimaryAbi();
        switch (abi) {
            case "arm64-v8a":   return "arm64";
            case "armeabi-v7a":
            case "armeabi":     return "armhf";
            case "x86_64":      return "amd64";
            case "x86":         return "i386";
            default:            return "arm64";
        }
    }

    // -------------------------------------------------------------------------
    // extractAsset
    // -------------------------------------------------------------------------
    public static boolean extractAsset(String assetName, String destPath) {
        try {
            InputStream in  = appContext.getAssets().open(assetName);
            OutputStream out = new FileOutputStream(destPath);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            out.close();
            new File(destPath).setExecutable(true, false);
            Log.i(TAG, "Extracted asset: " + assetName + " -> " + destPath);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract asset " + assetName + ": " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // ensureDirs
    // -------------------------------------------------------------------------
    private static void ensureDirs(String... paths) {
        for (String path : paths) {
            File f = new File(path);
            if (!f.exists()) {
                boolean ok = f.mkdirs();
                Log.d(TAG, "mkdir " + path + ": " + ok);
            }
        }
    }

    // -------------------------------------------------------------------------
    // getContext
    // -------------------------------------------------------------------------
    public static Context getContext() {
        return appContext;
    }

    // -------------------------------------------------------------------------
    // getRootfsUrl
    // Returns the correct Kali rootfs tarball URL for the current device ABI.
    // -------------------------------------------------------------------------
    public static String getRootfsUrl() {
        String arch = getKaliArch();
        return "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-" + arch + ".tar.xz";
    }

    // -------------------------------------------------------------------------
    // isStorageAccessGranted
    // -------------------------------------------------------------------------
    public static boolean isStorageAccessGranted() {
        File test = new File("/sdcard/.voidterm_test");
        try {
            if (test.createNewFile()) {
                test.delete();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
