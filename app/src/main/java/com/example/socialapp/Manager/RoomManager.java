package com.example.socialapp.Manager;

import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Entity.Profile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
    public void setRoomList(List<ChatRoom> roomList) {
        this.roomList = roomList;
        sortChatRoomsByTimestamp(this.roomList);
    }
    public void addRoomList(ChatRoom list) {
        boolean check = false;
        for(ChatRoom chatRoom : this.roomList) {
            if(chatRoom.getRoomId() == list.getRoomId()) {
                check = true;
            }
        }
        if(!check) {
            this.roomList.add(list);
        }
        sortChatRoomsByTimestamp(this.roomList);
    }
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

    private void sortChatRoomsByTimestamp(List<ChatRoom> list) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Collections.sort(list, new Comparator<ChatRoom>() {
            @Override
            public int compare(ChatRoom room1, ChatRoom room2) {
                try {
                    Date date1 = inputFormat.parse(room1.getTimestamp());
                    Date date2 = inputFormat.parse(room2.getTimestamp());
                    // 최신 메시지가 위로 오도록 정렬
                    return date2.compareTo(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
    }
}