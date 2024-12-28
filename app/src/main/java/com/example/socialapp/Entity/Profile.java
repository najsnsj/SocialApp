package com.example.socialapp.Entity;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Profile implements Parcelable {
    private int userId;
    private String userName;
    private String profileMessage;
    private Bitmap profileImage;

    public Profile(int userId, String userName, String profileMessage, Bitmap profileImage) {
        this.userId = userId;
        this.userName = userName;
        this.profileMessage = profileMessage;
        this.profileImage = profileImage;
    }

    public Profile(int userId, String userName, Bitmap profileImage) {
        this.userId = userId;
        this.userName = userName;
        this.profileImage = profileImage;
    }

    protected Profile(Parcel in) {
        userId = in.readInt();
        userName = in.readString();
        profileMessage = in.readString();
        profileImage = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<Profile> CREATOR = new Creator<Profile>() {
        @Override
        public Profile createFromParcel(Parcel in) {
            return new Profile(in);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(userId);
        dest.writeString(userName);
        dest.writeString(profileMessage);
        dest.writeParcelable(profileImage, flags);
    }

    public int getUserId() { return userId; }
    public  String getUserName() { return userName; }
    public String getProfileMessage() { return profileMessage; }
    public Bitmap getProfileImage() { return profileImage; }
}