package com.example.timersapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

final class TimerScheduler {
    private static final String TAG = "TimerScheduler";

    private TimerScheduler() {}

    @SuppressLint({"MissingPermission", "ScheduleExactAlarm"})
    static boolean schedule(Context context, TimerModel timer) {
        AlarmManager alarmManager = getAlarmManager(context);
        if (alarmManager == null || !timer.isRunning()) return false;

        return scheduleAlarmClock(context, timer, timer.getEndTime());
    }

    static boolean scheduleImmediateAlert(Context context, TimerModel timer) {
        AlarmManager alarmManager = getAlarmManager(context);
        if (alarmManager == null) return false;

        return scheduleAlarmClock(context, timer, System.currentTimeMillis() + 1_000L);
    }

    @SuppressLint({"MissingPermission", "ScheduleExactAlarm"})
    private static boolean scheduleAlarmClock(Context context, TimerModel timer, long triggerAtMillis) {
        PendingIntent operation = createAlarmPendingIntent(
                context,
                timer,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent showIntent = createShowPendingIntent(context, timer);
        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent);

        try {
            AlarmManager alarmManager = getAlarmManager(context);
            if (alarmManager == null) return false;

            alarmManager.setAlarmClock(alarmClockInfo, operation);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to schedule timer alarm", e);
            operation.cancel();
            return false;
        }
    }

    static void cancel(Context context, TimerModel timer) {
        AlarmManager alarmManager = getAlarmManager(context);
        if (alarmManager == null) return;

        PendingIntent pending = createAlarmPendingIntent(
                context,
                timer,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pending != null) {
            alarmManager.cancel(pending);
            pending.cancel();
        }
    }

    static void reconcilePersistedState(Context context) {
        List<TimerModel> timers = TimerStore.load(context);
        TimerStateReconciler.Result result =
                TimerStateReconciler.reconcile(timers, System.currentTimeMillis());
        if (result.isChanged()) {
            TimerStore.saveImmediately(context, timers);
        }
        for (TimerModel timer : result.getTimersToSchedule()) {
            schedule(context, timer);
        }
        for (TimerModel timer : result.getTimersToAlert()) {
            scheduleImmediateAlert(context, timer);
        }
    }

    static PendingIntent createAlarmPendingIntent(Context context, TimerModel timer, int flags) {
        Intent intent = new Intent(context, TimerExpiredReceiver.class);
        intent.setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED);
        intent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_ID, timer.getId());
        intent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_NAME, timer.getName());
        intent.putExtra(TimerExpiredReceiver.EXTRA_SOUND_URI, timer.getSoundUri());
        return PendingIntent.getBroadcast(context, timer.getId().hashCode(), intent, flags);
    }

    private static PendingIntent createShowPendingIntent(Context context, TimerModel timer) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                timer.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}
