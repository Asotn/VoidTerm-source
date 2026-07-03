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

        // Step 2: Download the chosen distro's rootfs tarball
        String archKey = kaliArchToKey(EnvironmentManager.getKaliArch());
        String rootfsUrl = distro.urlFor(archKey);
        String ext = rootfsUrl.endsWith(".tar.xz") ? ".tar.xz" : ".tar.gz";
        String tarPath = EnvironmentManager.TMP_DIR + "/rootfs" + ext;

        broadcastStatus("Downloading " + distro.displayName + "...\n" + rootfsUrl);
        boolean ok = downloadFile(rootfsUrl, tarPath);
        if (!ok) {
            broadcastError("Failed to download " + distro.displayName + " rootfs");
            return;
        }

        // Step 3: Extract rootfs with busybox tar
        broadcastStatus("Extracting " + distro.displayName + " (this may take a few minutes)...");
        // wipe any previous rootfs first so distros don't mix
        deleteRecursive(new File(EnvironmentManager.KALI_ROOTFS_DIR));
        new File(EnvironmentManager.KALI_ROOTFS_DIR).mkdirs();
        ok = extractTarball(tarPath, EnvironmentManager.KALI_ROOTFS_DIR, ext);
        if (!ok) {
            broadcastError("Failed to extract " + distro.displayName + " rootfs");
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
    private boolean downloadFile(String urlStr, String destPath) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(60_000);
            conn.connect();

            int total = conn.getContentLength();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            FileOutputStream out = new FileOutputStream(destPath);

            byte[] buf = new byte[8192];
            int downloaded = 0, n;
            int lastPct = -1;

            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;

                if (total > 0) {
                    int pct = (downloaded * 100) / total;
                    if (pct != lastPct) {
                        lastPct = pct;
                        updateNotification("Downloading Kali rootfs: " + pct + "%", pct);
                        broadcastProgress(pct, "Downloading: " + pct + "%");
                    }
                }
            }

            out.close();
            in.close();
            conn.disconnect();
            Log.i(TAG, "Download complete: " + destPath);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Download failed: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // extractTarball
    // -------------------------------------------------------------------------
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
            int exit = p.waitFor();
            if (exit != 0) {
                Log.e(TAG, "tar extraction failed with code " + exit);
                return false;
            }
            // Delete tarball to save space
            new File(tarPath).delete();
            Log.i(TAG, "Rootfs extracted to " + destDir);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Extraction exception: " + e.getMessage());
            return false;
        }
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
