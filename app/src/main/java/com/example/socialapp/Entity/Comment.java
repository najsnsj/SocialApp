package com.example.socialapp.Entity;

import android.graphics.Bitmap;

public class Comment {
    private int id;
    private int story_id;
    private int answer_id;
    private int user_id;
    private int count;
    private String comment;
    private String created_at;
    private String name;
    private String answerName;
    private Bitmap profile_img;
    private boolean check;

    public Comment(int id, int story_id, int answer_id, int user_id, int count, String comment, String created_at, String name, Bitmap profile_img) {
        this.id = id;
        this.story_id = story_id;
        this.answer_id = answer_id;
        this.user_id = user_id;
        this.count = count;
        this.comment = comment;
        this.created_at = created_at;
        this.name = name;
        this.profile_img = profile_img;
        this.check = false;
    }

    public Comment(int id, int story_id, int answer_id, String answerName, int user_id, String comment, String created_at, String name, Bitmap profile_img) {
        this.id = id;
        this.story_id = story_id;
        this.answer_id = answer_id;
        this.answerName = answerName;
        this.user_id = user_id;
        this.comment = comment;
        this.created_at = created_at;
        this.name = name;
        this.profile_img = profile_img;
    }

    public Comment(int story_id,int answer_id,String comment,int user_id, String name) {
        this.story_id = story_id;
        this.answer_id = answer_id;
        this.comment = comment;
        this.user_id = user_id;
        this.name = name;
    }

    public int getId() { return id; }
    public int getStoryId() { return story_id; }
    public int getAnswerId() { return answer_id; }
    public int getUser_id() { return user_id; }
    public int getCount() { return count; }
    public String getComment() { return comment; }
    public String getCreated_at() { return created_at; }
    public String getName() { return name; }
    public String getAnswerName() { return answerName; }
    public Bitmap getProfile_img() { return profile_img; }
    public boolean getCheck() { return check; }
    public void setCheck(boolean ok) { this.check = ok; }
}
