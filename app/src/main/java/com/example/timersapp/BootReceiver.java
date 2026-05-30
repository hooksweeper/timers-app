package com.example.timersapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        List<TimerModel> timers = TimerStore.load(context);
        if (timers.isEmpty()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long now = System.currentTimeMillis();
        for (TimerModel timer : timers) {
            if (timer.getEndTime() > now) {
                Intent alarmIntent = new Intent(context, TimerExpiredReceiver.class);
                alarmIntent.setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED);
                alarmIntent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_ID, timer.getId());
                alarmIntent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_NAME, timer.getName());
                alarmIntent.putExtra(TimerExpiredReceiver.EXTRA_SOUND_URI, timer.getSoundUri());

                PendingIntent pending = PendingIntent.getBroadcast(context,
                        timer.getId().hashCode(), alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
                }
            }
        }
    }
}
