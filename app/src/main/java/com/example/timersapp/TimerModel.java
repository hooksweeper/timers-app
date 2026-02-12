package com.example.timersapp;

public class TimerModel {
    private String id;
    private String name;
    private long durationSeconds;
    private long remainingSeconds; // Used when paused/stopped
    private long endTime; // 0 if not running, otherwise system timestamp in ms
    private boolean isFiring;
    private String soundUri;

    public TimerModel(String id, String name, long durationSeconds, String soundUri) {
        this.id = id;
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
        this.endTime = 0;
        this.isFiring = false;
        this.soundUri = soundUri;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getDurationSeconds() { return durationSeconds; }
    
    public long getRemainingSeconds() { 
        if (isRunning()) {
            long left = endTime - System.currentTimeMillis();
            return Math.max(0, left / 1000);
        }
        return remainingSeconds; 
    }
    
    public boolean isRunning() { return endTime > 0; }
    public boolean isFiring() { return isFiring; }
    public String getSoundUri() { return soundUri; }
    public long getEndTime() { return endTime; }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public void setFiring(boolean firing) {
        isFiring = firing;
    }
    
    public void setSoundUri(String soundUri) {
        this.soundUri = soundUri;
    }
}