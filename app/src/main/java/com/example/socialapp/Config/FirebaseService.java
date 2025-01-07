package com.example.socialapp.Config;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.socialapp.Activity.ChatActivity;
import com.example.socialapp.Activity.CommentActivity;
import com.example.socialapp.Manager.AppStatus;
import com.example.socialapp.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("login_prefs", MODE_PRIVATE);
        int myId = sharedPreferences.getInt("user_id", -1);
        int roomId = -1;
        int storyId = -1;

        String room = remoteMessage.getData().get("room");
        String story = remoteMessage.getData().get("story");
        String name = remoteMessage.getData().get("name");
        String userId = remoteMessage.getData().get("userId");
        String title =  remoteMessage.getNotification().getTitle();
        String body =  remoteMessage.getNotification().getBody();

        if(room != null) {roomId = Integer.valueOf(room);}
        if(story != null) {storyId = Integer.valueOf(story);}

        if(AppStatus.isChatActivityActive && AppStatus.getInstance().getRoomId() == roomId) {
            return;
        }
        if(AppStatus.isCommentActivityActive && AppStatus.getInstance().getStoryId() == storyId) {
            return;
        }

        if(roomId == 0) {
            if(myId == Integer.valueOf(userId)) {
                return;
            }
            commentNotification(title, body, storyId);
        } else {
            showNotification(title, body, roomId, name);
        }
    }

    private void showNotification(String title, String body, int roomId, String name) {
        // NotificationManager를 사용하여 알림 표시
        String text = title;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "my_channel_id"; // 채널 ID 설정

        // Android Oreo 이상의 경우 채널을 설정해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        if(title.equals(name)) {
            text = null;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("room_id", roomId); // 전달할 데이터 (예: room_id)
        intent.putExtra("title", text);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 기존 Activity 스택 제거 후 새로 생성

        PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // PendingIntent 업데이트 플래그
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setSmallIcon(R.mipmap.ic_launcher_socialapp) // 알림 아이콘
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(0, builder.build());
    }

    private void commentNotification(String title, String body, int storyId){
        // NotificationManager를 사용하여 알림 표시
        String text = title;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "my_channel_id"; // 채널 ID 설정

        // Android Oreo 이상의 경우 채널을 설정해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, CommentActivity.class);
        intent.putExtra("storyId", storyId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 기존 Activity 스택 제거 후 새로 생성

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // PendingIntent 업데이트 플래그
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setSmallIcon(R.mipmap.ic_launcher_socialapp) // 알림 아이콘
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(0, builder.build());
    }
}


