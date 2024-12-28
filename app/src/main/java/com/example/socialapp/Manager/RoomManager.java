package com.example.socialapp.Manager;

import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Entity.Profile;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {
    private static RoomManager instance;
    private List<ChatRoom> roomList = new ArrayList<>();
    private List<Profile> memberList = new ArrayList<>();

    private RoomManager() {
        roomList.clear();
        memberList.clear();
    }

    public static synchronized RoomManager getInstance() {
        if(instance == null) {
            instance = new RoomManager();
        }
        return instance;
    }
    public List<ChatRoom> getRoomList() { return roomList; }
    public void setRoomList(List<ChatRoom> roomList) { this.roomList = roomList; }
    public List<Profile> getMemberList() { return memberList; }
    public void removeMemberList(Profile profile) {
        for(int i=0;i<this.memberList.size();i++) {
            if(this.memberList.get(i).getUserId() == profile.getUserId()){
                this.memberList.remove(i);
            }
        }
    }
    public boolean checkMemberList(Profile profile) {
        boolean check = false;
        for(int i=0;i<this.memberList.size();i++) {
            if(this.memberList.get(i).getUserId() == profile.getUserId()){
                check =true;
            }
        }
        return check;
    }

    public void addMemberList(Profile profile) { this.memberList.add(profile); }
    public void resetMemberList() { this.memberList.clear(); }
    public void resetList() {
        this.roomList.clear();
        this.memberList.clear();
    }
}