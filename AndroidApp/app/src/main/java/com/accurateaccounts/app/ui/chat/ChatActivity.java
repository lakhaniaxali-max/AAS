package com.accurateaccounts.app.ui.chat;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.api.SocketManager;
import com.accurateaccounts.app.models.ChatMessage;
import com.accurateaccounts.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private ImageButton btnSend;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private SessionManager sessionManager;
    private Socket mSocket;
    private int currentUserId;
    private Integer otherUserId = null;
    private Integer groupId = null;
    private String roomName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        // Get chat intent extras
        if (getIntent().hasExtra("group_id")) {
            groupId = getIntent().getIntExtra("group_id", 1);
            roomName = "group_" + groupId;
        } else if (getIntent().hasExtra("other_user_id")) {
            otherUserId = getIntent().getIntExtra("other_user_id", 1);
            roomName = "user_" + currentUserId;
        } else {
            // Default demo group
            groupId = 1;
            roomName = "group_1";
        }

        MaterialToolbar toolbar = findViewById(R.id.chatToolbar);
        toolbar.setTitle(groupId != null ? "Group Chat" : "Personal Chat");
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(messageList, currentUserId);
        rvMessages.setAdapter(chatAdapter);

        // Setup Socket.IO
        initSocket();

        // Load historical messages via REST
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initSocket() {
        mSocket = SocketManager.getSocket();
        mSocket.connect();

        mSocket.emit("join_room", roomName);

        mSocket.on("receive_message", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                JSONObject obj = (JSONObject) args[0];
                try {
                    int senderId = obj.optInt("sender_id");
                    String text = obj.optString("message");
                    String firstName = obj.optString("first_name");

                    ChatMessage incoming = new ChatMessage(senderId, otherUserId, groupId, text);
                    runOnUiThread(() -> {
                        messageList.add(incoming);
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadMessages() {
        ApiClient.getService().getMessages(currentUserId, otherUserId, groupId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    messageList.addAll(response.body());
                    chatAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage newMsg = new ChatMessage(currentUserId, otherUserId, groupId, text);

        // Send via REST endpoint (which also emits via socket in chatRoutes.js)
        ApiClient.getService().sendMessage(newMsg).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                etMessageInput.setText("");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Message send failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.emit("leave_room", roomName);
        }
    }
}

