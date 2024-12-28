package com.example.socialapp.Activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.socialapp.Adapter.AddSliderAdapter;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Fragment.StoryFragment;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.R;
import com.example.socialapp.Entity.Story;
import com.example.socialapp.Adapter.StoryAdapter;
import com.example.socialapp.Manager.StoryManager;
import com.example.socialapp.Entity.User;
import com.example.socialapp.Manager.UserManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.WebSocket;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class AddStoryActivity extends AppCompatActivity {
    private int storyIndex = -1;
    private ViewPager2 viewPager;
    private EditText editText;
    private ImageButton imageButton;
    private Button button;
    private List<Bitmap> imageList = new ArrayList<>();
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.add_story_page);

        viewPager = findViewById(R.id.vp_add_images);
        editText = findViewById(R.id.et_add_post);
        button = findViewById(R.id.btn_share);
        imageButton = findViewById(R.id.ib_photo);

        Intent intent = getIntent();
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra("imageUris");
        int myId = intent.getIntExtra("id", -1);
        int storyId = intent.getIntExtra("storyId", -1);
        String storyPost = intent.getStringExtra("post");

        if(storyId != -1) {     // 스토리 수정할 때
            for (Story list : StoryManager.getInstance().getImageList()) {
                if (list.getId() == storyId) {
                    imageList.add(list.getStory_img());
                }
            }
            if (!imageList.isEmpty()) {
                imageButton.setVisibility(View.GONE);
                editText.setText(storyPost);
                AddSliderAdapter addSliderAdapter = new AddSliderAdapter(imageList);
                viewPager.setAdapter(addSliderAdapter);
            }
        } else {       // 스토리 추가할 때
            AddSliderAdapter addSliderAdapter = new AddSliderAdapter(imageUris, false);
            viewPager.setAdapter(addSliderAdapter);

            galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            imageUris.clear();
                            if(data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                for(int i = 0; i < count; i++) {
                                    Uri imgUri = data.getClipData().getItemAt(i).getUri();
                                    imageUris.add(imgUri);
                                }
                            } else if(data.getClipData() != null) {
                                Uri imgUri = data.getData();
                                imageUris.add(imgUri);
                            }
                            addSliderAdapter.notifyDataSetChanged();
                        }
                    });
        }

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(storyId != -1) {
                    if(storyPost.equals(editText.getText().toString().trim())) {
                       finish();
                    } else {
                        reviseStory(storyId, editText.getText().toString().trim());
                        finish();
                    }
                } else {
                    if (imageUris == null || imageUris.isEmpty()) {
                        Toast.makeText(AddStoryActivity.this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
                    } else {
                        makeStory(myId, imageUris);
                        setResult(Activity.RESULT_OK);
                    }
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
    }

    private void openGallery() {        // 갤러리 사진 가져오기(MULTI)
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(intent); // 갤러리 열기
    }

    private void makeStory(int myId , ArrayList<Uri> imageUris) {     // 스토리 추가
        String post = editText.getText().toString().trim();
        editText.setText("");
        List<User> userList = UserManager.getInstance().getUserList();

        for (User user : userList) {
            if (myId == user.getId()) {
                RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
                Story story = new Story(user.getId(), user.getName(), ImageUtils.bitmapToBase64(user.getProfile_img()), post);
                retrofit2.Call<RetrofitService.StoryResponse> call = retrofitService.makeStory(story);
                call.enqueue(new retrofit2.Callback<RetrofitService.StoryResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<RetrofitService.StoryResponse> call, retrofit2.Response<RetrofitService.StoryResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int storyId = response.body().getId(); // 생성된 ID 가져오기
                            addImages(storyId, imageUris); // 생성된 ID로 이미지 삽입
                        } else {
                            try {
                                Log.e("AddStoryActivity", "Failed story: " + response.errorBody().string());
                            } catch (IOException e) {
                                Log.e("AddStoryActivity", "Failed to read error body", e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<RetrofitService.StoryResponse> call, Throwable t) {
                        Log.e("AddStoryActivity", "Failed to story", t);
                    }
                });
            }
        }
    }

    private void addImages(int storyId , ArrayList<Uri> imageUris) {      // 추가할 스토리 이미지 추가
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        for(Uri uri : imageUris) {
            String image = ImageUtils.uriToBase64(this, uri);
            Story story = new Story(storyId, image);

            retrofit2.Call<Void> call = retrofitService.addImages(story);
            call.enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

                }
                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    Log.e("AddStoryActivity", "Failed to story", t);
                }
            });
        }
        finish();
    }

    private void reviseStory(int storyId, String post) {        // 스토리 수정
        Map<String, String> body = new HashMap<>();
        body.put("storyId", String.valueOf(storyId));
        body.put("post", post);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.putStory(body);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                StoryManager.getInstance().reviseStoryList(storyId, post);
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("AddStoryActivity", "Failed to story", t);
            }
        });
    }
}


