package com.jayanti.mlguru;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.jayanti.mlguru.adapter.HistoryAdapter;
import com.jayanti.mlguru.database.ChatDatabase;
import com.jayanti.mlguru.database.ChatHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays saved conversation history.
 */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryActionListener {

    public static final String EXTRA_LOAD_CHAT_ID = "extra_load_chat_id";

    private RecyclerView rvHistoryList;
    private LinearLayout llEmptyHistory;
    private HistoryAdapter adapter;
    private final List<ChatHistory> historyList = new ArrayList<>();
    private ChatDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            rvHistoryList = findViewById(R.id.rvHistoryList);
            llEmptyHistory = findViewById(R.id.llEmptyHistory);

            if (rvHistoryList != null) {
                rvHistoryList.setLayoutManager(new LinearLayoutManager(this));
                adapter = new HistoryAdapter(this, historyList, this);
                rvHistoryList.setAdapter(adapter);
            }

            db = ChatDatabase.getInstance(this);
            loadHistory();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHistory() {
        try {
            if (db != null) {
                List<ChatHistory> chats = db.chatDao().getAllChats();
                historyList.clear();
                if (chats != null && !chats.isEmpty()) {
                    historyList.addAll(chats);
                    if (llEmptyHistory != null) llEmptyHistory.setVisibility(View.GONE);
                    if (rvHistoryList != null) rvHistoryList.setVisibility(View.VISIBLE);
                } else {
                    if (llEmptyHistory != null) llEmptyHistory.setVisibility(View.VISIBLE);
                    if (rvHistoryList != null) rvHistoryList.setVisibility(View.GONE);
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onOpenChat(ChatHistory chat) {
        if (chat != null) {
            Intent result = new Intent();
            result.putExtra(EXTRA_LOAD_CHAT_ID, chat.getId());
            setResult(RESULT_OK, result);
            finish();
        }
    }

    @Override
    public void onRenameChat(ChatHistory chat) {
        if (chat == null) return;
        EditText input = new EditText(this);
        input.setText(chat.getTitle());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.rename_dialog_title)
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        chat.setTitle(newTitle);
                        try {
                            db.chatDao().updateChat(chat);
                            loadHistory();
                            Toast.makeText(this, "Renamed", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Error renaming: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDeleteChat(ChatHistory chat) {
        if (chat == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        db.chatDao().deleteChatById(chat.getId());
                        loadHistory();
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
