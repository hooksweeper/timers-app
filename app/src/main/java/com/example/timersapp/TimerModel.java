package com.example.timersapp;

public class TimerModel {
    private String id;
    private String name;
    private long durationSeconds;
    private long remainingSeconds;
    private boolean isRunning;
    private boolean isFiring;
    private String soundUri;

    public TimerModel(String id, String name, long durationSeconds, String soundUri) {
        this.id = id;
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
        this.isRunning = false;
        this.isFiring = false;
        this.soundUri = soundUri;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getDurationSeconds() { return durationSeconds; }
    public long getRemainingSeconds() { return remainingSeconds; }
    public boolean isRunning() { return isRunning; }
    public boolean isFiring() { return isFiring; }
    public String getSoundUri() { return soundUri; }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
    
    public void setFiring(boolean firing) {
        isFiring = firing;
    }
    
    public void setSoundUri(String soundUri) {
        this.soundUri = soundUri;
    }
}