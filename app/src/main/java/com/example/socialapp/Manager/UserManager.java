package com.example.socialapp.Manager;

import com.example.socialapp.Entity.User;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static UserManager instance;
    private List<User> userList = new ArrayList<>();
    private List<Integer> friendList = new ArrayList<>();
    private List<Integer> blockedList = new ArrayList<>();

    private UserManager() {
        userList.clear();
        friendList.clear();
        blockedList.clear();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public List<User> getUserList() {
        return userList;
    }
    public List<Integer> getFriendList() { return friendList; }
    public List<Integer> getBlockedList() { return blockedList; }
    public void setBlockedList(List<Integer> blockedList) { this.blockedList = blockedList; }
    public void addBlockedList(int id) { this.blockedList.add(id); }
    public void cancelBlockedList(int id) { int index = this.blockedList.indexOf(id);
        if (index != -1) {this.blockedList.remove(index);}
    }
    public void setFriendList(List<Integer> friendList) { this.friendList = friendList; }
    public void addFriendList(int id) { this.friendList.add(id); }
    public void setUserList(List<User> userList) {
        this.userList = userList;
    }
    public void addUserList(User user) { this.userList.add(user); }
    public void resetList() {
        userList.clear();
        friendList.clear();
        blockedList.clear();
    }
}


