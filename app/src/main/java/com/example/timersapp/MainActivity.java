package com.example.timersapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TimerAdapter.OnTimerActionListener {

    private TimerAdapter adapter;
    private List<TimerModel> timers = new ArrayList<>();
    private TextView emptyState;
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private Runnable tickerRunnable;

    private AlarmManager alarmManager;
    private NotificationManager notificationManager;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notification permission denied — alarms won't show", Toast.LENGTH_LONG).show();
                }
            });

    // For sound picker in the add-timer dialog
    private Uri tempSelectedSoundUri;
    private TextView tempSoundNameView;
    private Ringtone previewRingtone;
    private final Handler previewHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> soundPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = getPickedRingtoneUri(result.getData());
                    if (uri != null) {
                        tempSelectedSoundUri = uri;
                        if (tempSoundNameView != null) {
                            Ringtone r = RingtoneManager.getRingtone(this, uri);
                            if (r != null) tempSoundNameView.setText(r.getTitle(this));
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lockscreen so full-screen alarm intent works
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            addLegacyLockScreenFlags();
        }

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        TimerAlarmService.ensureNotificationChannel(this);

        requestNotificationPermission();
        checkExactAlarmPermission();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (recyclerView.getItemAnimator() != null) {
            recyclerView.getItemAnimator().setChangeDuration(0);
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        adapter = new TimerAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddTimer);
        fab.setOnClickListener(v -> showAddTimerDialog());

        loadTimers();
        startGlobalTicker();
        handleIntent(getIntent());
    }

    @SuppressWarnings("deprecation")
    private Uri getPickedRingtoneUri(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
        }
        return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
    }

    @SuppressWarnings("deprecation")
    private void addLegacyLockScreenFlags() {
        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the user stopped alarms from the notification while the app was backgrounded,
        // refresh from persisted state.
        if (!TimerAlarmService.isRunning) {
            timers = TimerStore.load(this);
            adapter.setTimers(timers);
            updateEmptyState();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        // Nothing special needed here — the service handles sound,
        // and loadTimers/onResume sync the firing state.
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Exact Alarm Permission")
                        .setMessage("For precise timer accuracy, grant exact alarm permission in Settings.")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                        })
                        .setNegativeButton("Not Now", null)
                        .show();
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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
                        if (remaining <= 0 && !t.isFiring()) {
                            // Fallback in case AlarmManager didn't fire (e.g. no exact alarm permission)
                            t.setEndTime(0);
                            t.setRemainingSeconds(0);
                            t.setFiring(true);
                            startAlarmService(t);
                            saveTimers();
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
        if (alarmManager == null || !timer.isRunning()) return;

        Intent intent = new Intent(this, TimerExpiredReceiver.class);
        intent.setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED);
        intent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_ID, timer.getId());
        intent.putExtra(TimerExpiredReceiver.EXTRA_TIMER_NAME, timer.getName());
        intent.putExtra(TimerExpiredReceiver.EXTRA_SOUND_URI, timer.getSoundUri());

        PendingIntent pending = PendingIntent.getBroadcast(
                this,
                timer.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
        }
    }

    private void cancelAlarm(TimerModel timer) {
        if (alarmManager == null) return;
        Intent intent = new Intent(this, TimerExpiredReceiver.class);
        intent.setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED);
        PendingIntent pending = PendingIntent.getBroadcast(
                this,
                timer.getId().hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pending != null) {
            alarmManager.cancel(pending);
            pending.cancel();
        }
    }

    private void cancelNotification(TimerModel timer) {
        notificationManager.cancel(timer.getId().hashCode());
    }

    private void startAlarmService(TimerModel timer) {
        Intent intent = new Intent(this, TimerAlarmService.class);
        intent.setAction(TimerAlarmService.ACTION_START_ALARM);
        intent.putExtra(TimerAlarmService.EXTRA_TIMER_ID, timer.getId());
        intent.putExtra(TimerAlarmService.EXTRA_TIMER_NAME, timer.getName());
        intent.putExtra(TimerAlarmService.EXTRA_SOUND_URI, timer.getSoundUri());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopAlarmService(String timerId) {
        Intent intent = new Intent(this, TimerAlarmService.class);
        intent.setAction(TimerAlarmService.ACTION_STOP_ALARM);
        intent.putExtra(TimerAlarmService.EXTRA_TIMER_ID, timerId);
        startService(intent);
    }

    private void showAddTimerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_timer, null);
        TextInputEditText editName = view.findViewById(R.id.editTimerName);
        TextInputEditText editMin = view.findViewById(R.id.editDurationMin);
        TextInputEditText editSec = view.findViewById(R.id.editDurationSec);

        tempSoundNameView = view.findViewById(R.id.textSoundName);
        Button btnSelectSound = view.findViewById(R.id.btnSelectSound);
        Button btnPreviewSound = view.findViewById(R.id.btnPreviewSound);

        tempSelectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (tempSelectedSoundUri == null) {
            tempSelectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
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
                    previewHandler.postDelayed(() -> {
                        if (previewRingtone != null && previewRingtone.isPlaying()) previewRingtone.stop();
                    }, 10_000);
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_timer)
                .setView(view)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(R.string.cancel, (d, which) -> stopPreview())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    if (previewRingtone != null) previewRingtone.stop();
                    previewHandler.removeCallbacksAndMessages(null);

                    String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(name)) name = "Timer " + (timers.size() + 1);

                    String minStr = editMin.getText() != null ? editMin.getText().toString().trim() : "";
                    String secStr = editSec.getText() != null ? editSec.getText().toString().trim() : "";
                    long min = minStr.isEmpty() ? 0 : Long.parseLong(minStr);
                    long sec = secStr.isEmpty() ? 0 : Long.parseLong(secStr);
                    if (sec > 59) {
                        editSec.setError(getString(R.string.seconds_range_error));
                        return;
                    }
                    long totalSec = (min * 60) + sec;

                    if (totalSec <= 0) {
                        editMin.setError(getString(R.string.duration_required_error));
                        return;
                    }

                    String uriString = tempSelectedSoundUri != null ? tempSelectedSoundUri.toString() : null;
                    TimerModel newTimer = new TimerModel(UUID.randomUUID().toString(), name, totalSec, uriString);
                    timers.add(newTimer);
                    adapter.notifyItemInserted(timers.size() - 1);
                    saveTimers();
                    updateEmptyState();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, R.string.duration_too_large_error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.setOnDismissListener(d -> stopPreview());
        dialog.show();
    }

    private void loadTimers() {
        timers = TimerStore.load(this);
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (TimerModel t : timers) {
            if (t.getEndTime() > 0 && t.getEndTime() <= now) {
                t.setEndTime(0);
                t.setRemainingSeconds(0);
                t.setFiring(true);
                changed = true;
                if (!TimerAlarmService.firingIds.contains(t.getId())) {
                    startAlarmService(t);
                }
            }
        }
        if (changed) saveTimers();
        adapter.setTimers(timers);
        updateEmptyState();
    }

    private void saveTimers() {
        TimerStore.save(this, timers);
    }

    @Override
    public void onDelete(TimerModel timer) {
        cancelAlarm(timer);
        cancelNotification(timer);
        if (timer.isFiring()) stopAlarmService(timer.getId());
        int pos = timers.indexOf(timer);
        if (pos != -1) {
            timers.remove(pos);
            adapter.notifyItemRemoved(pos);
            saveTimers();
            updateEmptyState();
        }
    }

    @Override
    public void onStopAlarm(TimerModel timer) {
        stopAlarmService(timer.getId());
        cancelAlarm(timer);
        cancelNotification(timer);
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
        cancelNotification(timer);
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
            // Pause: snapshot remaining time, cancel alarm
            timer.setRemainingSeconds(timer.getRemainingSeconds());
            timer.setEndTime(0);
            cancelAlarm(timer);
            cancelNotification(timer);
        } else {
            // Start: schedule alarm and show notification
            long endTime = System.currentTimeMillis() + (timer.getRemainingSeconds() * 1000L);
            timer.setEndTime(endTime);
            scheduleAlarm(timer);
            updateRunningNotification(timer);
        }

        adapter.notifyItemChanged(index);
        saveTimers();
    }

    private void updateRunningNotification(TimerModel timer) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, TimerAlarmService.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(timer.getName())
                        .setContentText("Timer running")
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setContentIntent(pi)
                        .setUsesChronometer(true)
                        .setChronometerCountDown(true)
                        .setWhen(System.currentTimeMillis() + (timer.getRemainingSeconds() * 1000L));

        notificationManager.notify(timer.getId().hashCode(), builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tickerHandler.removeCallbacksAndMessages(null);
        previewHandler.removeCallbacksAndMessages(null);
        stopPreview();
        if (adapter != null) adapter.cleanup();
    }

    private void stopPreview() {
        previewHandler.removeCallbacksAndMessages(null);
        if (previewRingtone != null && previewRingtone.isPlaying()) previewRingtone.stop();
        previewRingtone = null;
    }

    private void updateEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(timers.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
