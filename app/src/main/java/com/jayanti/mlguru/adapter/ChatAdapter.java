package com.jayanti.mlguru.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jayanti.mlguru.R;
import com.jayanti.mlguru.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying user and bot chat messages.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private final Context context;
    private final List<ChatMessage> messageList;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(Context context, List<ChatMessage> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList != null && position < messageList.size()) {
            if (messageList.get(position).getSender() == ChatMessage.SENDER_USER) {
                return VIEW_TYPE_USER;
            }
        }
        return VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_message, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bot_message, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (messageList == null || position >= messageList.size()) return;

        ChatMessage message = messageList.get(position);
        String formattedTime = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            if (userHolder.tvUserMessage != null) {
                userHolder.tvUserMessage.setText(message.getMessage());
            }
            if (userHolder.tvUserTime != null) {
                userHolder.tvUserTime.setText(formattedTime);
            }
            if (userHolder.ivUserImagePreview != null) {
                if (message.getImageUri() != null && !message.getImageUri().isEmpty()) {
                    userHolder.ivUserImagePreview.setVisibility(View.VISIBLE);
                    try {
                        userHolder.ivUserImagePreview.setImageURI(Uri.parse(message.getImageUri()));
                    } catch (Exception e) {
                        userHolder.ivUserImagePreview.setVisibility(View.GONE);
                    }
                } else {
                    userHolder.ivUserImagePreview.setVisibility(View.GONE);
                }
            }
        } else if (holder instanceof BotViewHolder) {
            BotViewHolder botHolder = (BotViewHolder) holder;
            if (botHolder.tvBotMessage != null) {
                botHolder.tvBotMessage.setText(message.getMessage());
            }
            if (botHolder.tvBotTime != null) {
                botHolder.tvBotTime.setText(formattedTime);
            }
            if (botHolder.btnCopyMessage != null) {
                botHolder.btnCopyMessage.setOnClickListener(v -> {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("ML Guru Answer", message.getMessage());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivUserImagePreview;
        public TextView tvUserMessage;
        public TextView tvUserTime;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserImagePreview = itemView.findViewById(R.id.ivUserImagePreview);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvUserTime = itemView.findViewById(R.id.tvUserTime);
        }
    }

    public static class BotViewHolder extends RecyclerView.ViewHolder {
        public TextView tvBotMessage;
        public TextView tvBotTime;
        public ImageView btnCopyMessage;

        public BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            tvBotTime = itemView.findViewById(R.id.tvBotTime);
            btnCopyMessage = itemView.findViewById(R.id.btnCopyMessage);
        }
    }
}
