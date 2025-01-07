package com.example.socialapp.Manager;

public class AppStatus {
    private static AppStatus instance;
    public static boolean isChatActivityActive = false;
    public static boolean isCommentActivityActive = false;
    int isChatActivityRoom;
    int isCommentActivityStory;

    private AppStatus() {
        this.isCommentActivityStory = 0;
        this.isChatActivityRoom = 0;
    }

    public static synchronized AppStatus getInstance() {
        if (instance == null) {
            instance = new AppStatus();
        }
        return instance;
    }

    public int getRoomId() { return this.isChatActivityRoom; }
    public void setRoomId(int roomId) { this.isChatActivityRoom = roomId; }
    public int getStoryId() { return this.isCommentActivityStory; }
    public void setStoryId(int storyId) { this.isCommentActivityStory = storyId; }
}
