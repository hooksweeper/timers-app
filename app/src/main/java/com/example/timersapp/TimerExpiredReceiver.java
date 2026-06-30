package com.example.timersapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class TimerExpiredReceiver extends BroadcastReceiver {
    public static final String ACTION_TIMER_EXPIRED = "com.example.timersapp.TIMER_EXPIRED";
    public static final String EXTRA_TIMER_ID = "timer_id";
    public static final String EXTRA_TIMER_NAME = "timer_name";
    public static final String EXTRA_SOUND_URI = "sound_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TIMER_EXPIRED.equals(intent.getAction())) return;

        TimerAlarmService.startAlarm(
                context,
                intent.getStringExtra(EXTRA_TIMER_ID),
                intent.getStringExtra(EXTRA_TIMER_NAME),
                intent.getStringExtra(EXTRA_SOUND_URI)
        );
    }
}
