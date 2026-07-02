/*
 * VoidTerm - NotificationHelper
 * Centralized utility for building and posting notifications.
 * Handles download progress, bootstrap status, session indicator, and errors.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.asotn.voidterm.VoidTermApp;
import com.asotn.voidterm.R;
import com.asotn.voidterm.ui.TerminalActivity;

public class NotificationHelper {

    // Notification IDs
    public static final int ID_DOWNLOAD  = 1001;
    public static final int ID_SESSION   = 1002;
    public static final int ID_BOOTSTRAP = 1003;
    public static final int ID_ERROR     = 1004;
    public static final int ID_INSTALL   = 1005;

    private static NotificationManager getManager(Context ctx) {
        return (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static PendingIntent getMainIntent(Context ctx) {
        Intent i = new Intent(ctx, TerminalActivity.class);
        return PendingIntent.getActivity(ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // -------------------------------------------------------------------------
    // showDownloadProgress
    // -------------------------------------------------------------------------
    public static void showDownloadProgress(Context ctx, String pkgName, int progress) {
        String title = (pkgName != null && !pkgName.isEmpty())
            ? "Downloading: " + pkgName
            : "Package download in progress";

        NotificationCompat.Builder b =
            new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_DOWNLOAD)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VoidTerm")
                .setContentText(title)
                .setOnlyAlertOnce(true)
                .setOngoing(progress < 100)
                .setContentIntent(getMainIntent(ctx));

        if (progress > 0) {
            b.setProgress(100, progress, false);
            b.setSubText(progress + "%");
        } else {
            b.setProgress(0, 0, true);
        }

        getManager(ctx).notify(ID_DOWNLOAD, b.build());
    }

    // -------------------------------------------------------------------------
    // dismissDownload
    // -------------------------------------------------------------------------
    public static void dismissDownload(Context ctx) {
        getManager(ctx).cancel(ID_DOWNLOAD);
    }

    // -------------------------------------------------------------------------
    // showInstallComplete
    // -------------------------------------------------------------------------
    public static void showInstallComplete(Context ctx, String pkgName) {
        Notification n = new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Install complete")
            .setContentText(pkgName + " installed successfully")
            .setAutoCancel(true)
            .setContentIntent(getMainIntent(ctx))
            .build();
        getManager(ctx).notify(ID_INSTALL, n);
    }

    // -------------------------------------------------------------------------
    // showBootstrapProgress
    // -------------------------------------------------------------------------
    public static void showBootstrapProgress(Context ctx, String status, int progress) {
        NotificationCompat.Builder b =
            new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_BOOTSTRAP)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VoidTerm Setup")
                .setContentText(status)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(getMainIntent(ctx));

        if (progress >= 0) b.setProgress(100, progress, false);
        else               b.setProgress(0, 0, true);

        getManager(ctx).notify(ID_BOOTSTRAP, b.build());
    }

    // -------------------------------------------------------------------------
    // dismissBootstrap
    // -------------------------------------------------------------------------
    public static void dismissBootstrap(Context ctx) {
        getManager(ctx).cancel(ID_BOOTSTRAP);
    }

    // -------------------------------------------------------------------------
    // showSessionActive
    // -------------------------------------------------------------------------
    public static void showSessionActive(Context ctx, int sessionCount) {
        String text = sessionCount == 1
            ? "1 terminal session active"
            : sessionCount + " terminal sessions active";

        Notification n = new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("VoidTerm")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(getMainIntent(ctx))
            .build();

        getManager(ctx).notify(ID_SESSION, n);
    }

    // -------------------------------------------------------------------------
    // dismissSession
    // -------------------------------------------------------------------------
    public static void dismissSession(Context ctx) {
        getManager(ctx).cancel(ID_SESSION);
    }

    // -------------------------------------------------------------------------
    // showError
    // -------------------------------------------------------------------------
    public static void showError(Context ctx, String title, String message) {
        Notification n = new NotificationCompat.Builder(ctx, VoidTermApp.CHANNEL_ID_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getMainIntent(ctx))
            .build();
        getManager(ctx).notify(ID_ERROR, n);
    }

    // -------------------------------------------------------------------------
    // dismissAll
    // -------------------------------------------------------------------------
    public static void dismissAll(Context ctx) {
        getManager(ctx).cancelAll();
    }

    // -------------------------------------------------------------------------
    // areNotificationsEnabled
    // -------------------------------------------------------------------------
    public static boolean areNotificationsEnabled(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getManager(ctx).areNotificationsEnabled();
        }
        return true;
    }
}
