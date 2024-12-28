package com.example.socialapp.Manager;

public class AppStatus {
    private static AppStatus instance;
    public static boolean isChatActivityActive = false;
    int isChatActivityRoom;

    private AppStatus() {
        isChatActivityRoom = 0;
    }

    public static synchronized AppStatus getInstance() {
        if (instance == null) {
            instance = new AppStatus();
        }
        return instance;
    }

    public int getRoomId() { return isChatActivityRoom; }
    public void setRoomId(int roomId) { this.isChatActivityRoom = roomId; }
}
