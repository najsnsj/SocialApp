package com.example.socialapp.Entity;

import android.graphics.Bitmap;

import java.util.List;

public class Story {
    private int id;
    private int user_id;
    private int likes;
    private String name;
    private String image;
    private Bitmap profile_img;
    private Bitmap story_img;
    private String post;
    private String created_at;
    private String updated_at;
    private boolean check;
    private List<Bitmap> story_imgs;

    public Story(int id, int user_id, String name, Bitmap profile_img, List<Bitmap> story_imgs, String post, int likes, boolean check, String created_at, String updated_at) {
        this.id = id;
        this.user_id = user_id;
        this.name = name;
        this.profile_img = profile_img;
        this.story_imgs = story_imgs;
        this.post = post;
        this.likes = likes;
        this.check = check;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public Story(int user_id, String name, String image, String post){
        this.user_id = user_id;
        this.name = name;
        this.image = image;
        this.post = post;
    }
    public Story(int id, Bitmap story_img) {
        this.id = id;
        this.story_img = story_img;
    }

    public Story(int id, List<Bitmap> story_imgs) {
        this.id = id;
        this.story_imgs = story_imgs;
    }

    public Story(int id, String image) {
        this.id = id;
        this.image = image;
    }

    public Story(int id, int likes, boolean check) {
        this.id = id;
        this.likes = likes;
        this.check = check;
    }

    public Story(int id, int user_id) {
        this.id = id;
        this.user_id = user_id;
    }

    public void setPost(String post) { this.post = post; }
    public void setCheck(boolean check) { this.check = check; }
    public void setLikes(boolean check) { if(check){ this.likes++; } else { this.likes--; } }
    public int getId() { return id; }
    public int getUserId() { return user_id; }
    public int getLikes() { return likes; }
    public String getName() { return name; }
    public Bitmap getProfile_img() { return profile_img; }
    public Bitmap getStory_img() { return story_img; }
    public List<Bitmap> getStory_imgs() { return story_imgs; }
    public String getPost() { return post; }
    public String getCreated_at() { return created_at; }
    public boolean getCheck() { return check; }
}
