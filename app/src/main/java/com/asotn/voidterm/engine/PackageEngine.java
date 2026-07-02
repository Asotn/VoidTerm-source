/*
 * VoidTerm - PackageEngine
 * Java interface to the native APT/dpkg/repo layer.
 * Safe — guards all native calls, works without the .so compiled.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.engine;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.asotn.voidterm.service.PackageService;
import com.asotn.voidterm.utils.AppPreferences;
import com.asotn.voidterm.utils.EnvironmentManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackageEngine {

    private static final String TAG = "VoidTerm-PkgEngine";

    // Safe: the .so is already loaded by NativeTerminal's static block.
    // We do NOT call loadLibrary again here — just guard each native call.
    private native void   nativeAptInit(String prootBin, String rootfs, String mirror);
    private native String nativeAptUpdate();
    private native String nativeAptInstall(String pkgList);
    private native String nativeAptRemove(String pkgList, boolean purge);
    private native String nativeAptSearch(String query);
    private native String nativeAptShow(String pkg);
    private native String nativeAptUpgrade(boolean distUpgrade);
    private native String nativeAptFixBroken();
    private native boolean nativeDpkgIsInstalled(String pkg);
    private native String  nativeDpkgGetVersion(String pkg);
    private native String  nativeDpkgListInstalled();
    private native int     nativeDpkgInstalledCount();
    private native boolean nativeRepoCheckConnectivity(String mirror);
    private native void    nativeRepoWriteSourcesList();

    // Callback interface
    public interface PackageCallback {
        void onOutput(String line);
        void onComplete(boolean success, String summary);
        void onError(String error);
    }

    private final Context         ctx;
    private final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());
    private       boolean         initialized = false;

    private static PackageEngine instance;
    public static PackageEngine getInstance(Context ctx) {
        if (instance == null) instance = new PackageEngine(ctx);
        return instance;
    }
    private PackageEngine(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------
    public void init() {
        if (initialized) return;
        if (!NativeTerminal.isLibraryLoaded()) {
            Log.i(TAG, "Native library not loaded — package engine in Java-only mode");
            initialized = true;
            return;
        }
        try {
            String mirror = AppPreferences.get(ctx).getKaliMirror();
            nativeAptInit(EnvironmentManager.PROOT_BIN,
                          EnvironmentManager.KALI_ROOTFS_DIR, mirror);
            initialized = true;
            Log.i(TAG, "Package engine initialized (native)");
        } catch (Throwable t) {
            Log.w(TAG, "Package engine native init failed: " + t.getMessage());
            initialized = true; // mark done so we don't retry endlessly
        }
    }

    // -------------------------------------------------------------------------
    // Safe native call helper
    // -------------------------------------------------------------------------
    private String safeNativeString(NativeStringCall call, String fallback) {
        if (!NativeTerminal.isLibraryLoaded()) return fallback;
        try { return call.run(); } catch (Throwable t) { return fallback + "\n[Error: " + t.getMessage() + "]"; }
    }

    @FunctionalInterface interface NativeStringCall { String run(); }

    // -------------------------------------------------------------------------
    // APT operations — run on background thread, callback on UI thread
    // -------------------------------------------------------------------------

    public void update(PackageCallback cb) {
        ensureInit(); startService("apt-get update");
        executor.execute(() -> {
            String out = safeNativeString(() -> nativeAptUpdate(),
                "[VoidTerm] Native engine not available. Use terminal: apt-get update");
            boolean ok = !out.contains("Err:") && !out.contains("E: ");
            uiHandler.post(() -> { cb.onOutput(out); cb.onComplete(ok, ok ? "Done." : "Errors occurred."); });
            stopService();
        });
    }

    public void install(String packages, PackageCallback cb) {
        ensureInit(); startService(packages);
        executor.execute(() -> {
            String out = safeNativeString(() -> nativeAptInstall(packages),
                "[VoidTerm] Use terminal: apt-get install -y " + packages);
            boolean ok = out.contains("newly installed") || out.contains("already the newest");
            uiHandler.post(() -> { cb.onOutput(out); cb.onComplete(ok, ok ? "Installed." : "Check output."); });
            stopService();
        });
    }

    public void remove(String packages, boolean purge, PackageCallback cb) {
        ensureInit();
        executor.execute(() -> {
            String out = safeNativeString(() -> nativeAptRemove(packages, purge), "[error]");
            boolean ok = !out.contains("E: ");
            uiHandler.post(() -> { cb.onOutput(out); cb.onComplete(ok, "Done."); });
        });
    }

    public void search(String query, PackageCallback cb) {
        ensureInit();
        executor.execute(() -> {
            String out = safeNativeString(() -> nativeAptSearch(query), "[no results]");
            uiHandler.post(() -> { cb.onOutput(out); cb.onComplete(true, ""); });
        });
    }

    public void upgrade(boolean dist, PackageCallback cb) {
        ensureInit(); startService("upgrade");
        executor.execute(() -> {
            String out = safeNativeString(() -> nativeAptUpgrade(dist), "[error]");
            boolean ok = !out.contains("E: ");
            uiHandler.post(() -> { cb.onOutput(out); cb.onComplete(ok, "Done."); });
            stopService();
        });
    }

    public boolean isInstalled(String pkg) {
        ensureInit();
        if (!NativeTerminal.isLibraryLoaded()) return false;
        try { return nativeDpkgIsInstalled(pkg); } catch (Throwable t) { return false; }
    }

    public int getInstalledCount() {
        ensureInit();
        if (!NativeTerminal.isLibraryLoaded()) return -1;
        try { return nativeDpkgInstalledCount(); } catch (Throwable t) { return -1; }
    }

    private void ensureInit() { if (!initialized) init(); }
    private void startService(String pkg) {
        try {
            Intent i = new Intent(ctx, PackageService.class);
            i.setAction(PackageService.ACTION_START);
            i.putExtra(PackageService.EXTRA_PACKAGE, pkg);
            ctx.startService(i);
        } catch (Throwable ignored) {}
    }
    private void stopService() {
        try {
            Intent i = new Intent(ctx, PackageService.class);
            i.setAction(PackageService.ACTION_STOP);
            ctx.startService(i);
        } catch (Throwable ignored) {}
    }
}
