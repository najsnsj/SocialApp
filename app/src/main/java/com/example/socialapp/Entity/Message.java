package com.example.socialapp.Entity;

import java.util.Date;

public class Message {
    private int message_id;
    private int room_id;
    private int sender_id;
    private String name;
    private String title;
    private String type;
    private String message_text;
    private String created_at;

    public Message(int roomId, String title, int senderId, String name, String type, String text, String created_at)  {
        this.room_id = roomId;
        this.sender_id = senderId;
        this.title = title;
        this.name = name;
        this.type = type;
        this.message_text = text;
        this.created_at = created_at;
    }

    public int getRoomId() {
        return room_id;
    }
    public void setRoomId(int room_id) {
        this.room_id = room_id;
    }
    public String getType() { return type; }
    public int getSenderId() {
        return sender_id;
    }
    public String getMessageText() {
        return message_text;
    }
    public String getName() { return name; }
    public String getCreatedAt() {
        return created_at;
    }
}

