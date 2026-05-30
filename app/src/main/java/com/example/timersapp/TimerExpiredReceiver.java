package com.example.timersapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TimerExpiredReceiver extends BroadcastReceiver {
    public static final String ACTION_TIMER_EXPIRED = "com.example.timersapp.TIMER_EXPIRED";
    public static final String EXTRA_TIMER_ID = "timer_id";
    public static final String EXTRA_TIMER_NAME = "timer_name";
    public static final String EXTRA_SOUND_URI = "sound_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TIMER_EXPIRED.equals(intent.getAction())) return;

        Intent serviceIntent = new Intent(context, TimerAlarmService.class);
        serviceIntent.setAction(TimerAlarmService.ACTION_START_ALARM);
        serviceIntent.putExtra(TimerAlarmService.EXTRA_TIMER_ID, intent.getStringExtra(EXTRA_TIMER_ID));
        serviceIntent.putExtra(TimerAlarmService.EXTRA_TIMER_NAME, intent.getStringExtra(EXTRA_TIMER_NAME));
        serviceIntent.putExtra(TimerAlarmService.EXTRA_SOUND_URI, intent.getStringExtra(EXTRA_SOUND_URI));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
