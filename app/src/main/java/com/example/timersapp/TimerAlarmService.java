package com.example.timersapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
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
    static final String RUNNING_CHANNEL_ID = "timers_running_channel";
    private static final int FOREGROUND_ID = 1001;

    private static final Set<String> firingIds = Collections.synchronizedSet(new HashSet<>());

    private final AlarmPlayer alarmPlayer = new AlarmPlayer();
    private NotificationManager notificationManager;

    static void startAlarm(Context context, TimerModel timer) {
        startAlarm(context, timer.getId(), timer.getName(), timer.getSoundUri());
    }

    static void startAlarm(Context context, String timerId, String timerName, String soundUri) {
        Intent intent = new Intent(context, TimerAlarmService.class);
        intent.setAction(TimerAlarmService.ACTION_START_ALARM);
        intent.putExtra(TimerAlarmService.EXTRA_TIMER_ID, timerId);
        intent.putExtra(TimerAlarmService.EXTRA_TIMER_NAME, timerName);
        intent.putExtra(TimerAlarmService.EXTRA_SOUND_URI, soundUri);
        startAlarmService(context, intent);
    }

    static void stopAlarm(Context context, String timerId) {
        Intent intent = new Intent(context, TimerAlarmService.class);
        intent.setAction(ACTION_STOP_ALARM);
        intent.putExtra(EXTRA_TIMER_ID, timerId);
        context.startService(intent);
    }

    private static void startAlarmService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
            if (timerId == null || !TimerStore.markFiring(this, timerId)) {
                if (firingIds.isEmpty()) {
                    stopSelf(startId);
                }
                return START_NOT_STICKY;
            }

            boolean isNewFiringTimer = false;
            isNewFiringTimer = firingIds.add(timerId);

            startForegroundForAlarm(buildNotification());
            if (isNewFiringTimer || !alarmPlayer.isPlaying()) {
                alarmPlayer.start(this, soundUriStr);
            }

        } else if (ACTION_STOP_ALARM.equals(action)) {
            String timerId = intent.getStringExtra(EXTRA_TIMER_ID);
            if (timerId != null) {
                firingIds.remove(timerId);
                TimerStore.resetFiringTimer(this, timerId);
            }
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
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(openPending, true)
                .setContentIntent(openPending)
                .addAction(0, "Stop All", stopAllPending)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }

    private void startForegroundForAlarm(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    FOREGROUND_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            );
        } else {
            startForeground(FOREGROUND_ID, notification);
        }
    }

    @SuppressWarnings("deprecation")
    private void shutdown() {
        alarmPlayer.stop();
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

            NotificationChannel runningChannel = new NotificationChannel(
                    RUNNING_CHANNEL_ID,
                    "Running timers",
                    NotificationManager.IMPORTANCE_LOW
            );
            runningChannel.setDescription("Timers currently counting down");
            runningChannel.setShowBadge(false);
            nm.createNotificationChannel(runningChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        firingIds.clear();
        alarmPlayer.stop();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
