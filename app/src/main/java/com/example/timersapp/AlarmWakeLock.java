package com.example.timersapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

final class AlarmWakeLock {
    private static final String TAG = "AlarmWakeLock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 30_000L;

    private static PowerManager.WakeLock wakeLock;

    private AlarmWakeLock() {}

    @SuppressLint("WakelockTimeout")
    static synchronized void acquire(Context context) {
        if (wakeLock == null) {
            PowerManager powerManager =
                    (PowerManager) context.getApplicationContext()
                            .getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) return;

            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    context.getPackageName() + ":timer_alarm"
            );
            wakeLock.setReferenceCounted(false);
        }

        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to acquire timer alarm wake lock", e);
        }
    }

    static synchronized void release() {
        if (wakeLock == null || !wakeLock.isHeld()) return;

        try {
            wakeLock.release();
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to release timer alarm wake lock", e);
        }
    }
}
