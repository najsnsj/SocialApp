package com.example.socialapp.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Activity.AddRoomActivity;
import com.example.socialapp.Activity.ChatActivity;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Adapter.ChatRoomAdapter;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.R;
import com.example.socialapp.Manager.RoomManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class RoomFragment extends Fragment {
    private WebSocket webSocket;
    private RecyclerView recyclerView;
    private ImageButton btnAddRoom;
    private ChatRoomAdapter chatRoomAdapter;
    private List<ChatRoom> roomList = new ArrayList<>();
    private List<ChatRoom> chatRoomList = new ArrayList<>();
    private int myId;
    private int roomId = -1;
    private String myName;
    private String title = null;
    private static final int REQUEST_CODE_CHAT_ACTIVITY = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.room_list, container, false);

        btnAddRoom = view.findViewById(R.id.btn_add_room);
        recyclerView = view.findViewById(R.id.rv_chat_room);

        Date date = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        if(getArguments() != null) {
            roomId = getArguments().getInt("roomId");
            title = getArguments().getString("title");
        }

        initWebSocket();

        chatRoomAdapter = new ChatRoomAdapter(RoomManager.getInstance().getRoomList(), myId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatRoomAdapter);

        if(roomId != -1) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("room_id", roomId);
            intent.putExtra("title", title);
            startActivityForResult(intent, REQUEST_CODE_CHAT_ACTIVITY);
        }

        btnAddRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddRoomActivity.class);
                startActivity(intent);
            }
        });

        chatRoomAdapter.setOnItemClickListener(new ChatRoomAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ChatRoom chatRoom, String title) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("room_id", chatRoom.getRoomId());
                intent.putExtra("title", title);
                startActivityForResult(intent, REQUEST_CODE_CHAT_ACTIVITY);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHAT_ACTIVITY && resultCode == Activity.RESULT_OK) {
            chatRoomAdapter.notifyDataSetChanged();
        }else {
            chatRoomAdapter.notifyDataSetChanged();
        }
    }

    private void initWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(RetrofitInstance.getWebUrl()).build(); // WebSocket 서버 URL
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("ChatActivity", "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
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
                                    //rooms.set(i, new ChatRoom(room.getRoomId(), room.getRoomName(), room.getUsers(), room.getName(), img,jsonObject.getString("message_text"), time, room.getCheck()));
                                    check = true;
                                    break;
                                }
                            }

                            if(!check) {    // 새로운 채팅방에서 연락 올 시 채팅방 조회
                                getGroupRooms(myId);
                            }

                            sortChatRoomsByTimestamp(rooms);
                            RoomManager.getInstance().setRoomList(rooms);
                            chatRoomAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    Log.e("RoomFragment", "Activity is null, cannot update UI.");
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("RoomFragment", "WebSocket error", t);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.d("RoomFragment", "WebSocket closing: " + reason);
            }
        });
    }

    private void sortChatRoomsByTimestamp(List<ChatRoom> list) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Collections.sort(list, new Comparator<ChatRoom>() {
            @Override
            public int compare(ChatRoom room1, ChatRoom room2) {
                try {
                    Date date1 = inputFormat.parse(room1.getTimestamp());
                    Date date2 = inputFormat.parse(room2.getTimestamp());
                    // 최신 메시지가 위로 오도록 정렬
                    return date2.compareTo(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
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
                            if("image".equals(jsonObject.getString("type"))) {
                                text = "이미지를 보냈습니다.";
                            } else if("video".equals(jsonObject.getString("type"))) {
                                text = "동영상을 보냈습니다.";
                            }
                            getMembers(roomId, roomName, text, createdAt, check);
                        }
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("RoomFragment", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("RoomFragment", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("RoomFragment", "Failed to load rooms", t);
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
                            if("image".equals(jsonObject.getString("type"))) {
                                text = "이미지를 보냈습니다.";
                            } else if("video".equals(jsonObject.getString("type"))) {
                                text = "동영상을 보냈습니다.";
                            }

                            if(check == 0) {
                                getMembers(roomId, roomName, text, createdAt, check);
                            }
                        }
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("RoomFragment", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("RoomFragment", "Failed to retrieve room info");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("RoomFragment", "Failed to load rooms", t);
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
                            if(isAdded() && getContext() != null) {
                                if (imageBase64 != null && !imageBase64.isEmpty() && !imageBase64.equals("null")) {
                                    img = ImageUtils.base64ToBitmap(imageBase64);
                                } else {
                                    img = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
                                }
                            }
                            groupList.add(new Profile(id, name, img));
                        }
                        /*if(check == 0) {
                            roomList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, false));
                        } else {
                            roomList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, true));
                        }
                        System.out.println("soty: "+text);
                        RoomManager.getInstance().setRoomList(roomList);*/
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if(check == 0) {
                                    RoomManager.getInstance().addRoomList(new ChatRoom(roomId, roomName, groupList, text, createdAt, false));
                                } else {
                                    RoomManager.getInstance().addRoomList(new ChatRoom(roomId, roomName, groupList, text, createdAt, true));
                                }
                                chatRoomAdapter.notifyDataSetChanged();
                            });
                        }
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("RoomFragment", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("RoomFragment", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("RoomFragment", "Failed to load rooms", t);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocket != null) {
            webSocket.close(1000, "Fragment destroyed");
        }
    }
}


