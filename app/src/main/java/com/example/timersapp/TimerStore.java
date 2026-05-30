package com.example.timersapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

final class TimerStore {
    static final String PREFS_NAME = "TimerPrefs";
    static final String KEY_TIMERS = "saved_timers";

    private static final Gson GSON = new Gson();
    private static final Type TIMER_LIST_TYPE = new TypeToken<List<TimerModel>>() {}.getType();

    private TimerStore() {}

    static List<TimerModel> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_TIMERS, null);
        if (json == null) return new ArrayList<>();

        try {
            List<TimerModel> timers = GSON.fromJson(json, TIMER_LIST_TYPE);
            return timers != null ? timers : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }

    static void save(Context context, List<TimerModel> timers) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TIMERS, GSON.toJson(timers))
                .apply();
    }

    static void markFiring(Context context, String timerId) {
        if (timerId == null) return;

        List<TimerModel> timers = load(context);
        boolean changed = false;
        for (TimerModel timer : timers) {
            if (timerId.equals(timer.getId())) {
                timer.setEndTime(0);
                timer.setRemainingSeconds(0);
                timer.setFiring(true);
                changed = true;
                break;
            }
        }
        if (changed) save(context, timers);
    }

    static void resetFiringTimers(Context context) {
        List<TimerModel> timers = load(context);
        boolean changed = false;
        for (TimerModel timer : timers) {
            if (timer.isFiring()) {
                timer.reset();
                changed = true;
            }
        }
        if (changed) save(context, timers);
    }
}
