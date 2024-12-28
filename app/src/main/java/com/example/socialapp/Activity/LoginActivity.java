package com.example.socialapp.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.socialapp.Config.NaverService;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Manager.ProfileManager;
import com.example.socialapp.Manager.StoryManager;
import com.example.socialapp.R;
import com.example.socialapp.Manager.RoomManager;
import com.example.socialapp.Entity.User;
import com.example.socialapp.Manager.UserManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;
import com.navercorp.nid.oauth.view.NidOAuthLoginButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnSignUp;
    private TextView tvJoin;
    private TextView tvPass;
    private NidOAuthLoginButton naverLogin;
    private List<User> userList = new ArrayList<>();
    private List<ChatRoom> roomList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private ExecutorService executorService;
    private long backPressedTime;
    private Toast exitToast;
    String myName = null;
    int chatId = -1;
    int storyId = -1;
    String title = null;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        if(getIntent() != null && getIntent().hasExtra("room")) {       // 메시지 알림 클릭 시 채팅방 이동
            chatId = Integer.valueOf(getIntent().getStringExtra("room"));
            title = getIntent().getStringExtra("title");
        }

        if (getIntent() != null && getIntent().hasExtra("story")) {     // 댓글 알림 클릭 시 스토리 이동
            chatId = -1;
            storyId = Integer.valueOf(getIntent().getStringExtra("story"));
        }

        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int id = sharedPreferences.getInt("user_id", -1);

        if(id != -1) {      // 재 로그인 유지
            getUsers(id);
            return;
        }

        setContentView(R.layout.login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignUp = findViewById(R.id.btn_signup);
        tvJoin = findViewById(R.id.tv_join);
        tvPass = findViewById(R.id.tv_find_password);
        naverLogin = findViewById(R.id.btn_naver);

        RoomManager.getInstance().resetList();
        ProfileManager.getInstance().resetList();
        StoryManager.getInstance().resetList();
        UserManager.getInstance().resetList();

        // 네이버 로그인 시 사용할 키
        NaverIdLoginSDK.INSTANCE.initialize(this, NaverService.clientId, NaverService.clientSecret, "socialApp");


        executorService = Executors.newSingleThreadExecutor();

        // 회원가입 버튼 클릭 리스너 설정
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });

        tvJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                startActivity(intent);
            }
        });

        tvPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, PassActivity.class);
                startActivity(intent);
            }
        });

        naverLogin.setOAuthLogin(new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();
                executorService.execute(() -> {
                    String responseBody = getNaver(accessToken);
                    if(responseBody != null && !responseBody.isEmpty()) {
                        runOnUiThread(() -> getProfile(responseBody));
                    }
                });
            }

            @Override
            public void onFailure(int i, @NonNull String s) {

            }

            @Override
            public void onError(int i, @NonNull String s) {

            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null && intent.hasExtra("room")) {
        }
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            exitToast.cancel(); // 이전에 띄운 Toast를 취소
            super.onBackPressed(); // 앱 종료
            return;
        }
        exitToast = Toast.makeText(getApplicationContext(), "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT);
        exitToast.show();
        backPressedTime = System.currentTimeMillis(); // 현재 시간 저장
    }


    private String getNaver(String accessToken) {       // 네이버 데이터 조회
        String apiURL = "https://openapi.naver.com/v1/nid/me";
        String authorization = "Bearer "+accessToken;

        HttpURLConnection con = connect(apiURL);
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", authorization);

            int responseCode = con.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {
                return readBody(con.getInputStream());
            } else {
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API Error", e);
        } finally {
            con.disconnect();
        }
    }

    private HttpURLConnection connect(String apiURL) {
        try {
            URL url = new URL(apiURL);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL Error"+apiURL, e);
        } catch (IOException e) {
            throw new RuntimeException("Connect Error"+apiURL, e);
        }
    }

    private String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);
        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }
            return  responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("Api Response Error",e);
        }
    }

    private void getProfile(String responseBody) {      // 네이버에서 받아 온 데이터 추출
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONObject response = jsonObject.getJSONObject("response");
            String id = response.getString("id");
            String name = response.getString("name");
            String email = response.getString("email");
            login(email, id, name, "naver");
        } catch (JSONException e) {
            throw new RuntimeException("JSON Error", e);
        }
    }

    private void getFCMToken(int myId) {        // FireBase 토큰 조회(알림 시 사용) *기기 별 토큰(아이디 별 X)*
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // 새 FCM 등록 토큰 가져오기
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // 서버에 토큰 저장하는 코드 작성
                    saveTokenToServer(myId,token);
                });
    }

    private void saveTokenToServer(int myId, String token) {        // 사용할 토큰과 유저 아이디 저장
        Map<String , String> body = new HashMap<>();
        body.put("userId", String.valueOf(myId));
        body.put("token", token);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.sendToken(body);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("LoginActivity", "Failed to send Token", t);
            }
        });
    }

    private void signUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        etEmail.setText("");
        etPassword.setText("");

        // 입력값 검증
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }
        login(email, password,"", "");     // 일반 로그인
    }

    private void joinUser(String name, String email, String password) {     // 회원 가입 후 로그인(네이버 로그인 시)
        User user = new User(name, email, password);
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.joinUser(user);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                login(email, password, "", "");
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {

            }
        });

    }

    private void login(String email, String password, String name, String type) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getEmail(email);
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
                            String getEmail = jsonObject.getString("email");
                            String getPassword = jsonObject.getString("password");
                            if(email.equals(getEmail) && password.equals(getPassword)){
                                SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("user_id", id);
                                editor.apply();
                                getFCMToken(id);
                                getUsers(id);
                                return;
                            }
                        }
                        if(type.equals("naver")) {
                            joinUser(name, email, password);
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load users", t);
            }
        });
    }

    private void searchfriends(int id) {     // 내 친구 리스트 조회(나를 추가한 사람 X)
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
                            int roomId = jsonObject.getInt("room_id");
                            if(id != userid) {
                                friendList.add(userid);
                            }
                        }
                        UserManager.getInstance().setFriendList(friendList);
                        Intent intent = new Intent(LoginActivity.this, NavigationActivity.class);
                        intent.putExtra("storyId", storyId);
                        intent.putExtra("roomId", chatId);
                        intent.putExtra("title", title);
                        startActivity(intent);
                        finish();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load users", t);
            }
        });
    }

    private void blockedfriends(int userId) {        // 내가 차단한 리스트 조회
        List<Integer> blockedList = new ArrayList<>();
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getBlockedFriends(userId);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        blockedList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int userId = jsonObject.getInt("user_id");
                            int blockedId = jsonObject.getInt("blocked_id");
                            blockedList.add(blockedId);
                        }
                        UserManager.getInstance().setBlockedList(blockedList);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                }

            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load users", t);
            }
        });
        searchfriends(userId);
    }

    private void getUsers(int userId) {       // 모든 친구 리스트 조회(친구, 나를 추가한 친구, 차단 친구)
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
                            } else {
                                img = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
                            }

                            if(userId == id) {
                                myName = name;
                                SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("myName", myName);
                                editor.apply();
                            }

                            userList.add(new User(id, name, email, password, message, img));
                        }
                        UserManager.getInstance().setUserList(userList);
                        blockedfriends(userId);
                        getGroupRooms(userId);
                        getRooms(userId);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load users", t);
            }
        });
    }

    public void getGroupRooms(int userId) {      // 그룹 채팅방 조회
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
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("ChatActivity", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load rooms", t);
            }
        });
    }

    public void getRooms(int userId) {     // 일반 채팅방 조회
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

                            if(check == 0) {        // 그룹 채팅방이 아닐 때
                                getMembers(roomId, roomName, text, createdAt, check);
                            }
                        }
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load rooms", t);
            }
        });
    }

    private void getMembers(int roomId, String roomName, String text, String createdAt, int check) {        // 채팅방 인원 조회
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
                            roomList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, false));
                        } else {
                            roomList.add(new ChatRoom(roomId, roomName, groupList, text, createdAt, true));
                        }
                        RoomManager.getInstance().setRoomList(roomList);
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("LoginActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("LoginActivity", "Failed to retrieve room info");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Failed to load rooms", t);
            }
        });
    }
}

