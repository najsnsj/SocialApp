package com.example.socialapp.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Fragment.ProfileFragment;
import com.example.socialapp.Fragment.RoomFragment;
import com.example.socialapp.Fragment.StoryFragment;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.R;
import com.example.socialapp.Manager.RoomManager;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class NavigationActivity extends AppCompatActivity {
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private long backPressedTime;
    private List<ChatRoom> roomList = new ArrayList<>();
    private WebSocket webSocket;
    private Toast exitToast;
    private String myName = null;
    private int myId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle bundle = new Bundle();
        Intent intent = getIntent();
        int roomId = intent.getIntExtra("roomId", -1);
        int storyId = intent.getIntExtra("storyId", -1);
        String title = intent.getStringExtra("title");

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        initWebSocket();

        NavigationBarView navigationBarView = findViewById(R.id.bottom_navigation);
        navigationBarView.setOnItemSelectedListener(item -> {       // 네이게이션 바 조절(프로필, 채팅방, 스토리)
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (id == R.id.nav_rooms) {
                bundle.putInt("roomId", roomId);
                bundle.putString("title", title);
                selectedFragment = new RoomFragment();
            } else if (id == R.id.nav_story) {
                bundle.putInt("storyId", storyId);
                selectedFragment = new StoryFragment();
            }

            if (selectedFragment != null) {
                selectedFragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .replace(R.id.container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // 기본 프래그먼트를 설정 (옵션)
        if (savedInstanceState == null) {
            if(roomId != -1) {
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new RoomFragment())
                        .commit();
                navigationBarView.setSelectedItemId(R.id.nav_rooms);
            } else if(storyId != -1) {
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new RoomFragment())
                        .commit();
                navigationBarView.setSelectedItemId(R.id.nav_story);
            } else {
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new RoomFragment())
                        .commit();
                navigationBarView.setSelectedItemId(R.id.nav_profile);
            }
        }
    }

    private void initWebSocket(){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(RetrofitInstance.getWebUrl()).build(); // WebSocket 서버 URL
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("NavigationActivity", "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        Date date = new Date();
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        String time = inputFormat.format(date);
                        boolean check = false;
                        List<ChatRoom> rooms = RoomManager.getInstance().getRoomList();
                        for (int i = 0; i < rooms.size(); i++) {
                            ChatRoom room = rooms.get(i);
                            if (room.getRoomId() == jsonObject.getInt("room_id")) {
                                String message = jsonObject.getString("message_text");
                                if("image".equals(jsonObject.getString("type"))) {
                                    message = "이미지를 보냈습니다.";
                                } else if("video".equals(jsonObject.getString("type"))) {
                                    message = "동영상을 보냈습니다.";
                                }
                                rooms.set(i, new ChatRoom(room.getRoomId(), room.getRoomName(), room.getUsers(), message, time, room.getCheck()));
                                check = true;
                                break;
                            }
                        }

                        if(!check) {
                            getGroupRooms(myId);
                        }
                        RoomManager.getInstance().setRoomList(rooms);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("NavigationActivity", "WebSocket error", t);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.d("NavigationActivity", "WebSocket closing: " + reason);
            }
        });
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
                        roomList.clear();
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
                        Log.e("NavigationActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("NavigationActivity", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("NavigationActivity", "Failed to load rooms", t);
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
                        Log.e("NavigationActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("NavigationActivity", "Failed to retrieve room info");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("NavigationActivity", "Failed to load rooms", t);
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
                        /*if(check == 0) {
                            roomList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, false));
                        } else {
                            roomList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, true));
                        }
                        RoomManager.getInstance().setRoomList(roomList);*/
                        runOnUiThread(() -> {
                            if (check == 0) {
                                RoomManager.getInstance().addRoomList(new ChatRoom(roomId, roomName, groupList, text, createdAt, false));
                            } else {
                                RoomManager.getInstance().addRoomList(new ChatRoom(roomId, roomName, groupList, text, createdAt, true));
                            }
                        });

                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("NavigationActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("NavigationActivity", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("NavigationActivity", "Failed to load rooms", t);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            webSocket.close(1000, "Goodbye");
            exitToast.cancel(); // 이전에 띄운 Toast를 취소
            super.onBackPressed(); // 앱 종료
            return;
        }

        exitToast = Toast.makeText(getApplicationContext(), "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT);
        exitToast.show();

        backPressedTime = System.currentTimeMillis(); // 현재 시간 저장
    }
}

