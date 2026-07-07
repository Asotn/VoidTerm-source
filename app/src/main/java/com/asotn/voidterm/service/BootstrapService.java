/*
 * VoidTerm - BootstrapService
 * Downloads and installs the Kali Linux rootfs on first run.
 * Runs as a foreground service so it survives app backgrounding.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.asotn.voidterm.VoidTermApp;
import com.asotn.voidterm.R;
import com.asotn.voidterm.ui.TerminalActivity;
import com.asotn.voidterm.utils.DistroCatalog;
import com.asotn.voidterm.utils.EnvironmentManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BootstrapService extends Service {

    private static final String TAG     = "VoidTerm-Bootstrap";
    private static final int    NOTIF_ID = 3001;

    public static final String ACTION_START  = "com.asotn.voidterm.BOOTSTRAP_START";
    public static final String EXTRA_DISTRO_ID = "distro_id"; // e.g. "kali", "ubuntu"
    public static final String BROADCAST_DONE    = "com.asotn.voidterm.BOOTSTRAP_DONE";
    public static final String BROADCAST_ERROR   = "com.asotn.voidterm.BOOTSTRAP_ERROR";
    public static final String BROADCAST_PROGRESS = "com.asotn.voidterm.BOOTSTRAP_PROGRESS";
    public static final String EXTRA_PROGRESS    = "progress";
    public static final String EXTRA_STATUS      = "status";

    private Thread bootstrapThread;
    private DistroCatalog.Distro distro;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String distroId = intent != null ? intent.getStringExtra(EXTRA_DISTRO_ID) : null;
        distro = findDistro(distroId);
        if (distro == null) distro = DistroCatalog.all().get(0); // default: Kali

        startForeground(NOTIF_ID, buildNotification("Setting up " + distro.displayName + "...", -1));
        bootstrapThread = new Thread(this::runBootstrap, "bootstrap-thread");
        bootstrapThread.start();
        return START_NOT_STICKY;
    }

    private DistroCatalog.Distro findDistro(String id) {
        if (id == null) return null;
        for (DistroCatalog.Distro d : DistroCatalog.all()) {
            if (d.id.equals(id)) return d;
        }
        return null;
    }

    private void runBootstrap() {
        Log.i(TAG, "Bootstrap started for " + distro.displayName);

        // Step 1: Verify proot/busybox are present. These now ship as real
        // native libraries (jniLibs) that Android extracts + chmod +x at
        // install time, so no runtime extraction is needed at all.
        broadcastStatus("Checking runtime binaries...");
        File prootFile = new File(EnvironmentManager.PROOT_BIN);
        File busyboxFile = new File(EnvironmentManager.BUSYBOX_BIN);
        if (!prootFile.exists() || !busyboxFile.exists()) {
            broadcastError("Missing proot/busybox in app package. Reinstall the APK.");
            return;
        }
        if (!prootFile.canExecute()) prootFile.setExecutable(true, false);
        if (!busyboxFile.canExecute()) busyboxFile.setExecutable(true, false);

        // Pre-flight diagnostic: actually run busybox on THIS device and
        // report what really happens, instead of assuming. This is the
        // single most useful line of output when something goes wrong.
        broadcastStatus(runDiagnostic(busyboxFile));

        // Step 2: Download the chosen distro's rootfs tarball
        String archKey = kaliArchToKey(EnvironmentManager.getKaliArch());
        String rootfsUrl = distro.urlFor(archKey);
        String ext = rootfsUrl.endsWith(".tar.xz") ? ".tar.xz" : ".tar.gz";
        String tarPath = EnvironmentManager.TMP_DIR + "/rootfs" + ext;

        broadcastStatus("Downloading " + distro.displayName + "...\n" + rootfsUrl);
        boolean ok = downloadFile(rootfsUrl, tarPath);
        if (!ok) {
            broadcastError("Failed to download " + distro.displayName + " rootfs\n\n" + lastDownloadError);
            return;
        }

        // Step 3: Extract rootfs with busybox tar
        broadcastStatus("Extracting " + distro.displayName + " (this may take a few minutes)...");
        // wipe any previous rootfs first so distros don't mix
        deleteRecursive(new File(EnvironmentManager.KALI_ROOTFS_DIR));
        new File(EnvironmentManager.KALI_ROOTFS_DIR).mkdirs();
        ok = extractTarball(tarPath, EnvironmentManager.KALI_ROOTFS_DIR, ext);
        if (!ok) {
            broadcastError("Failed to extract " + distro.displayName + " rootfs\n\n" + lastExtractError);
            return;
        }

        // Step 4: Write initial resolv.conf
        writeResolvConf();

        // Step 5: Mark as bootstrapped
        EnvironmentManager.markBootstrapped(distro.id, distro.displayName);

        broadcastStatus(distro.displayName + " is ready.");
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(new Intent(BROADCAST_DONE));

        stopForeground(true);
        stopSelf();
        Log.i(TAG, "Bootstrap complete: " + distro.displayName);
    }

    private static String kaliArchToKey(String kaliArch) {
        // EnvironmentManager.getKaliArch() already returns arm64/armhf/amd64/i386
        return kaliArch;
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    // -------------------------------------------------------------------------
    // downloadFile with progress
    // -------------------------------------------------------------------------
    /** Set by downloadFile() so the caller can report the real failure reason. */
    private String lastDownloadError = "";

    private boolean downloadFile(String urlStr, String destPath) {
        return downloadFile(urlStr, destPath, 0);
    }

    private boolean downloadFile(String urlStr, String destPath, int redirectDepth) {
        if (redirectDepth > 5) {
            lastDownloadError = "Too many redirects";
            return false;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(120_000);
            conn.setInstanceFollowRedirects(false); // handle manually so we can log the target
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android) VoidTerm/26.2");
            conn.connect();

            int code = conn.getResponseCode();

            // Manual redirect handling (some mirrors 301/302 to a CDN host)
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) {
                    lastDownloadError = "HTTP " + code + " redirect with no Location header";
                    return false;
                }
                Log.i(TAG, "Redirect " + code + " -> " + location);
                return downloadFile(location, destPath, redirectDepth + 1);
            }

            if (code != HttpURLConnection.HTTP_OK) {
                lastDownloadError = "Server returned HTTP " + code + " for:\n" + urlStr;
                conn.disconnect();
                return false;
            }

            int total = conn.getContentLength();
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(destPath)) {

                byte[] buf = new byte[8192];
                int downloaded = 0, n;
                int lastPct = -1;

                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    downloaded += n;

                    if (total > 0) {
                        int pct = (int) ((downloaded * 100L) / total);
                        if (pct < 0) pct = 0;
                        if (pct > 100) pct = 100;
                        if (pct != lastPct) {
                            lastPct = pct;
                            updateNotification("Downloading: " + pct + "%", pct);
                            broadcastProgress(pct, "Downloading: " + pct + "%");
                        }
                    }
                }
            }

            Log.i(TAG, "Download complete: " + destPath);
            return true;
        } catch (Exception e) {
            lastDownloadError = e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "\nURL: " + urlStr;
            Log.e(TAG, "Download failed: " + lastDownloadError);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // -------------------------------------------------------------------------
    // extractTarball
    // -------------------------------------------------------------------------
    private String lastExtractError = "";

    private boolean extractTarball(String tarPath, String destDir, String ext) {
        try {
            String flag = ext.equals(".tar.xz") ? "-xJf" : "-xzf";
            String[] cmd = new String[]{
                EnvironmentManager.BUSYBOX_BIN,
                "tar", flag, tarPath,
                "-C", destDir,
                "--strip-components=1"
            };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(destDir));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // IMPORTANT: the output stream MUST be drained while the process
            // runs, or a large amount of tar output can fill the OS pipe
            // buffer and deadlock both the child (blocked writing) and this
            // thread (blocked in waitFor()).
            StringBuilder outputLog = new StringBuilder();
            Thread drain = new Thread(() -> {
                try (java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        outputLog.append(line).append('\n');
                        if (outputLog.length() > 4000) {
                            outputLog.delete(0, outputLog.length() - 4000); // keep tail only
                        }
                    }
                } catch (IOException ignored) { }
            }, "tar-output-drain");
            drain.start();

            int exit = p.waitFor();
            drain.join(2000);

            if (exit != 0) {
                lastExtractError = "tar exited with code " + exit + "\n" +
                        tail(outputLog.toString(), 600);
                Log.e(TAG, "tar extraction failed: " + lastExtractError);
                return false;
            }
            // Delete tarball to save space
            new File(tarPath).delete();
            Log.i(TAG, "Rootfs extracted to " + destDir);
            return true;
        } catch (Exception e) {
            lastExtractError = e.getClass().getSimpleName() + ": " + e.getMessage();
            Log.e(TAG, "Extraction exception: " + lastExtractError);
            return false;
        }
    }

    /** Actually runs busybox on THIS device and reports the real outcome —
     *  file size, whether it executes at all, and whether the tar applet
     *  responds — instead of assuming the binary works. */
    private String runDiagnostic(File busyboxFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Busybox binary: ").append(busyboxFile.length()).append(" bytes, ")
          .append("executable=").append(busyboxFile.canExecute()).append('\n');
        try {
            Process p = new ProcessBuilder(busyboxFile.getAbsolutePath())
                    .redirectErrorStream(true).start();
            String banner = readAllQuick(p, 2000);
            p.waitFor();
            sb.append("Runs OK: ").append(firstLine(banner)).append('\n');
        } catch (Exception e) {
            sb.append("FAILED TO EXECUTE: ")
              .append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');
            return sb.toString();
        }
        try {
            Process p = new ProcessBuilder(busyboxFile.getAbsolutePath(), "tar", "--help")
                    .redirectErrorStream(true).start();
            String out = readAllQuick(p, 2000);
            p.waitFor();
            boolean hasTar = out.toLowerCase().contains("usage: tar");
            sb.append("tar applet: ").append(hasTar ? "OK" : "MISSING (" + firstLine(out) + ")");
        } catch (Exception e) {
            sb.append("tar applet check FAILED: ").append(e.getMessage());
        }
        return sb.toString();
    }

    private static String readAllQuick(Process p, int timeoutMs) throws Exception {
        StringBuilder out = new StringBuilder();
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            String line;
            while (System.currentTimeMillis() < deadline && (line = r.readLine()) != null) {
                out.append(line).append('\n');
                if (out.length() > 500) break;
            }
        }
        return out.toString();
    }

    private static String firstLine(String s) {
        if (s == null || s.isEmpty()) return "(no output)";
        int i = s.indexOf('\n');
        return i == -1 ? s : s.substring(0, i);
    }

    private static String tail(String s, int maxChars) {
        if (s == null || s.length() <= maxChars) return s;
        return "...\n" + s.substring(s.length() - maxChars);
    }

    // -------------------------------------------------------------------------
    // writeResolvConf
    // -------------------------------------------------------------------------
    private void writeResolvConf() {
        String resolvPath = EnvironmentManager.KALI_ROOTFS_DIR + "/etc/resolv.conf";
        try {
            FileOutputStream fos = new FileOutputStream(resolvPath);
            fos.write("nameserver 8.8.8.8\nnameserver 8.8.4.4\n".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write resolv.conf: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Broadcasts
    // -------------------------------------------------------------------------
    private void broadcastStatus(String status) {
        Log.i(TAG, status);
        updateNotification(status, -1);
        Intent i = new Intent(BROADCAST_PROGRESS);
        i.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void broadcastProgress(int pct, String status) {
        Intent i = new Intent(BROADCAST_PROGRESS);
        i.putExtra(EXTRA_PROGRESS, pct);
        i.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void broadcastError(String error) {
        Log.e(TAG, error);
        Intent i = new Intent(BROADCAST_ERROR);
        i.putExtra(EXTRA_STATUS, error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        stopForeground(true);
        stopSelf();
    }

    // -------------------------------------------------------------------------
    // Notification
    // -------------------------------------------------------------------------
    private Notification buildNotification(String text, int progress) {
        Intent intent = new Intent(this, TerminalActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
            new NotificationCompat.Builder(this, VoidTermApp.CHANNEL_ID_BOOTSTRAP)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VoidTerm Setup")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi);

        if (progress >= 0) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    private void updateNotification(String text, int progress) {
        NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(text, progress));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bootstrapThread != null) bootstrapThread.interrupt();
    }
}
