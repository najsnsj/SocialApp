package com.example.socialapp.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Adapter.ProfileAdapter;
import com.example.socialapp.R;
import com.example.socialapp.Entity.User;
import com.example.socialapp.Manager.UserManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class AddFriendActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_FRIEND_UPDATE = 1;
    private ProfileAdapter profileAdapter;
    private RecyclerView recyclerView;
    private List<Profile> profileList = new ArrayList<>();
    private List<User> userList = new ArrayList<>();
    private int myId;
    private String myName;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_friend_list);

        recyclerView = findViewById(R.id.rv_my_friend);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        getUsers(myId);

        Bitmap bitmapImage = BitmapFactory.decodeResource(getResources(),R.drawable.profile_img);
        List<User> users = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        List<Integer> blockedFriends = UserManager.getInstance().getBlockedList();

        for (User user : users) {
            if ((!friends.contains(user.getId()) && user.getId() != myId) || blockedFriends.contains(user.getId())) {
                if (user.getProfile_img() == null) {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), bitmapImage));
                } else {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                }
            }
        }

        profileAdapter = new ProfileAdapter(profileList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(profileAdapter);

        profileAdapter.setOnItemClickListener(new ProfileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Profile profile) {
                Intent intent = new Intent(AddFriendActivity.this, UserProfileActivity.class);
                intent.putExtra("userId",profile.getUserId());
                intent.putExtra("condition", true);
                startActivityForResult(intent, REQUEST_CODE_FRIEND_UPDATE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FRIEND_UPDATE && resultCode == RESULT_OK) {
            if (data != null) {
                int addId = data.getIntExtra("friendId", -1);
                updateList(addId);
                profileAdapter.notifyDataSetChanged();
            }
        } else {
            updateList(-1);
            profileAdapter.notifyDataSetChanged();
        }
    }

    private void updateList(int id) {       // 친구목록 초기화
        Bitmap bitmapImage = BitmapFactory.decodeResource(getResources(),R.drawable.profile_img);
        List<User> users = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        List<Integer> blockedFriends = UserManager.getInstance().getBlockedList();

        if(id != -1) {
            UserManager.getInstance().addFriendList(id);
        }

        profileList.clear();
        for (User user : users) {
            if ((!friends.contains(user.getId()) && user.getId() != myId) || blockedFriends.contains(user.getId())) {
                if (user.getProfile_img() == null) {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), bitmapImage));
                } else {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                }
            }
        }
        setResult(Activity.RESULT_OK);
    }

    private void getUsers(int userId) {       // 친구목록 조회
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getUsers(userId);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        userList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            String name = jsonObject.getString("name");
                            String email = jsonObject.getString("email");
                            String password = jsonObject.getString("password");
                            String message = jsonObject.optString("message", null);
                            String imageBase64 = jsonObject.optString("profile_img", null);
                            Bitmap img = null;
                            if (imageBase64 != null && !imageBase64.isEmpty() && !imageBase64.equals("null")) {
                                img = ImageUtils.base64ToBitmap(imageBase64);
                            }
                            userList.add(new User(id, name, email, password, message, img));
                        }
                        UserManager.getInstance().setUserList(userList);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("AddFirendActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("AddFirendActivity", "Failed to load messages", t);
            }
        });
    }
}
