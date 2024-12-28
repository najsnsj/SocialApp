package com.example.socialapp.Entity;

import android.graphics.Bitmap;

import java.util.List;

public class ChatRoom {
    private String room_name;
    private String message_text;
    private String name;
    private String created_at;
    private int room_id;
    private int user_id;
    private int freind_id;
    private boolean check;
    private List<Profile> users;

    public ChatRoom(int room_id, String room_name, List<Profile> users, String message_text, String created_at, boolean check) {
        this.room_id = room_id;
        this.room_name = room_name;
        this.users = users;
        this.message_text = message_text;
        this.created_at = created_at;
        this.check = check;
    }

    public ChatRoom(int room_id, int user_id, int freind_id) {
        this.room_id = room_id;
        this.user_id = user_id;
        this.freind_id = freind_id;
    }

    public int getFriendId() {return freind_id;}
    public int getRoomId() { return room_id; }
    public int getMyId() { return user_id; }
    public String getRoomName() {
        return room_name;
    }
    public String getName() { return name; }
    public String getLastMessage() {
        return message_text;
    }
    public String getTimestamp() {
        return created_at;
    }
    public boolean getCheck() { return check; }
    public List<Profile> getUsers() { return users; }

}

