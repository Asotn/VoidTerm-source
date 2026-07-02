/*
 * VoidTerm - TerminalSession
 * Manages a real shell session. Uses one of two backends:
 *   1. Native PTY engine (C/JNI) — when the .so is compiled and loaded
 *   2. Java ProcessBuilder — always available, no NDK needed
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.terminal;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.asotn.voidterm.VoidTermApp;
import com.asotn.voidterm.R;
import com.asotn.voidterm.engine.NativeTerminal;
import com.asotn.voidterm.ui.TerminalActivity;
import com.asotn.voidterm.utils.EnvironmentManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalSession {

    private static final String TAG     = "VoidTerm-Session";
    private static final int    READ_BUF = 32 * 1024;
    private static final int    NOTIF_ID_DOWNLOAD = 1001;
    private static final int    NOTIF_ID_SESSION  = 1002;

    private static final Pattern PROGRESS_PAT = Pattern.compile("(\\d{1,3})%\\s+\\[");

    public interface OutputCallback {
        void onOutput(String text);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final Context        ctx;
    private final AtomicBoolean  running    = new AtomicBoolean(false);
    private       OutputCallback outputCallback;
    private       NotificationManager nm;

    // Native backend
    private NativeTerminal  nativeTerm;
    private Thread          nativeReaderThread;

    // Java ProcessBuilder backend (always-available fallback)
    private Process         shellProcess;
    private OutputStream    shellStdin;
    private Thread          javaReaderThread;
    private boolean         usingJavaBackend = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    public TerminalSession(Context context) {
        this.ctx = context.getApplicationContext();
        this.nm  = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        this.nativeTerm = new NativeTerminal();
    }

    public void setOutputCallback(OutputCallback cb) {
        this.outputCallback = cb;
    }

    // -------------------------------------------------------------------------
    // start — picks the best available backend
    // -------------------------------------------------------------------------
    public void start() {
        if (running.get()) return;

        if (nativeTerm.isNativeAvailable() && EnvironmentManager.isBootstrapped()) {
            Log.i(TAG, "Starting native PTY backend");
            startNativeBackend();
        } else {
            Log.i(TAG, "Starting Java ProcessBuilder backend (native not available)");
            startJavaBackend();
        }
    }

    // =========================================================================
    // BACKEND 1 — Native PTY (when .so is compiled)
    // =========================================================================

    private void startNativeBackend() {
        String   shellPath = EnvironmentManager.PROOT_BIN;
        String[] argv      = EnvironmentManager.buildShellArgs(true);
        String[] envp      = EnvironmentManager.buildEnvironment();
        String   cwd       = EnvironmentManager.HOME_DIR;

        int sessionId = nativeTerm.openSession(shellPath, argv, envp, cwd, 80, 24);
        if (sessionId < 0) {
            emit("\r\n[VoidTerm] Native session failed. Falling back to system shell.\r\n");
            startJavaBackend();
            return;
        }

        running.set(true);
        usingJavaBackend = false;
        startNativeReaderThread();
        showSessionNotification();
    }

    private void startNativeReaderThread() {
        nativeReaderThread = new Thread(() -> {
            while (running.get()) {
                byte[] data = nativeTerm.read(READ_BUF);
                if (data == null || data.length == 0) {
                    if (!nativeTerm.isAlive()) {
                        emit("\r\n[Session ended]\r\n");
                        running.set(false);
                        break;
                    }
                    try { Thread.sleep(8); } catch (InterruptedException ignored) {}
                    continue;
                }
                String text = new String(data, StandardCharsets.UTF_8);
                emit(text);
                checkProgress(text);
            }
            nm.cancel(NOTIF_ID_SESSION);
        }, "native-pty-reader");
        nativeReaderThread.setDaemon(true);
        nativeReaderThread.start();
    }

    // =========================================================================
    // BACKEND 2 — Java ProcessBuilder (always works, no NDK needed)
    // =========================================================================

    private void startJavaBackend() {
        usingJavaBackend = true;

        List<String> cmd = new ArrayList<>();

        if (EnvironmentManager.isBootstrapped()) {
            // Use proot if rootfs is ready
            String[] args = EnvironmentManager.buildShellArgs(true);
            for (String a : args) cmd.add(a);
        } else {
            // Use the Android system shell — always present
            cmd.add("/system/bin/sh");
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("TERM",     "xterm-256color");
        pb.environment().put("HOME",     "/data/data/com.asotn.voidterm/files/home");
        pb.environment().put("USER",     "root");
        pb.environment().put("LOGNAME",  "root");
        pb.environment().put("LANG",     "en_US.UTF-8");
        pb.environment().put("DEBIAN_FRONTEND", "noninteractive");
        pb.environment().put("PATH",
            "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:" +
            EnvironmentManager.BIN_DIR);
        pb.redirectErrorStream(true);

        try {
            shellProcess = pb.start();
            shellStdin   = shellProcess.getOutputStream();
            running.set(true);

            startJavaReaderThread(shellProcess.getInputStream());
            showSessionNotification();

            // Send initial welcome + prompt setup
            if (!EnvironmentManager.isBootstrapped()) {
                sendInput("echo 'VoidTerm - System Shell (Kali not yet set up)'\n");
                sendInput("echo 'Run: apt-get update to begin Kali setup'\n");
                sendInput("PS1='root@kali:# '\n");
            }

            Log.i(TAG, "Java shell backend started");

        } catch (IOException e) {
            Log.e(TAG, "Failed to start shell: " + e.getMessage());
            emit("\r\n[VoidTerm] Could not start shell: " + e.getMessage() + "\r\n");
            emit("[VoidTerm] Type commands and press Enter to try manually.\r\n");
        }
    }

    private void startJavaReaderThread(InputStream stream) {
        javaReaderThread = new Thread(() -> {
            byte[] buf = new byte[READ_BUF];
            try {
                int n;
                while (running.get() && (n = stream.read(buf)) != -1) {
                    String text = new String(buf, 0, n, StandardCharsets.UTF_8);
                    emit(text);
                    checkProgress(text);
                }
            } catch (IOException e) {
                if (running.get()) {
                    Log.w(TAG, "Shell reader closed: " + e.getMessage());
                }
            } finally {
                if (running.get()) {
                    emit("\r\n[Session ended]\r\n");
                    running.set(false);
                }
                nm.cancel(NOTIF_ID_SESSION);
            }
        }, "java-shell-reader");
        javaReaderThread.setDaemon(true);
        javaReaderThread.start();
    }

    // =========================================================================
    // Input / signals
    // =========================================================================

    public void sendInput(String text) {
        if (!running.get()) return;
        try {
            if (usingJavaBackend) {
                if (shellStdin != null) {
                    shellStdin.write(text.getBytes(StandardCharsets.UTF_8));
                    shellStdin.flush();
                }
            } else {
                nativeTerm.writeString(text);
            }
        } catch (IOException | Exception e) {
            Log.e(TAG, "sendInput error: " + e.getMessage());
        }
    }

    public void sendInput(byte[] data) {
        if (!running.get()) return;
        try {
            if (usingJavaBackend) {
                if (shellStdin != null) { shellStdin.write(data); shellStdin.flush(); }
            } else {
                nativeTerm.write(data);
            }
        } catch (IOException | Exception e) {
            Log.e(TAG, "sendInput(bytes) error: " + e.getMessage());
        }
    }

    public void sendSignalInterrupt() {
        if (usingJavaBackend) {
            sendInput("\u0003"); // Ctrl-C as text
        } else {
            nativeTerm.sendCtrlC();
        }
    }

    public void resize(int cols, int rows) {
        if (!usingJavaBackend) nativeTerm.resize(cols, rows);
    }

    // =========================================================================
    // Stop / lifecycle
    // =========================================================================

    public void stop() {
        running.set(false);
        if (usingJavaBackend) {
            if (shellProcess != null) {
                shellProcess.destroy();
                shellProcess = null;
            }
            if (javaReaderThread != null) javaReaderThread.interrupt();
        } else {
            nativeTerm.destroy();
            if (nativeReaderThread != null) nativeReaderThread.interrupt();
        }
        nm.cancel(NOTIF_ID_SESSION);
        nm.cancel(NOTIF_ID_DOWNLOAD);
    }

    public boolean isRunning() {
        if (!running.get()) return false;
        if (usingJavaBackend) {
            try { shellProcess.exitValue(); return false; }
            catch (IllegalThreadStateException e) { return true; }
            catch (Exception e) { return false; }
        }
        return nativeTerm.isAlive();
    }

    public boolean isUsingJavaBackend() { return usingJavaBackend; }

    // =========================================================================
    // Notifications
    // =========================================================================

    private void showSessionNotification() {
        try {
            Intent intent = new Intent(ctx, TerminalActivity.class);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_SESSION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VoidTerm")
                .setContentText("Terminal session active")
                .setOngoing(true)
                .setSilent(true)
                .setContentIntent(pi)
                .build();
            nm.notify(NOTIF_ID_SESSION, n);
        } catch (Exception e) {
            Log.w(TAG, "Could not show session notification: " + e.getMessage());
        }
    }

    private void checkProgress(String text) {
        try {
            Matcher m = PROGRESS_PAT.matcher(text);
            if (m.find()) {
                int pct = Integer.parseInt(m.group(1));
                Intent intent = new Intent(ctx, TerminalActivity.class);
                PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Notification n = new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_DOWNLOAD)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("VoidTerm - Downloading")
                    .setContentText("Package download: " + pct + "%")
                    .setProgress(100, pct, false)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pi)
                    .build();
                nm.notify(NOTIF_ID_DOWNLOAD, n);
                if (pct >= 100) {
                    new Handler(Looper.getMainLooper()).postDelayed(
                        () -> nm.cancel(NOTIF_ID_DOWNLOAD), 3000);
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // Emit
    // =========================================================================
    private void emit(String text) {
        if (outputCallback != null) outputCallback.onOutput(text);
    }
}
