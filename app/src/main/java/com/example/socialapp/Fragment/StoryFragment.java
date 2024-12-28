package com.example.socialapp.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.socialapp.Activity.AddStoryActivity;
import com.example.socialapp.Activity.CommentActivity;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.R;
import com.example.socialapp.Entity.Story;
import com.example.socialapp.Adapter.StoryAdapter;
import com.example.socialapp.Manager.StoryManager;
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
import retrofit2.http.Query;

public class StoryFragment extends Fragment {

    private ActivityResultLauncher<Intent> launcher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StoryAdapter storyAdapter;
    private List<Story> sList = new ArrayList<>();
    private List<Story> storyList = new ArrayList<>();
    private List<Story> list = new ArrayList<>();
    private List<Story> imageList = new ArrayList<>();
    private List<Bitmap> bitmapList = new ArrayList<>();
    private List<Story> likeList = new ArrayList<>();
    private List<Integer> blockedList = UserManager.getInstance().getBlockedList();
    private List<Integer> friendList = UserManager.getInstance().getFriendList();
    private ImageButton imageButton;
    private int myId;
    private int storyId;
    private String myName;
    private boolean isLoading = false;
    private final int pageSize = 10;
    private static final int REQUEST_CODE_STORY_ACTIVITY = 1;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.story_list, container, false);

        recyclerView = view.findViewById(R.id.rv_story);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        imageButton = view.findViewById(R.id.btn_add_story);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        myId = sharedPreferences.getInt("user_id", -1);
        myName = sharedPreferences.getString("myName", null);

        if(getArguments() != null) {
            storyId = getArguments().getInt("storyId");
        }

        if(storyId != -1) {
            Intent intent = new Intent(getActivity(), CommentActivity.class);
            intent.putExtra("storyId", storyId);
            startActivity(intent);
        }

        if(StoryManager.getInstance().getStoryList().isEmpty() || StoryManager.getInstance().getImageList().isEmpty()) {
            getImages(myId);
        }

        storyAdapter = new StoryAdapter(filterStories(StoryManager.getInstance().getStoryList()), myId, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(storyAdapter);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null) {
                ArrayList<Uri> imageUris = new ArrayList<>(uris);
                openAddStoryActivity(imageUris);
            }
        });

        imageButton.setOnClickListener(v -> openGallery());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if(!isLoading) {
                StoryManager.getInstance().resetList();
                storyAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if(linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == StoryManager.getInstance().getStoryList().size() -1) {
                    getImages(myId);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_STORY_ACTIVITY && resultCode == Activity.RESULT_OK) {
            StoryManager.getInstance().resetList();
            getImages(myId);
        }
        storyAdapter.notifyDataSetChanged();
    }

    private void openGallery() {
        imagePickerLauncher.launch("image/*");
    }

    private void openAddStoryActivity(ArrayList<Uri> imageUris) {
        Intent intent = new Intent(getActivity(), AddStoryActivity.class);
        intent.putExtra("id", myId);
        intent.putParcelableArrayListExtra("imageUris", imageUris);
        startActivityForResult(intent, REQUEST_CODE_STORY_ACTIVITY);
    }

    private List<Story> filterStories(List<Story> list) {
        List<Story> storyList = new ArrayList<>();
        for(Story story : list) {
            if(story.getUserId() == myId) {
                storyList.add(story);
            }
            if(friendList.contains(story.getUserId()) && !blockedList.contains(story.getUserId())) {
                storyList.add(story);
            }
        }
        return storyList;
    }

    public void getImages(int userId) {
        if(isLoading) return;
        isLoading = true;

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getImages(userId, StoryManager.getInstance().getPage(), pageSize);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        imageList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            String image = jsonObject.optString("image", null);
                            Bitmap img = null;
                            if (image != null && !image.isEmpty() && !image.equals("null")) {
                                img = ImageUtils.base64ToBitmap(image);
                                imageList.add(new Story(id, img));
                            }
                        }
                        if(isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                StoryManager.getInstance().addImageList(imageList);
                                getStorys(userId, imageList);
                            });
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("StoryFragment", "Error parsing JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("StoryFragment", "Failed to load Images", t);
            }
        });
    }

    public void getStorys(int userId, List<Story> list) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getLikes(userId, StoryManager.getInstance().getPage(), pageSize);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
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

        call = retrofitService.getStories(userId, StoryManager.getInstance().getPage(), pageSize);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        sList.clear();
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
                                    bitmapList.add(story.getStory_img());
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
                                sList.add(new Story(id, userId, name, profileImg, copiedList, post, likes, check,created_at, updated_at));
                            }
                        }
                        if(isAdded() && getActivity() != null) {
                            if (!sList.isEmpty()) {
                                getActivity().runOnUiThread(() -> {
                                    StoryManager.getInstance().addStoryList(sList);
                                    storyAdapter = new StoryAdapter(filterStories(StoryManager.getInstance().getStoryList()), myId, getContext());
                                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                                    recyclerView.setAdapter(storyAdapter);
                                    storyAdapter.notifyDataSetChanged();
                                    StoryManager.getInstance().addPage();
                                });
                            }
                        }
                        swipeRefreshLayout.setRefreshing(false);
                        isLoading = false;
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("StoryFragment", "Error parsing JSON", e);
                    } finally {
                        isLoading = false;
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("StoryFragment", "Failed to load story", t);
            }
        });

    }
}
