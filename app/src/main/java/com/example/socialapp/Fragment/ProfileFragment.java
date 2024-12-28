package com.example.socialapp.Fragment;

import static android.app.Activity.RESULT_OK;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Activity.AddFriendActivity;
import com.example.socialapp.Activity.LoginActivity;
import com.example.socialapp.Activity.NavigationActivity;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Adapter.ProfileAdapter;
import com.example.socialapp.Manager.ProfileManager;
import com.example.socialapp.R;
import com.example.socialapp.Entity.User;
import com.example.socialapp.Manager.UserManager;
import com.example.socialapp.Activity.UserProfileActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_CODE_PROFILE_UPDATE = 1;
    private TextView tvName;
    private TextView tvMessage;
    private TextView tvFriend;
    private ImageView ivProfile;
    private LinearLayout llProfile;
    private RecyclerView recyclerView;
    private ImageButton imageButton;
    private SearchView searchView;
    private ProfileAdapter profileAdapter;
    private List<User> userList = new ArrayList<>();
    private List<Profile> profileList = new ArrayList<>();
    private List<Integer> friends = UserManager.getInstance().getFriendList();
    private List<Integer> blockedFriends = UserManager.getInstance().getBlockedList();
    private List<User> users = UserManager.getInstance().getUserList();
    private int myId;
    private int userId;
    private String myName;
    private boolean check = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_list, container, false);

        recyclerView = view.findViewById(R.id.rv_profile);
        imageButton = view.findViewById(R.id.ib_search);
        searchView = view.findViewById(R.id.sv_search);
        tvName = view.findViewById(R.id.tv_my_name);
        tvMessage = view.findViewById(R.id.tv_my_message);
        tvFriend = view.findViewById(R.id.tv_my_friend);
        ivProfile = view.findViewById(R.id.iv_my_profile);
        llProfile = view.findViewById(R.id.ll_my_profile);

        Bitmap bitmapImage = BitmapFactory.decodeResource(getResources(),R.drawable.profile_img);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        for(User user: users) {
            if(user.getId() == myId) {
                if (user.getProfile_img() == null) {
                    ivProfile.setImageBitmap(bitmapImage);
                    tvName.setText(user.getName());
                    if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                        tvMessage.setText("");
                    } else {
                        tvMessage.setText(user.getProfile_message());
                    }
                    userId = user.getId();
                } else {
                    ivProfile.setImageBitmap(user.getProfile_img());
                    tvName.setText(user.getName());
                    if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                        tvMessage.setText("");
                    } else {
                        tvMessage.setText(user.getProfile_message());
                    }
                    userId = user.getId();
                }
            } else if(friends.contains(user.getId()) && !blockedFriends.contains(user.getId())) {
                if (user.getProfile_img() == null) {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), bitmapImage));
                } else {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                }
            }
        }

        profileAdapter = new ProfileAdapter(profileList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(profileAdapter);
        profileAdapter.notifyDataSetChanged();

        llProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserProfileActivity.class);
                intent.putExtra("userId",userId);
                startActivityForResult(intent,REQUEST_CODE_PROFILE_UPDATE);
            }
        });

        profileAdapter.setOnItemClickListener(new ProfileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Profile profile) {
                Intent intent = new Intent(getActivity(), UserProfileActivity.class);
                intent.putExtra("userId",profile.getUserId());
                intent.putExtra("condition", check);
                startActivityForResult(intent,REQUEST_CODE_PROFILE_UPDATE);
            }
        });

        tvFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchfriends(myId);
                Intent intent = new Intent(getActivity(), AddFriendActivity.class);
                startActivityForResult(intent,REQUEST_CODE_PROFILE_UPDATE);
            }
        });

        imageButton.setOnClickListener(v -> {
            String query = searchView.getQuery().toString();
            searchView.setQuery("",true);
            searchProfile(query);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PROFILE_UPDATE && resultCode == RESULT_OK) {
            // 데이터 갱신
            updateProfileData();
            profileAdapter.notifyDataSetChanged();
        }
        updateProfileData();
        profileAdapter.notifyDataSetChanged();
    }

    public void updateProfileData() {
        List<User> userList = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        List<Integer> blockedFriends = UserManager.getInstance().getBlockedList();
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
        profileList.clear();

        for(User user: userList) {
            if(user.getId() == myId) {
                if (user.getProfile_img() == null) {
                    ivProfile.setImageBitmap(defaultBitmap);
                    tvName.setText(user.getName());
                    if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                        tvMessage.setText("");
                    } else {
                        tvMessage.setText(user.getProfile_message());
                    }
                    userId = user.getId();
                } else {
                    ivProfile.setImageBitmap(user.getProfile_img());
                    tvName.setText(user.getName());
                    if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                        tvMessage.setText("");
                    } else {
                        tvMessage.setText(user.getProfile_message());
                    }
                    userId = user.getId();
                }
            } else if(friends.contains(user.getId()) && !blockedFriends.contains(user.getId())) {
                if (user.getProfile_img() == null) {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), defaultBitmap));
                } else {
                    profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                }
            }
        }

        profileAdapter.notifyDataSetChanged();
    }

    private void searchfriends(int id) {
        List<Integer> friendList = new ArrayList<>();
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getFriends(id);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int userid = jsonObject.getInt("friend_id");
                            if(id != userid) {
                                friendList.add(userid);
                            }
                        }
                        UserManager.getInstance().setFriendList(friendList);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("ProfileFragment", "Error parsing JSON", e);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("ProfileFragment", "Failed to load users", t);
            }
        });
    }

    private void searchProfile(String query) {
        List<User> userList = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        profileList.clear(); // 기존 데이터 초기화
        boolean ok = false;
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);

        if(query.equals("")){
            for (User user : userList) {
                if (user.getId() == myId) {
                    if (user.getProfile_img() == null) {
                        ivProfile.setImageBitmap(defaultBitmap);
                        tvName.setText(user.getName());
                        if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                            tvMessage.setText("");
                        } else {
                            tvMessage.setText(user.getProfile_message());
                        }
                    } else {
                        ivProfile.setImageBitmap(user.getProfile_img());
                        tvName.setText(user.getName());
                        if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                            tvMessage.setText("");
                        } else {
                            tvMessage.setText(user.getProfile_message());
                        }
                    }
                } else if (friends.contains(user.getId())) {
                    if (user.getProfile_img() == null) {
                        profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), defaultBitmap));
                    } else {
                        profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                    }
                }
            }
            ok = true;
        } else {
            for (User user : userList) {
                if (query.equals(user.getName()) || query.equals(user.getEmail())) {
                    if (user.getProfile_img() == null) {
                        profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), defaultBitmap));
                    } else {
                        profileList.add(new Profile(user.getId(), user.getName(), user.getProfile_message(), user.getProfile_img()));
                    }
                    if(!friends.contains(user.getId())) {
                        check = true;
                    }
                    ok = true;
                }
            }
        }
        if(!ok) {
            check = false;
            searchUser(query);
        }

        profileAdapter.notifyDataSetChanged();
    }

    private void searchUser(String query) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getEmail(query);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            String name = jsonObject.getString("name");
                            String message = jsonObject.optString("message", null);
                            String imageBase64 = jsonObject.optString("profile_img", null);
                            Bitmap img = null;

                            if (imageBase64 != null && !imageBase64.isEmpty() && !imageBase64.equals("null")) {
                                img = ImageUtils.base64ToBitmap(imageBase64);
                            } else {
                                img = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
                            }
                            // 유저 데이터 처리
                            profileList.add(new Profile(id, name, message, img));
                            ProfileManager.getInstance().setUserProfileList(profileList);
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                profileAdapter.notifyDataSetChanged();
                            }
                        });
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("ProfileFragment", "Error parsing JSON", e);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("ProfileFragment", "Failed to load users", t);
            }
        });
    }

}

