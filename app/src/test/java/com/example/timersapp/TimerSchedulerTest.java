package com.example.timersapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TimerSchedulerTest {
    @Test
    public void schedule_usesAlarmClockAlarm() {
        Context context = RuntimeEnvironment.getApplication();
        TimerModel timer = new TimerModel("timer-id", "Laundry", 60, null);
        timer.setEndTime(System.currentTimeMillis() + 60_000L);

        assertTrue(TimerScheduler.schedule(context, timer));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assertNotNull(alarmManager.getNextAlarmClock());
    }

    @Test
    public void cancel_removesAlarmPendingIntent() {
        Context context = RuntimeEnvironment.getApplication();
        TimerModel timer = new TimerModel("cancel-id", "Coffee", 60, null);
        timer.setEndTime(System.currentTimeMillis() + 60_000L);

        assertTrue(TimerScheduler.schedule(context, timer));
        TimerScheduler.cancel(context, timer);

        PendingIntent pendingIntent = TimerScheduler.createAlarmPendingIntent(
                context,
                timer,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        assertNull(pendingIntent);
    }

    @Test
    public void schedule_returnsFalseForIdleTimer() {
        Context context = RuntimeEnvironment.getApplication();
        TimerModel timer = new TimerModel("idle-id", "Idle", 60, null);

        assertFalse(TimerScheduler.schedule(context, timer));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assertNull(alarmManager.getNextAlarmClock());
    }

    @Test
    public void scheduleImmediateAlert_usesAlarmClockAlarm() {
        Context context = RuntimeEnvironment.getApplication();
        TimerModel timer = new TimerModel("alert-id", "Alert", 60, null);
        timer.setFiring(true);

        assertTrue(TimerScheduler.scheduleImmediateAlert(context, timer));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assertNotNull(alarmManager.getNextAlarmClock());
    }
}
