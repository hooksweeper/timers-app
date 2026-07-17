package com.example.timersapp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

final class AlarmPlayer {
    private static final String TAG = "AlarmPlayer";

    private MediaPlayer mediaPlayer;

    void start(Context context, String soundUri) {
        stop();

        Context appContext = context.getApplicationContext();
        Uri selectedUri = soundUri != null ? Uri.parse(soundUri) : null;
        if (selectedUri != null && play(appContext, selectedUri)) return;

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri != null && play(appContext, alarmUri)) return;

        Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (notificationUri != null) {
            play(appContext, notificationUri);
        }
    }

    void stop() {
        if (mediaPlayer == null) return;

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (IllegalStateException ignored) {
            // The player is already stopped or failed during setup.
        } finally {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private boolean play(Context context, Uri uri) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(context, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
            mediaPlayer = player;
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to play alarm sound: " + uri, e);
            player.release();
            return false;
        }
    }
}
