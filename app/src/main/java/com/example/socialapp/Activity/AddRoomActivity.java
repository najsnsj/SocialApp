package com.example.socialapp.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Adapter.AddRoomAdapter;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Adapter.GuestAdapter;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Manager.RoomManager;
import com.example.socialapp.R;
import com.example.socialapp.Entity.User;
import com.example.socialapp.Manager.UserManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class AddRoomActivity extends AppCompatActivity {
    private int myId;
    private String myName;
    private List<Profile> profileList = new ArrayList<>();
    private EditText searchBar;
    private EditText etTitle;
    private Button btnMake;
    private ImageButton btnReset;
    private RecyclerView rvFriends;
    private RecyclerView rvAdded;
    private AddRoomAdapter addRoomAdapter;
    private GuestAdapter guestAdapter;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.add_room_list);

        etTitle = findViewById(R.id.et_room_title);
        searchBar = findViewById(R.id.et_room_searchbar);
        btnMake = findViewById(R.id.btn_make_room);
        btnReset = findViewById(R.id.ib_room_reset);
        rvFriends = findViewById(R.id.rv_room_friends);
        rvAdded = findViewById(R.id.rv_added_friends);

        Bitmap bitmapImage = BitmapFactory.decodeResource(getResources(),R.drawable.profile_img);
        List<User> users = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        List<Integer> blockedFriends = UserManager.getInstance().getBlockedList();

        RoomManager.getInstance().resetMemberList();

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        for(User user: users) {
            if(friends.contains(user.getId()) && !blockedFriends.contains(user.getId())) {
                if (user.getProfile_img() == null) {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), bitmapImage));
                } else {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                }
            }
        }

        addRoomAdapter = new AddRoomAdapter(profileList);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(addRoomAdapter);

        guestAdapter = new GuestAdapter(RoomManager.getInstance().getMemberList());
        rvAdded.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAdded.setAdapter(guestAdapter);

        btnMake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(RoomManager.getInstance().getMemberList().size() > 1) {
                    String text = etTitle.getText().toString();
                    addUser(myId, text);
                } else {
                    Toast.makeText(AddRoomActivity.this, "최소 3명 이상 추가해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                performSearch(query);
                addRoomAdapter.notifyDataSetChanged();
                guestAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
                addRoomAdapter.notifyDataSetChanged();
                guestAdapter.notifyDataSetChanged();
            }
        });

        addRoomAdapter.setOnItemClickListener(new AddRoomAdapter.OnItemClickListener() {        // 채팅방 인원 확인(프로필)
            @Override
            public void onItemClick(Profile profile) {
                addRoomAdapter.notifyDataSetChanged();
                guestAdapter.notifyDataSetChanged();
            }
            @Override
            public void onItemChecked(Profile profile) {
                if (!RoomManager.getInstance().getMemberList().contains(profile.getUserId())) {
                    RoomManager.getInstance().addMemberList(profile);
                    rvAdded.setVisibility(View.VISIBLE);
                }
                addRoomAdapter.notifyDataSetChanged();
                guestAdapter.notifyDataSetChanged();
            }
            @Override
            public void offItemChecked(Profile profile) {
                RoomManager.getInstance().removeMemberList(profile);
                if(RoomManager.getInstance().getMemberList().size()==0) {
                    rvAdded.setVisibility(View.GONE);
                }
                addRoomAdapter.notifyDataSetChanged();
                guestAdapter.notifyDataSetChanged();
            }
        });
    }

    private void performSearch(String query) {
        profileList.clear();
        Bitmap bitmapImage = BitmapFactory.decodeResource(getResources(),R.drawable.profile_img);
        List<User> users = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        List<Integer> blockedFriends = UserManager.getInstance().getBlockedList();
        for(User user: users) {
            if(friends.contains(user.getId()) && !blockedFriends.contains(user.getId()) && user.getName().contains(query)) {
                if (user.getProfile_img() == null) {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), bitmapImage));
                } else {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                }
            }
        }
        guestAdapter.notifyDataSetChanged();
        addRoomAdapter.notifyDataSetChanged();
    }

    private void addUser(int myId, String title) {      // 새롭게 채팅방 제작
        Map<String, Integer> body = new HashMap<>();
        body.put("user_id", myId);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        Call<ResponseBody> call = retrofitService.makeGroup(body, title);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        int roomId = jsonObject.getInt("id");

                        addGroupUsers(roomId, RoomManager.getInstance().getMemberList());
                    } catch (IOException | JSONException e) {
                        Log.e("AddRoomActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("AddRoomActivity", "Failed to make Room");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("AddRoomActivity", "Failed to make Room", t);
            }
        });
    }

    private void addGroupUsers(int roomId, List<Profile> userList) {        // 생성된 채팅방 유저 추가
        Map<String, Integer> body = new HashMap<>();
        body.put("roomId", roomId);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        for(Profile user : userList) {
            body.put("userId", user.getUserId());
            Call<Void> call = retrofitService.addGroups(body);
            call.enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

                }
                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    Log.e("AddRoomActivity", "Failed to add User", t);
                }
            });
        }
        Intent intent = new Intent(AddRoomActivity.this, ChatActivity.class);
        intent.putExtra("room_id", roomId);
        startActivity(intent);
        finish();
    }
}
