package com.example.timersapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TimerAdapter.OnTimerActionListener {

    private TimerAdapter adapter;
    private List<TimerModel> timers = new ArrayList<>();
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private Runnable tickerRunnable;

    private static final String PREFS_NAME = "TimerPrefs";
    private static final String KEY_TIMERS = "saved_timers";
    
    private AlarmManager alarmManager;
    
    // For selecting sound in dialog
    private Uri tempSelectedSoundUri;
    private TextView tempSoundNameView;
    
    // For playing alarm
    private Ringtone currentRingtone;
    private Ringtone previewRingtone;
    private final Handler previewHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> soundPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        tempSelectedSoundUri = uri;
                        if (tempSoundNameView != null) {
                            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                            tempSoundNameView.setText(ringtone.getTitle(this));
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        checkExactAlarmPermission();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new TimerAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddTimer);
        fab.setOnClickListener(v -> showAddTimerDialog());

        loadTimers();
        startGlobalTicker();
        
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        if (TimerExpiredReceiver.ACTION_TIMER_EXPIRED.equals(intent.getAction())) {
            String timerId = intent.getStringExtra(TimerExpiredReceiver.EXTRA_TIMER_ID);
            if (timerId != null) {
                for (int i = 0; i < timers.size(); i++) {
                    TimerModel t = timers.get(i);
                    if (t.getId().equals(timerId)) {
                        t.setEndTime(0);
                        t.setFiring(true);
                        playAlarmSound(t.getSoundUri());
                        adapter.notifyItemChanged(i);
                        saveTimers();
                        break;
                    }
                }
            }
        }
    }
    
    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void startGlobalTicker() {
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < timers.size(); i++) {
                    TimerModel t = timers.get(i);
                    if (t.isRunning()) {
                        long remaining = t.getRemainingSeconds();
                        if (remaining <= 0) {
                            // Handled by Alarm/Receiver, but safe fallback for UI while open
                            if (!t.isFiring()) {
                                t.setEndTime(0);
                                t.setFiring(true);
                                playAlarmSound(t.getSoundUri());
                                saveTimers();
                            }
                        }
                        adapter.notifyItemChanged(i);
                    }
                }
                tickerHandler.postDelayed(this, 1000);
            }
        };
        tickerHandler.post(tickerRunnable);
    }

    private void scheduleAlarm(TimerModel timer) {
        if (alarmManager != null && timer.isRunning()) {
            Intent intent = new Intent(this, TimerExpiredReceiver.class);
            intent.setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED);
            intent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_ID, timer.getId());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 
                    timer.getId().hashCode(), 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pendingIntent);
            }
        }
    }

    private void cancelAlarm(TimerModel timer) {
        if (alarmManager != null) {
            Intent intent = new Intent(this, TimerExpiredReceiver.class);
            intent.setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 
                    timer.getId().hashCode(), 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
            );
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private void playAlarmSound(String soundUriStr) {
        try {
            Uri notification;
            if (soundUriStr != null) {
                notification = Uri.parse(soundUriStr);
            } else {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (notification == null) {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
            
            // Stop previous if playing
            if (currentRingtone != null && currentRingtone.isPlaying()) {
                currentRingtone.stop();
            }

            currentRingtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            currentRingtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAddTimerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_timer, null);
        EditText editName = view.findViewById(R.id.editTimerName);
        EditText editMin = view.findViewById(R.id.editDurationMin);
        EditText editSec = view.findViewById(R.id.editDurationSec);
        
        tempSoundNameView = view.findViewById(R.id.textSoundName);
        Button btnSelectSound = view.findViewById(R.id.btnSelectSound);
        Button btnPreviewSound = view.findViewById(R.id.btnPreviewSound);
        
        // Reset selected sound for new dialog
        tempSelectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (tempSelectedSoundUri == null) {
            tempSelectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        // Update initial sound name
        if (tempSelectedSoundUri != null) {
            Ringtone r = RingtoneManager.getRingtone(this, tempSelectedSoundUri);
            if (r != null) tempSoundNameView.setText(r.getTitle(this));
        }
        
        btnPreviewSound.setOnClickListener(v -> {
            if (previewRingtone != null && previewRingtone.isPlaying()) {
                previewRingtone.stop();
                previewHandler.removeCallbacksAndMessages(null);
            }
            
            if (tempSelectedSoundUri != null) {
                previewRingtone = RingtoneManager.getRingtone(this, tempSelectedSoundUri);
                if (previewRingtone != null) {
                    previewRingtone.play();
                    // Stop after 10 seconds
                    previewHandler.postDelayed(() -> {
                        if (previewRingtone != null && previewRingtone.isPlaying()) {
                            previewRingtone.stop();
                        }
                    }, 10000);
                }
            }
        });
        
        btnSelectSound.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Timer Sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tempSelectedSoundUri);
            soundPickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_timer)
                .setView(view)
                .setPositiveButton(R.string.create, (dialog, which) -> {
                    if (previewRingtone != null) previewRingtone.stop();
                    previewHandler.removeCallbacksAndMessages(null);
                    
                    String name = editName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) name = "Timer " + (timers.size() + 1);
                    
                    String minStr = editMin.getText().toString().trim();
                    String secStr = editSec.getText().toString().trim();
                    
                    long min = minStr.isEmpty() ? 0 : Long.parseLong(minStr);
                    long sec = secStr.isEmpty() ? 0 : Long.parseLong(secStr);
                    long totalSec = (min * 60) + sec;
                    
                    if (totalSec <= 0) {
                        Toast.makeText(this, "Duration must be > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String uriString = (tempSelectedSoundUri != null) ? tempSelectedSoundUri.toString() : null;

                    TimerModel newTimer = new TimerModel(UUID.randomUUID().toString(), name, totalSec, uriString);
                    timers.add(newTimer);
                    adapter.notifyItemInserted(timers.size() - 1);
                    saveTimers();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (previewRingtone != null) previewRingtone.stop();
                    previewHandler.removeCallbacksAndMessages(null);
                })
                .show();
    }

    private void loadTimers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_TIMERS, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<TimerModel>>(){}.getType();
            List<TimerModel> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                timers = loaded;
                // Ensure running state is reset on app launch to avoid "running in background" confusion for this simple app
                // Or we could keep them running if we used timestamps.
                // For simplicity: pause all on load.
                for (TimerModel t : timers) {
                    t.setEndTime(0);
                    t.setFiring(false);
                } 
            }
        }
        adapter.setTimers(timers);
    }

    private void saveTimers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(timers);
        editor.putString(KEY_TIMERS, json);
        editor.apply();
    }

    @Override
    public void onDelete(TimerModel timer) {
        cancelAlarm(timer);
        int pos = timers.indexOf(timer);
        if (pos != -1) {
            timers.remove(pos);
            adapter.notifyItemRemoved(pos);
            saveTimers();
        }
    }

    @Override
    public void onStopAlarm(TimerModel timer) {
        cancelAlarm(timer);
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }
        int index = timers.indexOf(timer);
        if (index != -1) {
            timer.setFiring(false);
            timer.setRemainingSeconds(timer.getDurationSeconds());
            timer.setEndTime(0);
            adapter.notifyItemChanged(index);
            saveTimers();
        }
    }

    @Override
    public void onReset(TimerModel timer) {
        cancelAlarm(timer);
        int index = timers.indexOf(timer);
        if (index != -1) {
            timer.setEndTime(0);
            timer.setFiring(false);
            timer.setRemainingSeconds(timer.getDurationSeconds());
            adapter.notifyItemChanged(index);
            saveTimers();
        }
    }

    @Override
    public void onToggleTimer(TimerModel timer) {
        int index = timers.indexOf(timer);
        if (index == -1) return;

        if (timer.isRunning()) {
            // Pause
            long remaining = timer.getRemainingSeconds();
            timer.setRemainingSeconds(remaining);
            timer.setEndTime(0);
            cancelAlarm(timer);
        } else {
            // Start
            long endTime = System.currentTimeMillis() + (timer.getRemainingSeconds() * 1000);
            timer.setEndTime(endTime);
            scheduleAlarm(timer);
        }
        
        adapter.notifyItemChanged(index);
        saveTimers();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        tickerHandler.removeCallbacksAndMessages(null);
        previewHandler.removeCallbacksAndMessages(null);
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }
        if (previewRingtone != null && previewRingtone.isPlaying()) {
            previewRingtone.stop();
        }
        if (adapter != null) adapter.cleanup();
    }
}