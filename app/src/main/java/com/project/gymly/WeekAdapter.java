package com.project.gymly;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class WeekAdapter extends RecyclerView.Adapter<WeekAdapter.DayViewHolder> {

    private final List<Day> days;
    private final OnDayClickListener onDayClickListener;
    private int selectedPosition = -1;
    private int lastSelectedPosition = -1;

    public WeekAdapter(List<Day> days, OnDayClickListener onDayClickListener) {
        this.days = days;
        this.onDayClickListener = onDayClickListener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Day day = days.get(position);
        holder.tvDayName.setText(day.getDayName());
        holder.tvDayNumber.setText(String.valueOf(day.getDayNumber()));

        if (day.isToday()) {
            holder.tvDayName.setTypeface(null, Typeface.BOLD);
            holder.tvDayNumber.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tvDayName.setTypeface(null, Typeface.NORMAL);
            holder.tvDayNumber.setTypeface(null, Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            lastSelectedPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            onDayClickListener.onDayClick(selectedPosition);
            if (lastSelectedPosition != -1) {
                notifyItemChanged(lastSelectedPosition);
            }
            notifyItemChanged(selectedPosition);
        });

        if (position == selectedPosition) {
            holder.animateSelection();
        } else {
            holder.animateDeselection();
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public interface OnDayClickListener {
        void onDayClick(int position);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName;
        TextView tvDayNumber;
        MaterialCardView cardDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            cardDay = itemView.findViewById(R.id.card_day);
        }

        void animateSelection() {
            Context context = itemView.getContext();
            int colorFrom = getThemeColor(context, com.google.android.material.R.attr.colorSurface);
            int colorTo = getThemeColor(context, com.google.android.material.R.attr.colorPrimaryContainer);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(250);
            colorAnimation.addUpdateListener(animator -> cardDay.setCardBackgroundColor((int) animator.getAnimatedValue()));
            colorAnimation.start();

            cardDay.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
        }

        void animateDeselection() {
            cardDay.setCardBackgroundColor(getThemeColor(itemView.getContext(), com.google.android.material.R.attr.colorSurface));
            cardDay.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }

        private int getThemeColor(Context context, int attr) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(attr, typedValue, true);
            return typedValue.data;
        }
    }
}
