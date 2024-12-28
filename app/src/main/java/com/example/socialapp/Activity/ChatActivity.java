package com.example.socialapp.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Manager.AppStatus;
import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Message;
import com.example.socialapp.Adapter.MessageAdapter;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.R;
import com.example.socialapp.Manager.RoomManager;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private List<ChatRoom> chatList;
    private WebSocket webSocket;
    private EditText etMessage;
    private Button btnSend;
    private Button btnImage;
    int myId;
    int roomId;
    String myName;
    String title;
    String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatting);

        recyclerView = findViewById(R.id.rv_chatting);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnImage = findViewById(R.id.btn_image_send);

        chatList = RoomManager.getInstance().getRoomList();

        Date date = new Date();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        time = inputFormat.format(date);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        Intent intent = getIntent();
        roomId = intent.getIntExtra("room_id", -1);
        title = intent.getStringExtra("title");

        if(title == null || title.equals("")){  // 전송 메시지의 채팅방 이름
            title = myName;
        }

        AppStatus.getInstance().setRoomId(roomId);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, myId, myName);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        Call<ResponseBody> call = retrofitService.getMessages(roomId);
        call.enqueue(new Callback<ResponseBody>() {     // 채팅방 메시지 초기화
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        messageList.clear(); // 리스트 초기화
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int room_id = jsonObject.getInt("room_id");
                            int sender_id = jsonObject.getInt("sender_id");
                            String type = jsonObject.getString("type");
                            String title = jsonObject.optString("title", null);
                            String messageText = jsonObject.getString("message_text");
                            String name = jsonObject.getString("name");

                            messageList.add(new Message(room_id, title, sender_id, name, type, messageText, time));
                        }
                        messageAdapter.notifyDataSetChanged(); // 어댑터에 변경 알림
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ChatActivity", "Failed to load messages", t);
            }
        });

        // WebSocket 초기화
        initWebSocket(roomId,myId);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    sendMessage(roomId, myId, title, myName);
            }
        });

        btnImage.setOnClickListener(new View.OnClickListener() {        // 미디어 전송
            @Override
            public void onClick(View v) {
                Intent mediaIntent = new Intent(Intent.ACTION_PICK);
                mediaIntent.setType("*/*"); // 모든 파일 형식을 대상으로 설정
                mediaIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"}); // 이미지와 동영상 MIME 타입 지정
                mediaIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 다중 선택 허용
                startActivityForResult(mediaIntent, 100);


            }
        });

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {    // 키보드 활성화(전송,미디어)
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);

                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                // 키보드가 열려 있는지 확인 (키보드 높이가 전체 화면의 15% 이상일 때)
                if (keypadHeight > screenHeight * 0.10) {
                    btnImage.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                } else {
                    if(!etMessage.getText().toString().equals("")) {
                        btnImage.setVisibility(View.GONE);
                        btnSend.setVisibility(View.VISIBLE);
                    } else {
                        btnSend.setVisibility(View.GONE);
                        btnImage.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                // 다중 선택
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri fileUri = data.getClipData().getItemAt(i).getUri();
                    uploadMedia(fileUri);
                }
            } else if (data.getData() != null) {
                // 단일 선택
                Uri fileUri = data.getData();
                uploadMedia(fileUri);
            }
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        AppStatus.isChatActivityActive = true;
        AppStatus.getInstance().setRoomId(roomId);
    }

    @Override
    public void finish() {
        AppStatus.isChatActivityActive = false;
        AppStatus.getInstance().setRoomId(0);
        setResult(Activity.RESULT_OK);
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.close(1000, "Goodbye");
    }

    private void initWebSocket(int roomId, int myId) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(RetrofitInstance.getWebUrl()).build(); // WebSocket 서버 URL
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("ChatActivity", "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        int room_id = jsonObject.getInt("room_id");
                        int sender_id = jsonObject.getInt("sender_id");
                        String type = jsonObject.getString("type");
                        String title = jsonObject.optString("title", null);
                        String messageText = jsonObject.getString("message_text");
                        String name = jsonObject.getString("name");

                        // 메시지를 받았을 때 리스트에 추가하고 어댑터에 알림
                        if(sender_id != myId && roomId == room_id) {
                            messageList.add(new Message(room_id, title, sender_id, name,type,messageText, time));
                            messageAdapter.notifyDataSetChanged();
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("ChatActivity", "WebSocket error", t);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                boolean check = false;
                for(ChatRoom chatRoom : chatList) {
                    if(chatRoom.getRoomId() == roomId) {
                        check = true;
                    }
                }
                if(!check) {        // 새롭게 추가된 방이 있을 시 채팅방 조회
                    getGroupRooms(myId);
                }
                webSocket.close(1000, null);
                Log.d("ChatActivity", "WebSocket closing: " + reason);
            }
        });
    }

    private void sendMessage(int roomId, int myId, String title, String myName) {       // 메시지 보내기
        String message_text = etMessage.getText().toString().trim();

        if (!message_text.isEmpty()) {
            Message message = new Message(roomId, title, myId, myName, "text", message_text, time);

            Gson gson = new Gson();
            String messageJson = gson.toJson(message);

            RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
            Call<Void> call = retrofitService.sendMessage(message); // roomId, senderId는 예시
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        messageList.add(new Message(roomId, title, myId, myName, "text", message_text, time));
                        webSocket.send(messageJson);
                        messageAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        etMessage.setText("");
                    } else {
                        Log.e("ChatActivity", "Failed to send message");
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e("ChatActivity", "Failed to send message", t);
                }
            });
        }
    }

    public void getGroupRooms(int userId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getGroupRooms(userId);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try{
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        chatList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int roomId = jsonObject.getInt("room_id");
                            String roomName = jsonObject.getString("room_name");
                            String text = jsonObject.getString("message_text");
                            String createdAt = jsonObject.getString("created_at");
                            int check = jsonObject.getInt("g_check");

                            getMembers(roomId, roomName, text, createdAt, check);
                        }
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("ChatActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("ChatActivity", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("ChatActivity", "Failed to load rooms", t);
            }
        });
        getRooms(userId);
    }

    public void getRooms(int userId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getRooms(userId);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try{
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int roomId = jsonObject.getInt("room_id");
                            String roomName = jsonObject.getString("room_name");
                            String text = jsonObject.getString("message_text");
                            String createdAt = jsonObject.getString("created_at");
                            int check = jsonObject.getInt("g_check");
                            if(check == 0) {
                                getMembers(roomId, roomName, text, createdAt, check);
                            }
                        }
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("ChatActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("ChatActivity", "Failed to retrieve room info");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("ChatActivity", "Failed to load rooms", t);
            }
        });

    }

    private void getMembers(int roomId, String roomName, String text, String createdAt, int check) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getMembers(roomId);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try{
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<Profile> groupList = new ArrayList<>();
                        groupList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            String name = jsonObject.getString("name");
                            String imageBase64 = jsonObject.optString("profile_img", null);
                            Bitmap img = null;
                            if (imageBase64 != null && !imageBase64.isEmpty() && !imageBase64.equals("null")) {
                                img = ImageUtils.base64ToBitmap(imageBase64);
                            } else {
                                img = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
                            }
                            groupList.add(new Profile(id, name, img));
                        }
                        if(check == 0) {
                            chatList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, false));
                        } else {
                            chatList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, true));
                        }
                        RoomManager.getInstance().setRoomList(chatList);
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("ChatActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("ChatActivity", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("ChatActivity", "Failed to load rooms", t);
            }
        });
    }

    public void uploadMedia(Uri fileUri) {
        // 권한 확인 및 요청
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }

        try {
            // ContentResolver를 사용하여 파일 이름과 MIME 타입 가져오기
            String fileName = getFileName(fileUri);
            String mimeType = getContentResolver().getType(fileUri);
            String[] type = mimeType.split("/");

            // InputStream으로 파일 읽기
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = readBytesFromInputStream(inputStream);

            // RequestBody 생성
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), fileBytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

            Gson gson = new Gson();

            // 서버 업로드 호출
            RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
            Call<RetrofitService.UploadResponse> call = retrofitService.uploadFile(body, RequestBody.create(MediaType.parse("text/plain"), "file description"), roomId, myId, type[0]);
            call.enqueue(new Callback<RetrofitService.UploadResponse>() {
                @Override
                public void onResponse(Call<RetrofitService.UploadResponse> call, Response<RetrofitService.UploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Message message = new Message(roomId, title, myId, myName, type[0], response.body().getFileUrl(), time);
                        String messageJson = gson.toJson(message);
                        messageList.add(message);
                        webSocket.send(messageJson);
                        messageAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        etMessage.setText("");

                        Log.d("Upload", "File uploaded successfully: " + response.body().getFileUrl());
                    } else {
                        Log.e("Upload", "Upload failed with response: " + response.message());
                    }
                }
                @Override
                public void onFailure(Call<RetrofitService.UploadResponse> call, Throwable t) {
                    Log.e("Upload", "Upload failed", t);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Upload", "Error uploading file", e);
        }
    }

    // 파일 이름 가져오기
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            // Fallback: 파일 경로에서 이름 추출
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // InputStream에서 바이트 배열 읽기
    private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
