package com.example.socialapp.Entity;

import android.graphics.Bitmap;

public class User {
    private int id;
    private int friend_id;
    private String name;
    private String email;
    private String password;
    private String profile_message;
    private Bitmap profile_img;

    public User(int id, String name, String email, String password, String profile_message, Bitmap img) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.profile_message = profile_message;
        this.profile_img = img;
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User(int id, int friend_id){
        this.id = id;
        this.friend_id = friend_id;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProfile_message() { return profile_message; }
    public Bitmap getProfile_img() { return profile_img; }
}
