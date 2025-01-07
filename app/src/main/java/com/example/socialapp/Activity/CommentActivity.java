package com.example.socialapp.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.Comment;
import com.example.socialapp.Adapter.CommentAdapter;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.Manager.AppStatus;
import com.example.socialapp.R;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class CommentActivity extends AppCompatActivity implements CommentAdapter.OnCommentClickListener {
    private RecyclerView recyclerView;
    private List<Comment> commentList = new ArrayList<>();
    private List<Comment> sendList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private WebSocket webSocket;
    private EditText chatText;
    private TextView tagText;
    private ImageButton btnDelete;
    private Button btnSend;
    private LinearLayout tagLayout;
    private int tagId = 0;
    private int storyId = 0;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.comment_list);

        recyclerView = findViewById(R.id.rv_comment);
        chatText = findViewById(R.id.et_comment_message);
        tagText = findViewById(R.id.tv_comment_tag);
        btnDelete = findViewById(R.id.ib_tag_delete);
        btnSend = findViewById(R.id.btn_comment_send);
        tagLayout = findViewById(R.id.ll_comment_tag);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        int myId = sharedPreferences.getInt("user_id", -1);
        String myName = sharedPreferences.getString("myName", null);

        Intent intent = getIntent();
        storyId = intent.getIntExtra("storyId", -1);

        commentList.clear();

        getComment(storyId);

        initWebSocket(storyId);

        commentAdapter = new CommentAdapter(this, commentList, myId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(commentAdapter);

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tagId = 0;
                chatText.setText("");
                tagText.setText("");
                tagLayout.setVisibility(View.GONE);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendComment(storyId,myId,tagId,myName);
                tagId = 0;
            }
        });
    }

    private void initWebSocket(int storyId){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(RetrofitInstance.getWebUrl()).build(); // WebSocket 서버 URL
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("CommentActivity", "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        int commentStoryId = jsonObject.getInt("story_id");
                        if(storyId == commentStoryId) {
                            getComment(storyId);
                            commentAdapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("CommentActivity", "WebSocket error", t);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.d("CommentActivity", "WebSocket closing: " + reason);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppStatus.isCommentActivityActive = true;
        AppStatus.getInstance().setStoryId(storyId);
    }

    @Override
    public void finish() {
        super.finish();
        AppStatus.isCommentActivityActive = false;
        AppStatus.getInstance().setStoryId(0);
        webSocket.close(1000, null);
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.close(1000, "Goodbye");
    }

    private void getComment(int storyId) {      // 댓글 조회
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getComments(storyId);
        call.enqueue(new retrofit2.Callback<ResponseBody>(){
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        commentList.clear();
                        for(int i=0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            int storyId = jsonObject.getInt("story_id");
                            int answerId = jsonObject.getInt("answer_id");
                            int userId = jsonObject.getInt("user_id");
                            int count = jsonObject.getInt("count") - 1;
                            String comment = jsonObject.getString("comment");
                            String created_at = jsonObject.getString("created_at");
                            String name = jsonObject.getString("name");
                            String profile = jsonObject.optString("profile_img", null);
                            Bitmap img = null;
                            if (profile != null && !profile.isEmpty() && !profile.equals("null")) {
                                img = ImageUtils.base64ToBitmap(profile);
                            } else {
                                img = BitmapFactory.decodeResource(getResources(), R.drawable.profile_img);
                            }
                            System.out.println("COUNT: "+count+" comment: "+comment);
                            if(listCheck(commentList, id, comment)) {   } else {
                                commentList.add(new Comment(id, storyId, answerId, userId, count, comment, created_at, name, img));
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    private void sendComment(int storyId, int myId, int tagId, String myName) {     // 댓글 보내기
        String comment_text = chatText.getText().toString().trim();
        if (!comment_text.isEmpty()) {
            Comment comment = new Comment(storyId, tagId, comment_text, myId, myName);

            Gson gson = new Gson();
            String commentJson = gson.toJson(comment);

            RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
            Call<Void> call = retrofitService.sendComment(comment); // roomId, senderId는 예시
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        if(webSocket != null) {
                            sendList.clear();
                            sendList.add(new Comment(storyId, tagId, comment_text, myId, myName));
                            webSocket.send(commentJson);
                        }
                    } else {
                        Log.e("CommentActivity", "Failed to send message");
                    }
                    getComment(storyId);
                    chatText.setText("");
                    tagText.setText("");
                    tagLayout.setVisibility(View.GONE);
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e("CommentActivity", "Failed to send message", t);
                }
            });
        }
    }

    public boolean listCheck(List<Comment> commentList, int index, String text) {
        for(int i = 0; i< commentList.size(); i++) {
            if(commentList.get(i).getId() == index && commentList.get(i).getComment().equals(text)) {
                return true;
            }
            if(commentList.get(i).getId() == index && !commentList.get(i).getComment().equals(text)) {
                commentList.remove(i);
            }
        }
        return false;
    }

    @Override
    public void onCommentClick(String text, int id) {
        tagText.setText(text+"님에게 남기는 답글");
        tagLayout.setVisibility(View.VISIBLE);
        tagId = id;
    }
}
