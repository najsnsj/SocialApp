package com.example.socialapp.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Manager.ProfileManager;
import com.example.socialapp.R;
import com.example.socialapp.Entity.Story;
import com.example.socialapp.Adapter.StoryAdapter;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class StoryProfileActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    StoryAdapter storyAdapter;
    ImageButton imageButton;
    List<Story> storyList = new ArrayList<>();
    int position;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PROFILE_REQUEST_CODE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_profile_list);

        recyclerView = findViewById(R.id.rv_user_stories);
        imageButton = findViewById(R.id.ib_story_preview_back);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        int myId = sharedPreferences.getInt("user_id", -1);
        String myName = sharedPreferences.getString("myName", null);

        Intent intent = getIntent();
        int storyId = intent.getIntExtra("storyId", -1);

        storyAdapter = new StoryAdapter(ProfileManager.getInstance().getUserStoryList(), myId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(storyAdapter);

        for (int i = 0; i < ProfileManager.getInstance().getUserStoryList().size(); i++) {
            if (ProfileManager.getInstance().getUserStoryList().get(i).getId() == storyId) {
                position = i;
                break;
            }
        }

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                // 해당 인덱스로 스크롤
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(position, 0);  // 4번째 스토리로 스크롤
                }
            }
        });

        storyAdapter.notifyDataSetChanged();

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
