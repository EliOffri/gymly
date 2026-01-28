package com.project.gymly.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.project.gymly.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<Date> dates;
    private Date selectedDate;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public CalendarAdapter(List<Date> dates, Date selectedDate, OnDateClickListener listener) {
        this.dates = dates;
        this.selectedDate = selectedDate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        Date date = dates.get(position);
        holder.bind(date, selectedDate);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDayName, tvDayNumber;
        private final MaterialCardView card;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            card = itemView.findViewById(R.id.card_calendar_day);
        }

        public void bind(Date date, Date selected) {
            SimpleDateFormat nameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            SimpleDateFormat numberFormat = new SimpleDateFormat("d", Locale.getDefault());

            tvDayName.setText(nameFormat.format(date).toUpperCase());
            tvDayNumber.setText(numberFormat.format(date));

            boolean isSelected = isSameDay(date, selected);
            
            if (isSelected) {
                card.setCardBackgroundColor(Color.BLACK);
                tvDayName.setTextColor(Color.WHITE);
                tvDayNumber.setTextColor(Color.WHITE);
            } else {
                card.setCardBackgroundColor(Color.TRANSPARENT);
                tvDayName.setTextColor(Color.parseColor("#94A3B8"));
                tvDayNumber.setTextColor(Color.parseColor("#1E293B"));
            }

            itemView.setOnClickListener(v -> {
                selectedDate = date;
                notifyDataSetChanged();
                listener.onDateClick(date);
            });
        }

        private boolean isSameDay(Date d1, Date d2) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            return fmt.format(d1).equals(fmt.format(d2));
        }
    }
}
