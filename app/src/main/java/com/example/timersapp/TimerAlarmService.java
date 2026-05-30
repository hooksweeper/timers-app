package com.example.timersapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TimerAlarmService extends Service {
    public static final String ACTION_START_ALARM = "com.example.timersapp.START_ALARM";
    public static final String ACTION_STOP_ALARM = "com.example.timersapp.STOP_ALARM";
    public static final String ACTION_STOP_ALL = "com.example.timersapp.STOP_ALL";
    public static final String EXTRA_TIMER_ID = "timer_id";
    public static final String EXTRA_TIMER_NAME = "timer_name";
    public static final String EXTRA_SOUND_URI = "sound_uri";

    static final String CHANNEL_ID = "timers_channel";
    private static final int FOREGROUND_ID = 1001;

    public static volatile boolean isRunning = false;
    // Accessible from MainActivity for UI sync
    static final Set<String> firingIds = Collections.synchronizedSet(new HashSet<>());

    private Ringtone ringtone;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START_ALARM.equals(action)) {
            String timerId = intent.getStringExtra(EXTRA_TIMER_ID);
            String soundUriStr = intent.getStringExtra(EXTRA_SOUND_URI);
            if (timerId != null) {
                firingIds.add(timerId);
                TimerStore.markFiring(this, timerId);
            }

            startForeground(FOREGROUND_ID, buildNotification());
            // Only start playing if not already ringing
            if (ringtone == null || !ringtone.isPlaying()) {
                playSound(soundUriStr);
            }

        } else if (ACTION_STOP_ALARM.equals(action)) {
            String timerId = intent.getStringExtra(EXTRA_TIMER_ID);
            if (timerId != null) firingIds.remove(timerId);
            if (firingIds.isEmpty()) {
                shutdown();
            } else {
                notificationManager.notify(FOREGROUND_ID, buildNotification());
            }

        } else if (ACTION_STOP_ALL.equals(action)) {
            firingIds.clear();
            TimerStore.resetFiringTimers(this);
            shutdown();
        }

        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        int count = firingIds.size();
        String title = count == 1 ? "Timer Finished!" : count + " Timers Finished!";

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopAllIntent = new Intent(this, TimerAlarmService.class);
        stopAllIntent.setAction(ACTION_STOP_ALL);
        PendingIntent stopAllPending = PendingIntent.getService(this, 1, stopAllIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText("Tap to open")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(openPending, true)
                .setContentIntent(openPending)
                .addAction(0, "Stop All", stopAllPending)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }

    private void playSound(String soundUriStr) {
        stopSound();
        try {
            Uri uri;
            if (soundUriStr != null) {
                uri = Uri.parse(soundUriStr);
            } else {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopSound() {
        if (ringtone != null) {
            if (ringtone.isPlaying()) ringtone.stop();
            ringtone = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void shutdown() {
        stopSound();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private void ensureChannel() {
        ensureNotificationChannel(this);
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Timers", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Timer countdowns and alarms");
            ch.setShowBadge(true);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setBypassDnd(true);
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        firingIds.clear();
        stopSound();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
