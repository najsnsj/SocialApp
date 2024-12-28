package com.example.socialapp.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Fragment.ProfileFragment;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Manager.ProfileManager;
import com.example.socialapp.R;
import com.example.socialapp.Entity.Story;
import com.example.socialapp.Entity.User;
import com.example.socialapp.Manager.UserManager;
import com.example.socialapp.Adapter.UserProfileAdapter;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class UserProfileActivity extends AppCompatActivity {

    private Button btnOk;
    private Button btnLogout;
    private Button btnMessage;
    private ImageButton ibImage;
    private ImageButton ibSetting;
    private ImageButton ibAdd;
    private ImageButton ibBlock;
    private ImageButton ibDeleteImg;
    private ImageView ibBlockFriend;
    private EditText etName;
    private EditText etMessage;
    private ImageView ivProfile;
    private WebSocket webSocket;
    private RecyclerView recyclerView;
    private UserProfileAdapter userProfileAdapter;
    private List<Story> imagesList = new ArrayList<>();
    private List<Bitmap> imageList = new ArrayList<>();
    private List<Bitmap> bitmapList = new ArrayList<>();
    private List<Story> storiesList = new ArrayList<>();;
    private List<Story> likeList = new ArrayList<>();
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PROFILE_REQUEST_CODE = 2;
    private String userName = null;
    private String userMessage = null;
    private Bitmap userImage = null;
    private String myName;
    private boolean condition = false;
    private boolean check = false;
    private boolean isBlocked = false;
    private final int pageSize = 10;

    private List<User> userList = new ArrayList<>();
    private List<Profile> profileList = new ArrayList<>();
    private List<ChatRoom> chatList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_page);

        btnOk = findViewById(R.id.btn_ok);
        btnLogout = findViewById(R.id.btn_logout);
        btnMessage = findViewById(R.id.btn_message);
        ibSetting = findViewById(R.id.ib_edit);
        ibImage = findViewById(R.id.ib_image);
        ibDeleteImg = findViewById(R.id.ib_delete_img);
        etName = findViewById(R.id.profile_name);
        etMessage = findViewById(R.id.profile_message);
        ivProfile = findViewById(R.id.iv_profile_image);
        ibAdd = findViewById(R.id.ib_add);
        ibBlock = findViewById(R.id.ib_block);
        ibBlockFriend = findViewById(R.id.ib_block_friend);
        recyclerView = findViewById(R.id.rv_preview_story);

        ScrollView scrollView = findViewById(R.id.scrollView);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        int myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        Intent intent = getIntent();
        int userId = intent.getIntExtra("userId", -1);
        condition = intent.getBooleanExtra("condition", false);

        imagesList.clear();
        ProfileManager.getInstance().resetList();


        if(ProfileManager.getInstance().getUserStoryList().isEmpty() || imagesList.isEmpty()) {
            getImage(userId, myId);
        }

        int screenHeight = ivProfile.getResources().getDisplayMetrics().heightPixels;
        int itemSize = screenHeight / 2;
        ViewGroup.LayoutParams layoutParams = ivProfile.getLayoutParams();
        layoutParams.height = itemSize;
        ivProfile.setLayoutParams(layoutParams);

        List<User> users = UserManager.getInstance().getUserList();
        List<Integer> friends = UserManager.getInstance().getFriendList();
        List<Integer> blockedList = UserManager.getInstance().getBlockedList();

        if(friends.contains(userId)){ condition = false; }
        boolean ok = false;
        for(User user : users) {
            if(user.getId() == userId){
                ok = true;
            }
        }
        if(!ok) {
            getOne(userId);
        }

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);   // 넓이 기준 3칸으로 배열
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });

        recyclerView.setLayoutManager(gridLayoutManager);

        userProfileAdapter = new UserProfileAdapter(imagesList, this);
        recyclerView.setAdapter(userProfileAdapter);

        userProfileAdapter.setOnItemClickListener(new UserProfileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Story story) {
                Intent storyIntent = new Intent(UserProfileActivity.this, StoryProfileActivity.class);
                storyIntent.putExtra("userId", userId);
                storyIntent.putExtra("storyId", story.getId());
                startActivity(storyIntent);
            }
        });

        for (User user : users) {
            if (userId == user.getId() && user.getId() == myId) {   // 내 프로필
                btnLogout.setVisibility(View.VISIBLE);
                ibSetting.setVisibility(View.VISIBLE);
                btnMessage.setVisibility(View.GONE);
                ibBlockFriend.setVisibility(View.GONE);
                ibAdd.setVisibility(View.GONE);
                ibBlock.setVisibility(View.GONE);

                etName.setText(user.getName());

                if (user.getProfile_img() != null) {
                    ivProfile.setImageBitmap(user.getProfile_img());
                }

                if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                    etMessage.setText("");
                } else {
                    etMessage.setText(user.getProfile_message());
                }
                userName = String.valueOf(user.getName());
                userMessage = String.valueOf(user.getProfile_message());
                if(ivProfile.getDrawable() != null) {
                    userImage = user.getProfile_img();
                } else {
                    userImage = null;
                }
            } else if (userId == user.getId() && !condition && !blockedList.contains(user.getId())) {     // 친구 프로필(차단x)
                btnMessage.setVisibility(View.VISIBLE);
                ibBlockFriend.setVisibility(View.VISIBLE);
                ibAdd.setVisibility(View.GONE);
                ibBlock.setVisibility(View.GONE);
                etName.setText(user.getName());

                if (user.getProfile_img() != null) {
                    ivProfile.setImageBitmap(user.getProfile_img());
                }

                if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                    etMessage.setText("");
                } else {
                    etMessage.setText(user.getProfile_message());
                }
                userName = user.getName();
                userMessage = user.getProfile_message();
                userImage = user.getProfile_img();
            } else if(userId == user.getId() && condition && !blockedList.contains(user.getId())) {    // 날 친추한 프로필(차단x)
                ibBlockFriend.setVisibility(View.GONE);
                ibAdd.setVisibility(View.VISIBLE);
                ibBlock.setVisibility(View.VISIBLE);

                if(blockedList.contains(user.getId())) {
                    ibAdd.setVisibility(View.GONE);
                    ibBlock.setVisibility(View.GONE);
                    ibBlockFriend.setImageDrawable(ContextCompat.getDrawable(ibBlockFriend.getContext(), R.drawable.add));
                    recyclerView.setVisibility(View.GONE);
                    isBlocked = true;
                }
                etName.setText(user.getName());
                if (user.getProfile_img() != null) {
                    ivProfile.setImageBitmap(user.getProfile_img());
                }
                if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                    etMessage.setText("");
                } else {
                    etMessage.setText(user.getProfile_message());
                }

                userName = user.getName();
                userMessage = user.getProfile_message();
                userImage = user.getProfile_img();
            } else if(userId == user.getId() && !condition && blockedList.contains(user.getId())) {     // 친구 프로필(차단ㅇ)
                ibBlockFriend.setVisibility(View.VISIBLE);
                ibBlockFriend.setImageDrawable(ContextCompat.getDrawable(ibBlockFriend.getContext(), R.drawable.add));
                ibAdd.setVisibility(View.GONE);
                ibBlock.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                btnMessage.setVisibility(View.GONE);
                isBlocked = true;

                etName.setText(user.getName());
                if (user.getProfile_img() != null) {
                    ivProfile.setImageBitmap(user.getProfile_img());
                }
                if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                    etMessage.setText("");
                } else {
                    etMessage.setText(user.getProfile_message());
                }

                userName = user.getName();
                userMessage = user.getProfile_message();
                userImage = user.getProfile_img();
            } else if(userId == user.getId() && condition && blockedList.contains(user.getId())) {      // 날 친추한 프로필(차단ㅇ)
                ibBlockFriend.setVisibility(View.VISIBLE);
                ibBlockFriend.setImageDrawable(ContextCompat.getDrawable(ibBlockFriend.getContext(), R.drawable.add));
                ibAdd.setVisibility(View.GONE);
                ibBlock.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                btnMessage.setVisibility(View.GONE);
                isBlocked = true;

                etName.setText(user.getName());
                if (user.getProfile_img() != null) {
                    ivProfile.setImageBitmap(user.getProfile_img());
                }
                if (user.getProfile_message() == null | user.getProfile_message().equals("") | user.getProfile_message().equals("null")) {
                    etMessage.setText("");
                } else {
                    etMessage.setText(user.getProfile_message());
                }

                userName = user.getName();
                userMessage = user.getProfile_message();
                userImage = user.getProfile_img();
            }
        }

        ibSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!check) {
                    btnLogout.setVisibility(View.GONE);
                    btnOk.setVisibility(View.VISIBLE);
                    ibImage.setVisibility(View.VISIBLE);
                    ibDeleteImg.setVisibility(View.VISIBLE);
                    etMessage.setHint("상태 메시지를 입력해주세요");
                    etMessage.setFocusable(true);
                    etMessage.setClickable(true);
                    etMessage.setFocusableInTouchMode(true);
                    etName.setHint("닉네임을 입력해주세요");
                    etName.setFocusable(true);
                    etName.setClickable(true);
                    etName.setFocusableInTouchMode(true);
                    check = true;
                } else {
                    btnOk.setVisibility(View.GONE);
                    btnLogout.setVisibility(View.VISIBLE);
                    ibImage.setVisibility(View.GONE);
                    ibDeleteImg.setVisibility(View.GONE);
                    etMessage.setHint("");
                    etMessage.setFocusable(false);
                    etMessage.setClickable(false);
                    etMessage.setFocusableInTouchMode(false);
                    etName.setHint("");
                    etName.setFocusable(false);
                    etName.setClickable(false);
                    etName.setFocusableInTouchMode(false);
                    check = false;
                }
            }
        });

        ibImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        ibBlockFriend.setOnClickListener(new View.OnClickListener() {       // 차단 버튼
            @Override
            public void onClick(View v) {
                if(!isBlocked) {
                    ibBlockFriend.setImageDrawable(ContextCompat.getDrawable(ibBlockFriend.getContext(), R.drawable.add));
                    if(condition) {
                        ibAdd.setVisibility(View.GONE);
                        ibBlock.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        btnMessage.setVisibility(View.GONE);
                    }
                    blockedList.add(userId);
                    UserManager.getInstance().addBlockedList(userId);
                    addBlockFriend(myId, userId);
                    isBlocked = true;
                } else {
                    ibBlockFriend.setImageDrawable(ContextCompat.getDrawable(ibBlockFriend.getContext(), R.drawable.block));
                    if(condition) {
                        ibBlockFriend.setVisibility(View.GONE);
                        ibAdd.setVisibility(View.VISIBLE);
                        ibBlock.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        btnMessage.setVisibility(View.VISIBLE);
                    }

                    blockedList.remove(blockedList.indexOf(userId));
                    UserManager.getInstance().cancelBlockedList(userId);
                    deleteBlockFriend(myId, userId);
                    isBlocked = false;
                }
            }
        });

        ibDeleteImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivProfile.setImageDrawable(null);
                userProfileAdapter.notifyDataSetChanged();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(etName.getText().toString().trim().equals("")) {
                    Toast.makeText(UserProfileActivity.this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    btnOk.setVisibility(View.GONE);
                    btnLogout.setVisibility(View.VISIBLE);
                    ibImage.setVisibility(View.GONE);
                    ibDeleteImg.setVisibility(View.GONE);
                    etMessage.setHint(null);
                    etMessage.setFocusable(false);
                    etMessage.setClickable(false);
                    etMessage.setFocusableInTouchMode(false);
                    etName.setFocusable(false);
                    etName.setClickable(false);
                    etName.setFocusableInTouchMode(false);
                    check(myId,String.valueOf(etName.getText()),String.valueOf(etMessage.getText()));
                    setResult(Activity.RESULT_OK);
                }
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("user_id");
                editor.apply();
                getFCMToken();
                Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        btnMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRoom(myId,userId);
            }
        });

        ibAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(condition == true) {
                    System.out.println("true: "+userId);
                    putMember(myId, userId);
                    ibAdd.setVisibility(View.GONE);
                    ibBlock.setVisibility(View.GONE);
                    btnMessage.setVisibility(View.VISIBLE);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("friendId", userId);
                    setResult(Activity.RESULT_OK, resultIntent);

                }else {
                    System.out.println("false: "+userId);
                    addUser(myId, userId);
                    setResult(Activity.RESULT_OK);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getUsers(myId);
                            friends.add(userId);
                            ibAdd.setVisibility(View.GONE);
                            ibBlock.setVisibility(View.GONE);
                            btnMessage.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });

        ibBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isBlocked) {
                    ibBlockFriend.setImageDrawable(ContextCompat.getDrawable(ibBlockFriend.getContext(), R.drawable.add));
                    ibBlockFriend.setVisibility(View.VISIBLE);
                    if(condition) {
                        ibAdd.setVisibility(View.GONE);
                        ibBlock.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        btnMessage.setVisibility(View.GONE);
                    }
                    blockedList.add(userId);
                    UserManager.getInstance().addBlockedList(userId);
                    addBlockFriend(myId, userId);
                    isBlocked = true;
                }
            }
        });

        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                View view = (View) scrollView.getChildAt(scrollView.getChildCount() - 1);

                int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

                // diff가 0이면 스크롤이 맨 아래에 도달한 상태
                if (diff <= 0) {
                    getImage(userId, myId);  // 마지막에 도달 시 이미지 로드
                    System.out.println("Loading more images...");
                }
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ivProfile.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("UserProfileActivity", "Error loading image", e);
            }
        }

        if (requestCode == PROFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // ProfileFragment 업데이트 호출
            FragmentManager fragmentManager = getSupportFragmentManager();
            ProfileFragment profileFragment = (ProfileFragment) fragmentManager.findFragmentById(R.id.container);
            if (profileFragment != null) {
                profileFragment.updateProfileData();
            }
        }
    }

    private void updateUser(int myId, String name, String message, Bitmap image) {      // 프로필 변경
        String base64Image = null;
        if (image != null) { base64Image = ImageUtils.bitmapToBase64(image); }

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.putUsers(myId, name, message, base64Image);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                getUsers(myId);
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to put profile", t);
            }
        });
    }

    private void getUsers(int userId) {
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
                        Log.e("UserProfileActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to load users", t);
            }
        });
    }

    private void getOne(int userId) {       // 유저한명의 정보 로드
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getUser(userId);
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

                            ibBlockFriend.setVisibility(View.GONE);
                            ibAdd.setVisibility(View.VISIBLE);
                            ibBlock.setVisibility(View.VISIBLE);

                            etName.setText(name);
                            if (img != null) {
                                ivProfile.setImageBitmap(img);
                            }
                            if (message == null | message.equals("") | message.equals("null")) {
                                etMessage.setText("");
                            } else {
                                etMessage.setText(message);
                            }

                            userName = name;
                            userMessage = message;
                            userImage = img;
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("UserProfileActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to load users", t);
            }
        });
    }

    private void check(int id,String name, String message) {
        Drawable drawable = ivProfile.getDrawable();
        Bitmap image = null;

        if(ivProfile.getDrawable() != null) { image = ((BitmapDrawable) drawable).getBitmap(); }

        if(name.equals(userName) && message.equals(userMessage) && userImage == image) {    }
        else {
            updateUser(id,name, message, image);
        }
    }

    private void searchRoom(int myId, int userId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getRoomId(myId);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                int getMyId = -1;
                int roomId = -1;
                int friendId = -1;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        chatList.clear();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            getMyId = jsonObject.getInt("user_id");
                            roomId = jsonObject.getInt("room_id");
                            friendId = jsonObject.getInt("friend_id");

                            chatList.add(new ChatRoom(roomId, getMyId, friendId));
                        }

                        for (ChatRoom chatRoom : chatList) {
                            getMyId = chatRoom.getMyId();
                            roomId = chatRoom.getRoomId();
                            friendId = chatRoom.getFriendId();

                            if (myId == getMyId && userId == friendId || myId == friendId && userId == getMyId) {
                                Intent intent = new Intent(UserProfileActivity.this, ChatActivity.class);
                                intent.putExtra("room_id", roomId);
                                startActivity(intent);
                                finish();
                                break;
                            }
                        }
                    }  catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("UserProfileActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("UserProfileActivity", "Failed to retrieve room info");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to send message", t);
            }
        });
    }

    private void putMember(int myId, int userId) {;
        Map<String, Integer> body = new HashMap<>();
        body.put("myId", myId);
        body.put("friendId", userId);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.addMember(body);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if(response.isSuccessful()){

                } else {
                    Log.e("UserProfileActivity", "Failed to add member");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to add member");
            }
        });
    }

    private void addUser(int myId, int friendId) {
        User friend = new User(myId, friendId);

        Gson gson = new Gson();
        String friendJson = gson.toJson(friend);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.makeFriend(friend);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    if(webSocket != null) {
                        webSocket.send(friendJson);
                    }
                } else {
                    Log.e("UserProfileActivity", "Failed to user");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to send user", t);
            }
        });
    }

    private void getImage(int id, int myId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getImage(id,ProfileManager.getInstance().getPage(), pageSize);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        int n = 0;
                        //imagesList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            String image = jsonObject.optString("image", null);
                            Bitmap img = null;
                            if (i != 0 && n != id) {
                                imagesList.add(new Story(n, new ArrayList<>(imageList)));
                                imageList.clear();
                            }

                            if (image != null && !image.isEmpty() && !image.equals("null")) {
                                img = ImageUtils.base64ToBitmap(image);
                                imageList.add(img);
                            }

                            if (i == jsonArray.length() - 1) {
                                imagesList.add(new Story(id, new ArrayList<>(imageList)));
                                imageList.clear();
                            }
                            n = id;
                        }
                        //ProfileManager.getInstance().setUserStoryList(imagesList);
                        getStory(id, myId, imagesList);
                        userProfileAdapter.notifyDataSetChanged();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("UserProfileActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to load Images", t);
            }
        });
    }

    public void getStory(int userId, int myId, List<Story> list) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call1 = retrofitService.getLike(userId, myId, ProfileManager.getInstance().getPage(), pageSize);
        call1.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        likeList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int storyId = jsonObject.getInt("story_id");
                            int count = jsonObject.getInt("num");
                            int n = jsonObject.getInt("me");
                            boolean check = false;
                            if(n == 1) { check = true; }
                            likeList.add(new Story(storyId, count, check));
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {

                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {

            }
        });

        retrofit2.Call<ResponseBody> call2 = retrofitService.getStory(userId, ProfileManager.getInstance().getPage(), pageSize);
        call2.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        storiesList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            bitmapList.clear();
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            int userId = jsonObject.getInt("user_id");
                            int likes = 0;
                            boolean check = false;
                            String name = jsonObject.getString("name");
                            String post = jsonObject.getString("post");
                            String created_at = jsonObject.getString("created_at");
                            String updated_at = jsonObject.getString("updated_at");
                            String profile = jsonObject.optString("profile_img", null);
                            Bitmap profileImg = null;
                            if (profile != null && !profile.isEmpty() && !profile.equals("null")) {
                                profileImg = ImageUtils.base64ToBitmap(profile);
                            }

                            for(Story story : list) {
                                if(story.getId() == id){
                                    bitmapList.addAll(story.getStory_imgs());
                                }
                            }

                            for (Story story : likeList) {
                                if(story.getId() == id) {
                                    likes = story.getLikes();
                                    check = story.getCheck();
                                }
                            }

                            if(!bitmapList.isEmpty()) {
                                List<Bitmap> copiedList = new ArrayList<>(bitmapList);
                                storiesList.add(new Story(id, userId, name, profileImg, copiedList, post, likes, check,created_at, updated_at));
                            }
                        }

                        if (!storiesList.isEmpty()) {
                            runOnUiThread(() -> {
                                ProfileManager.getInstance().addUserStoryList(storiesList);
                                ProfileManager.getInstance().addPage();
                            });
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("UserProfileActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to load story", t);
            }
        });

    }

    private void addBlockFriend(int myId, int friendId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.addBlock(friendId, myId);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {

                } else {
                    Log.e("UserProfileActivity", "Failed to send message");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to send message", t);
            }
        });
    }

    private void deleteBlockFriend(int myId, int friendId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.deleteBlock(friendId, myId);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {

                } else {
                    Log.e("UserProfileActivity", "Failed to send message");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to send message", t);
            }
        });
    }

    private void deleteToken(String token) {
        Map<String , String> body = new HashMap<>();
        body.put("token", token);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.deleteToken(body);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("UserProfileActivity", "Failed to send message", t);
            }
        });
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    String token = task.getResult();
                    deleteToken(token);
                });

    }
}
