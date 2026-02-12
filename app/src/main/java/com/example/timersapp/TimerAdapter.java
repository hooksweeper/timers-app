package com.example.timersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.TimerViewHolder> {

    private List<TimerModel> timers = new ArrayList<>();
    private final OnTimerActionListener listener;

    public interface OnTimerActionListener {
        void onDelete(TimerModel timer);
        void onStopAlarm(TimerModel timer);
        void onReset(TimerModel timer);
        void onToggleTimer(TimerModel timer);
    }

    public TimerAdapter(OnTimerActionListener listener) {
        this.listener = listener;
    }

    public void setTimers(List<TimerModel> newTimers) {
        if (this.timers != newTimers) {
            this.timers = newTimers;
            notifyDataSetChanged();
        }
    }
    
    public List<TimerModel> getTimers() {
        return timers;
    }

    @NonNull
    @Override
    public TimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timer, parent, false);
        return new TimerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimerViewHolder holder, int position) {
        TimerModel timer = timers.get(position);
        holder.bind(timer);
    }

    @Override
    public int getItemCount() {
        return timers.size();
    }
    
    public void cleanup() {
        // No-op
    }

    class TimerViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, timeText;
        Button startPauseButton, resetButton, stopAlarmButton;
        ImageButton deleteButton;

        public TimerViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.timerName);
            timeText = itemView.findViewById(R.id.timerTime);
            startPauseButton = itemView.findViewById(R.id.startPauseButton);
            resetButton = itemView.findViewById(R.id.resetButton);
            stopAlarmButton = itemView.findViewById(R.id.stopAlarmButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(TimerModel timer) {
            nameText.setText(timer.getName());
            
            long min = timer.getRemainingSeconds() / 60;
            long sec = timer.getRemainingSeconds() % 60;
            timeText.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));

            if (timer.isRunning()) {
                startPauseButton.setText("Pause");
            } else {
                startPauseButton.setText("Start");
            }
            
            // Show stop button only if firing
            if (timer.isFiring()) {
                stopAlarmButton.setVisibility(View.VISIBLE);
                startPauseButton.setVisibility(View.GONE);
                resetButton.setVisibility(View.GONE);
            } else {
                stopAlarmButton.setVisibility(View.GONE);
                startPauseButton.setVisibility(View.VISIBLE);
                resetButton.setVisibility(View.VISIBLE);
            }
            
            // Disable start if 0
            startPauseButton.setEnabled(timer.getRemainingSeconds() > 0);

            startPauseButton.setOnClickListener(v -> {
                listener.onToggleTimer(timer);
            });

            stopAlarmButton.setOnClickListener(v -> {
                listener.onStopAlarm(timer);
            });

            resetButton.setOnClickListener(v -> {
                listener.onReset(timer);
            });

            deleteButton.setOnClickListener(v -> {
                listener.onDelete(timer);
            });
        }
    }
}