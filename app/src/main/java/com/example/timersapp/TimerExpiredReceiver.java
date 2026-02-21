package com.example.timersapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.media.RingtoneManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class TimerExpiredReceiver extends BroadcastReceiver {
    public static final String ACTION_TIMER_EXPIRED = "com.example.timersapp.TIMER_EXPIRED";
    public static final String EXTRA_TIMER_ID = "timer_id";
    private static final String CHANNEL_ID = "timers_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TIMER_EXPIRED.equals(intent.getAction())) {
            String timerId = intent.getStringExtra(EXTRA_TIMER_ID);
            
            // 1. Prepare intent to launch Activity
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setAction(ACTION_TIMER_EXPIRED);
            activityIntent.putExtra(EXTRA_TIMER_ID, timerId);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                    activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // 2. Build High Priority Notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Timer Finished!")
                    .setContentText("Tap to stop the alarm.")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    .setFullScreenIntent(fullScreenPendingIntent, true) // Key for lockscreen
                    .setAutoCancel(true)
                    .setOngoing(true);

            // 3. Notify
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(timerId != null ? timerId.hashCode() : 999, builder.build());
            }
            
            // Also try to start activity directly as backup for older versions
            context.startActivity(activityIntent);
        }
    }
}