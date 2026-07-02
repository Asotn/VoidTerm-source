/*
 * VoidTerm - NativeFs
 * Java wrapper for native fs/crypto/path JNI layer.
 * Safe — never crashes if the native library is absent.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.utils;

import android.util.Log;

public class NativeFs {

    private static final String TAG = "VoidTerm-NativeFs";
    private static final boolean LIB_OK;

    static {
        boolean ok = false;
        try {
            System.loadLibrary("voidterm_native");
            ok = true;
        } catch (Throwable t) {
            Log.w(TAG, "Native library not available: " + t.getMessage());
        }
        LIB_OK = ok;
    }

    // Path resolver
    public static native void    nativePathResolverInit(String rootfs, String home, String sdcard);
    public static native String  nativeGuestToHost(String guestPath);
    public static native String  nativePathJoin(String base, String rel);

    // FS utils
    public static native boolean nativeExists(String path);
    public static native long    nativeFileSize(String path);
    public static native long    nativeFreeSpace(String path);
    public static native boolean nativeMkdirs(String path);
    public static native boolean nativeDeleteFile(String path);
    public static native boolean nativeDeleteDirRecursive(String path);

    // Permissions
    public static native boolean nativeCanRead(String path);
    public static native boolean nativeCanWrite(String path);
    public static native boolean nativeSdcardReadable();
    public static native boolean nativeSdcardWritable();
    public static native String  nativeGetModeString(String path);

    // SHA-256
    public static native String  nativeSha256File(String path);
    public static native boolean nativeSha256Verify(String path, String expectedHex);

    // -------------------------------------------------------------------------
    // Safe wrappers — fall back to Java implementations when native is absent
    // -------------------------------------------------------------------------

    public static boolean exists(String path) {
        if (LIB_OK) { try { return nativeExists(path); } catch (Throwable ignored) {} }
        return new java.io.File(path).exists();
    }

    public static long fileSize(String path) {
        if (LIB_OK) { try { return nativeFileSize(path); } catch (Throwable ignored) {} }
        return new java.io.File(path).length();
    }

    public static boolean mkdirs(String path) {
        if (LIB_OK) { try { return nativeMkdirs(path); } catch (Throwable ignored) {} }
        return new java.io.File(path).mkdirs();
    }

    public static boolean isLibraryAvailable() { return LIB_OK; }

    public static String formatBytes(long bytes) {
        if (bytes < 0)              return "unknown";
        if (bytes < 1024L)          return bytes + " B";
        if (bytes < 1024L * 1024)   return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }
}
