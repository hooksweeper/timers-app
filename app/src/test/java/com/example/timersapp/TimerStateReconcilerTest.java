package com.example.timersapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TimerStateReconcilerTest {
    @Test
    public void reconcile_keepsFutureTimerRunningAndSchedulesIt() {
        long now = 1_000_000L;
        TimerModel timer = new TimerModel("future", "Tea", 60, null);
        timer.setEndTime(now + 30_000L);

        TimerStateReconciler.Result result = TimerStateReconciler.reconcile(listOf(timer), now);

        assertFalse(result.isChanged());
        assertEquals(1, result.getTimersToSchedule().size());
        assertEquals(timer, result.getTimersToSchedule().get(0));
        assertEquals(0, result.getTimersToAlert().size());
        assertTrue(timer.isRunning());
        assertFalse(timer.isFiring());
    }

    @Test
    public void reconcile_marksExpiredRunningTimerAsFiring() {
        long now = 1_000_000L;
        TimerModel timer = new TimerModel("expired", "Pasta", 120, null);
        timer.setEndTime(now - 1L);

        TimerStateReconciler.Result result = TimerStateReconciler.reconcile(listOf(timer), now);

        assertTrue(result.isChanged());
        assertEquals(0, result.getTimersToSchedule().size());
        assertEquals(1, result.getTimersToAlert().size());
        assertEquals(timer, result.getTimersToAlert().get(0));
        assertFalse(timer.isRunning());
        assertTrue(timer.isFiring());
        assertEquals(0, timer.getRemainingSeconds());
    }

    @Test
    public void reconcile_restartsPersistedFiringTimer() {
        TimerModel timer = new TimerModel("firing", "Oven", 45, null);
        timer.setRemainingSeconds(12);
        timer.setEndTime(500L);
        timer.setFiring(true);

        TimerStateReconciler.Result result = TimerStateReconciler.reconcile(listOf(timer), 1_000L);

        assertTrue(result.isChanged());
        assertEquals(0, result.getTimersToSchedule().size());
        assertEquals(1, result.getTimersToAlert().size());
        assertFalse(timer.isRunning());
        assertTrue(timer.isFiring());
        assertEquals(0, timer.getRemainingSeconds());
    }

    @Test
    public void reset_returnsTimerToFullDurationAndIdleState() {
        TimerModel timer = new TimerModel("reset", "Stretch", 90, null);
        timer.setRemainingSeconds(0);
        timer.setFiring(true);

        timer.reset();

        assertFalse(timer.isRunning());
        assertFalse(timer.isFiring());
        assertEquals(90, timer.getRemainingSeconds());
    }

    private static List<TimerModel> listOf(TimerModel timer) {
        List<TimerModel> timers = new ArrayList<>();
        timers.add(timer);
        return timers;
    }
}
