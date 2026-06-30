package com.example.timersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        setHasStableIds(true);
    }

    public void setTimers(List<TimerModel> newTimers) {
        if (this.timers != newTimers) {
            DiffUtil.DiffResult diffResult =
                    DiffUtil.calculateDiff(new TimerDiffCallback(this.timers, newTimers));
            this.timers = newTimers;
            diffResult.dispatchUpdatesTo(this);
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

    @Override
    public long getItemId(int position) {
        return timers.get(position).getId().hashCode();
    }
    
    public void cleanup() {
        // No-op
    }

    private static class TimerDiffCallback extends DiffUtil.Callback {
        private final List<TimerModel> oldTimers;
        private final List<TimerModel> newTimers;

        TimerDiffCallback(List<TimerModel> oldTimers, List<TimerModel> newTimers) {
            this.oldTimers = oldTimers;
            this.newTimers = newTimers;
        }

        @Override
        public int getOldListSize() {
            return oldTimers.size();
        }

        @Override
        public int getNewListSize() {
            return newTimers.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(
                    oldTimers.get(oldItemPosition).getId(),
                    newTimers.get(newItemPosition).getId()
            );
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            TimerModel oldTimer = oldTimers.get(oldItemPosition);
            TimerModel newTimer = newTimers.get(newItemPosition);
            return Objects.equals(oldTimer.getName(), newTimer.getName())
                    && oldTimer.getDurationSeconds() == newTimer.getDurationSeconds()
                    && oldTimer.getRemainingSeconds() == newTimer.getRemainingSeconds()
                    && oldTimer.getEndTime() == newTimer.getEndTime()
                    && oldTimer.isFiring() == newTimer.isFiring()
                    && Objects.equals(oldTimer.getSoundUri(), newTimer.getSoundUri());
        }
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

            long remainingSeconds = timer.getRemainingSeconds();
            long min = remainingSeconds / 60;
            long sec = remainingSeconds % 60;
            timeText.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));

            if (timer.isRunning()) {
                startPauseButton.setText(itemView.getContext().getString(R.string.pause));
            } else {
                startPauseButton.setText(itemView.getContext().getString(R.string.start));
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
            startPauseButton.setEnabled(remainingSeconds > 0);

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
