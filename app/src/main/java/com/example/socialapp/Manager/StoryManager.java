package com.example.socialapp.Manager;

import com.example.socialapp.Entity.Story;

import java.util.ArrayList;
import java.util.List;

public class StoryManager {
    private static  StoryManager instance;
    private List<Story> storyList = new ArrayList<>();
    private List<Story> imageList = new ArrayList<>();
    int currentPage;

    private StoryManager() {
        storyList.clear();
        imageList.clear();
        currentPage = 1;

    }

    public static synchronized StoryManager getInstance() {
        if(instance == null) {
            instance = new StoryManager();
        }
        return instance;
    }

    public List<Story> getStoryList() { return storyList; }
    public List<Story> getImageList() { return imageList; }
    public void addStoryList(List<Story> storyList) { this.storyList.addAll(storyList); }
    public void addImageList(List<Story> imageList) { this.imageList.addAll(imageList); }
    public int getPage() { return currentPage; }
    public void addPage() { this.currentPage++; }
    public void resetList() {
        this.imageList.clear();
        this.storyList.clear();
        this.currentPage = 1;
    }
    public void reviseStoryList(int storyId, String post) {
        for(int i = 0; i < this.storyList.size(); i++) {
            if(this.storyList.get(i).getId() == storyId) {
                this.storyList.get(i).setPost(post);
            }
        }
    }
 }
