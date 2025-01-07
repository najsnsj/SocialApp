package com.example.socialapp.Manager;

import com.example.socialapp.Entity.Story;

import java.util.ArrayList;
import java.util.List;

public class StoryManager {
    private static  StoryManager instance;
    private List<Story> storyList = new ArrayList<>();
    private List<Story> filterList = new ArrayList<>();
    private List<Story> imageList = new ArrayList<>();
    int currentPage;

    private StoryManager() {
        storyList.clear();
        imageList.clear();
        filterList.clear();
        currentPage = 1;

    }

    public static synchronized StoryManager getInstance() {
        if(instance == null) {
            instance = new StoryManager();
        }
        return instance;
    }

    public List<Story> getStoryList() { return storyList; }
    public List<Story> getFilterList() { return filterList; }
    public List<Story> getImageList() { return imageList; }
    public void addFilterList(List<Story> storyList) { this.filterList.addAll(storyList); }
    public void addStoryList(List<Story> storyList) { this.storyList.addAll(storyList); }
    public void addImageList(List<Story> imageList) { this.imageList.addAll(imageList); }
    public int getPage() { return currentPage; }
    public void addPage() { this.currentPage++; }
    public void resetList() {
        this.imageList.clear();
        this.storyList.clear();
        this.filterList.clear();
        this.currentPage = 1;
    }
    public void reviseStoryList(int storyId, String post) {
        for(int i = 0; i < this.storyList.size(); i++) {
            if(this.storyList.get(i).getId() == storyId) {
                this.storyList.get(i).setPost(post);
            }
        }
        for(int i = 0; i < this.filterList.size(); i++) {
            if(this.filterList.get(i).getId() == storyId) {
                this.filterList.get(i).setPost(post);
            }
        }
    }
    public void deleteStoryList(int storyId) {
        for(int i = 0; i < this.storyList.size(); i++) {
            if(this.storyList.get(i).getId() == storyId) {
                this.storyList.remove(i);
            }
        }
        for(int i = 0; i < this.filterList.size(); i++) {
            if(this.filterList.get(i).getId() == storyId) {
                this.filterList.remove(i);
            }
        }
    }
 }
