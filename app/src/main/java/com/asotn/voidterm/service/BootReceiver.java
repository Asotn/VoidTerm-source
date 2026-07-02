/*
 * VoidTerm - BootReceiver
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

package com.asotn.voidterm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "VoidTerm-Boot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Boot completed received");
            // Nothing to auto-start; terminal sessions are user-initiated.
        }
    }
}
