/*
 * VoidTerm - Application Class
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.asotn.voidterm.utils.EnvironmentManager;

public class VoidTermApp extends Application {

    private static final String TAG = "VoidTerm-App";

    public static final String CHANNEL_ID_DOWNLOAD  = "voidterm_download";
    public static final String CHANNEL_ID_BOOTSTRAP = "voidterm_bootstrap";
    public static final String CHANNEL_ID_SESSION   = "voidterm_session";

    private static VoidTermApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Wrap everything — the Application must never crash
        try {
            createNotificationChannels();
        } catch (Throwable t) {
            Log.e(TAG, "Notification channel setup failed: " + t.getMessage());
        }

        try {
            EnvironmentManager.init(this);
        } catch (Throwable t) {
            Log.e(TAG, "EnvironmentManager init failed: " + t.getMessage());
        }
    }

    public static VoidTermApp getInstance() { return instance; }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel dl = new NotificationChannel(
            CHANNEL_ID_DOWNLOAD, "Package Downloads", NotificationManager.IMPORTANCE_LOW);
        dl.setDescription("Download progress for Kali packages");
        dl.setShowBadge(false);
        nm.createNotificationChannel(dl);

        NotificationChannel bs = new NotificationChannel(
            CHANNEL_ID_BOOTSTRAP, "Bootstrap Setup", NotificationManager.IMPORTANCE_DEFAULT);
        bs.setDescription("First-time Kali environment setup");
        nm.createNotificationChannel(bs);

        NotificationChannel sess = new NotificationChannel(
            CHANNEL_ID_SESSION, "Terminal Session", NotificationManager.IMPORTANCE_MIN);
        sess.setDescription("Active terminal session");
        sess.setShowBadge(false);
        nm.createNotificationChannel(sess);
    }
}
