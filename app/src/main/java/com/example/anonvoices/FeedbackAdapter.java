package com.example.anonvoices;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {

    private List<Feedback> feedbackList;
    private OnFeedbackClickListener listener;

    public interface OnFeedbackClickListener {
        void onFeedbackClick(Feedback feedback);
    }

    public FeedbackAdapter(List<Feedback> feedbackList, OnFeedbackClickListener listener) {
        this.feedbackList = feedbackList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        Feedback feedback = feedbackList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvCategory.setText(feedback.getCategory());
        holder.tvMessage.setText(feedback.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(feedback.getTimestamp())));

        // Bind Category Accent Color Strips and Text Styling
        int colorAccent;
        int colorLightBackground;

        String category = feedback.getCategory() != null ? feedback.getCategory() : "Suggestion";
        switch (category) {
            case "Complaint":
                colorAccent = ContextCompat.getColor(context, R.color.cat_complaint);
                colorLightBackground = ContextCompat.getColor(context, R.color.gray_100);
                break;
            case "Question":
                colorAccent = ContextCompat.getColor(context, R.color.cat_question);
                colorLightBackground = ContextCompat.getColor(context, R.color.gray_100);
                break;
            case "Appreciation":
                colorAccent = ContextCompat.getColor(context, R.color.cat_appreciation);
                colorLightBackground = ContextCompat.getColor(context, R.color.gray_100);
                break;
            case "Suggestion":
            default:
                colorAccent = ContextCompat.getColor(context, R.color.cat_suggestion);
                colorLightBackground = ContextCompat.getColor(context, R.color.gray_100);
                break;
        }

        // Apply visual category indicators
        holder.viewCategoryAccent.setBackgroundColor(colorAccent);
        holder.tvCategory.setTextColor(colorAccent);
        
        // Set category badge background shade dynamically
        holder.tvCategory.setBackgroundTintList(ColorStateList.valueOf(colorAccent & 0x15FFFFFF | 0x1A000000)); 

        // Set Unread Dot State
        if (feedback.isRead()) {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.75f); // Subtly fade read cards to emphasize unread ones
        } else {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }

        // Bind Session Badge Details
        if (feedback.getSessionTitle() != null && !feedback.getSessionTitle().isEmpty()) {
            String tag = "Session: " + feedback.getSessionTitle() + " (Code: " + feedback.getSessionCode() + ")";
            holder.tvSessionTag.setText(tag);
            holder.tvSessionTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvSessionTag.setVisibility(View.GONE);
        }

        // Entire Card Click Listener to open detail bottom sheet
        holder.itemView.setOnClickListener(v -> listener.onFeedbackClick(feedback));
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        View viewCategoryAccent;
        TextView tvCategory, tvDate, tvMessage, tvSessionTag;
        View viewUnreadDot;

        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryAccent = itemView.findViewById(R.id.viewCategoryAccent);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvSessionTag = itemView.findViewById(R.id.tvSessionTag);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
