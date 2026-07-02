/*
 * VoidTerm - NativeTerminal (JNI wrapper)
 * Safe wrapper around the native PTY engine.
 * Never crashes — all native calls are guarded with Throwable catches.
 * Falls back to Java-only mode when the .so is not yet compiled.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.engine;

import android.util.Log;

public class NativeTerminal {

    private static final String TAG = "VoidTerm-Native";

    // Set to true only if the .so loaded AND linked without error
    private static volatile boolean LIB_OK = false;

    static {
        try {
            System.loadLibrary("voidterm_native");
            LIB_OK = true;
            Log.i(TAG, "Native library loaded");
        } catch (Throwable t) {
            LIB_OK = false;
            Log.w(TAG, "Native library unavailable — Java shell mode active. " + t.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Native declarations — names MUST match jni_bridge.cpp exactly
    // -------------------------------------------------------------------------
    private native int     nativeInit();
    private native int     nativeOpenSession(String shellPath, String[] argv,
                                              String[] envp, String cwd, int cols, int rows);
    private native int     nativeWrite(int session, byte[] data);
    private native byte[]  nativeRead(int session, int maxLen);
    private native int     nativeResize(int session, int cols, int rows);
    private native int     nativeClose(int session);
    private native boolean nativeIsAlive(int session);
    private native int     nativeSendSignal(int session, int sig);
    private native int     nativeGetMasterFd(int session);
    private native void    nativeDestroy();
    private native String  nativeGetVersion();

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private int     sessionId   = -1;
    private boolean initialized = false;

    public NativeTerminal() {
        if (!LIB_OK) return;          // skip — no library
        try {
            initialized = (nativeInit() == 0);
            if (initialized) Log.i(TAG, nativeGetVersion());
        } catch (Throwable t) {
            initialized = false;
            Log.w(TAG, "nativeInit failed: " + t.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Public API — every call wrapped in try/catch(Throwable)
    // -------------------------------------------------------------------------

    /** True only when the .so is loaded and nativeInit() returned 0. */
    public boolean isNativeAvailable() { return LIB_OK && initialized; }
    public static boolean isLibraryLoaded() { return LIB_OK; }

    public int openSession(String shellPath, String[] argv, String[] envp,
                           String cwd, int cols, int rows) {
        if (!isNativeAvailable()) return -1;
        try {
            sessionId = nativeOpenSession(shellPath, argv, envp, cwd, cols, rows);
            return sessionId;
        } catch (Throwable t) { Log.e(TAG, "openSession: " + t); return -1; }
    }

    public int write(byte[] data) {
        if (!isNativeAvailable() || sessionId < 0 || data == null) return -1;
        try { return nativeWrite(sessionId, data); }
        catch (Throwable t) { return -1; }
    }

    public int writeString(String s) {
        if (s == null) return -1;
        return write(s.getBytes());
    }

    public byte[] read(int maxLen) {
        if (!isNativeAvailable() || sessionId < 0) return null;
        try { return nativeRead(sessionId, maxLen); }
        catch (Throwable t) { return null; }
    }

    public int resize(int cols, int rows) {
        if (!isNativeAvailable() || sessionId < 0) return -1;
        try { return nativeResize(sessionId, cols, rows); }
        catch (Throwable t) { return -1; }
    }

    public int sendSignal(int sig) {
        if (!isNativeAvailable() || sessionId < 0) return -1;
        try { return nativeSendSignal(sessionId, sig); }
        catch (Throwable t) { return -1; }
    }

    public void sendCtrlC() { sendSignal(2);  }
    public void sendCtrlD() { writeString("\u0004"); }
    public void sendCtrlZ() { sendSignal(20); }

    public boolean isAlive() {
        if (!isNativeAvailable() || sessionId < 0) return false;
        try { return nativeIsAlive(sessionId); }
        catch (Throwable t) { return false; }
    }

    public int getMasterFd() {
        if (!isNativeAvailable() || sessionId < 0) return -1;
        try { return nativeGetMasterFd(sessionId); }
        catch (Throwable t) { return -1; }
    }

    public void close() {
        if (!isNativeAvailable() || sessionId < 0) return;
        try { nativeClose(sessionId); } catch (Throwable ignored) {}
        sessionId = -1;
    }

    public void destroy() {
        close();
        if (!isNativeAvailable()) return;
        try { nativeDestroy(); } catch (Throwable ignored) {}
        initialized = false;
    }

    public int getSessionId() { return sessionId; }
    public boolean isInitialized() { return initialized; }
}
