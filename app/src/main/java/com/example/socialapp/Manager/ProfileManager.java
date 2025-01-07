package com.example.socialapp.Manager;

import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Entity.Story;

import java.util.ArrayList;
import java.util.List;

public class ProfileManager {
    private static ProfileManager instance;
    private List<Profile> userProfileList = new ArrayList<>();
    private List<Story> userStoryList = new ArrayList<>();
    int currentPage;

    private ProfileManager() {
        userProfileList.clear();
        userStoryList.clear();
        currentPage = 1;
    }

    public static synchronized ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    public List<Story> getUserStoryList() { return userStoryList; }
    public void setUserProfileList(List<Profile> userProfileList) { this.userProfileList = userProfileList; }
    public void addUserStoryList(List<Story> storyList) { this.userStoryList.addAll(storyList); }
    public int getPage() { return currentPage; }
    public void addPage() { this.currentPage++; }
    public void resetList() {
        userProfileList.clear();
        userStoryList.clear();
        this.currentPage = 1;
    }
    public void reviseStoryList(int storyId, String post) {
        for(int i = 0; i < this.userStoryList.size(); i++) {
            if(this.userStoryList.get(i).getId() == storyId) {
                this.userStoryList.get(i).setPost(post);
            }
        }
    }
    public void deleteStoryList(int storyId) {
        for(int i = 0; i < this.userStoryList.size(); i++) {
            if(this.userStoryList.get(i).getId() == storyId) {
                this.userStoryList.remove(i);
            }
        }
    }
}

