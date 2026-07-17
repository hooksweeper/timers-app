package com.example.timersapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TimerStateReconciler {
    private TimerStateReconciler() {}

    static Result reconcile(List<TimerModel> timers, long nowMillis) {
        List<TimerModel> timersToSchedule = new ArrayList<>();
        List<TimerModel> timersToAlert = new ArrayList<>();
        boolean changed = false;

        for (TimerModel timer : timers) {
            if (timer.isFiring()) {
                changed |= normalizeFiringTimer(timer);
                timersToAlert.add(timer);
            } else if (timer.getEndTime() > 0 && timer.getEndTime() <= nowMillis) {
                timer.setEndTime(0);
                timer.setRemainingSeconds(0);
                timer.setFiring(true);
                changed = true;
                timersToAlert.add(timer);
            } else if (timer.getEndTime() > nowMillis) {
                timersToSchedule.add(timer);
            }
        }

        return new Result(changed, timersToSchedule, timersToAlert);
    }

    private static boolean normalizeFiringTimer(TimerModel timer) {
        boolean changed = false;
        if (timer.getEndTime() != 0) {
            timer.setEndTime(0);
            changed = true;
        }
        if (timer.getRemainingSeconds() != 0) {
            timer.setRemainingSeconds(0);
            changed = true;
        }
        return changed;
    }

    static final class Result {
        private final boolean changed;
        private final List<TimerModel> timersToSchedule;
        private final List<TimerModel> timersToAlert;

        private Result(
                boolean changed,
                List<TimerModel> timersToSchedule,
                List<TimerModel> timersToAlert
        ) {
            this.changed = changed;
            this.timersToSchedule = Collections.unmodifiableList(timersToSchedule);
            this.timersToAlert = Collections.unmodifiableList(timersToAlert);
        }

        boolean isChanged() {
            return changed;
        }

        List<TimerModel> getTimersToSchedule() {
            return timersToSchedule;
        }

        List<TimerModel> getTimersToAlert() {
            return timersToAlert;
        }
    }
}
