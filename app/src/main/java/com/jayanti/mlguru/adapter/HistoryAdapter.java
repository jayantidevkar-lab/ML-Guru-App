package com.jayanti.mlguru.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.jayanti.mlguru.R;
import com.jayanti.mlguru.database.ChatHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for conversation history list.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryActionListener {
        void onOpenChat(ChatHistory chat);
        void onRenameChat(ChatHistory chat);
        void onDeleteChat(ChatHistory chat);
    }

    private final Context context;
    private final List<ChatHistory> historyList;
    private final OnHistoryActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());

    public HistoryAdapter(Context context, List<ChatHistory> historyList, OnHistoryActionListener listener) {
        this.context = context;
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (historyList == null || position >= historyList.size()) return;

        ChatHistory chat = historyList.get(position);
        holder.tvHistoryTitle.setText(chat.getTitle() != null ? chat.getTitle() : "ML Conversation");
        holder.tvHistoryDate.setText(dateFormat.format(new Date(chat.getTime())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpenChat(chat);
        });

        holder.btnHistoryDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteChat(chat);
        });

        holder.btnHistoryMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnHistoryMore);
            popup.getMenu().add(0, 1, 0, R.string.open);
            popup.getMenu().add(0, 2, 1, R.string.rename);
            popup.getMenu().add(0, 3, 2, R.string.delete);
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1 && listener != null) {
                    listener.onOpenChat(chat);
                    return true;
                } else if (id == 2 && listener != null) {
                    listener.onRenameChat(chat);
                    return true;
                } else if (id == 3 && listener != null) {
                    listener.onDeleteChat(chat);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView tvHistoryTitle;
        public TextView tvHistoryDate;
        public ImageView btnHistoryDelete;
        public ImageView btnHistoryMore;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
            btnHistoryDelete = itemView.findViewById(R.id.btnHistoryDelete);
            btnHistoryMore = itemView.findViewById(R.id.btnHistoryMore);
        }
    }
}
