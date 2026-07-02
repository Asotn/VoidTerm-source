/*
 * VoidTerm - PackageInstallActivity
 * Transparent shim that triggers APK installs via FileProvider.
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

package com.asotn.voidterm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class PackageInstallActivity extends AppCompatActivity {

    public static final String EXTRA_APK_PATH = "apk_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apkPath = getIntent().getStringExtra(EXTRA_APK_PATH);
        if (apkPath != null) {
            installApk(apkPath);
        }
        finish();
    }

    private void installApk(String path) {
        File apkFile = new File(path);
        if (!apkFile.exists()) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apkFile);
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
