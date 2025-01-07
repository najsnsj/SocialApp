package com.example.socialapp.Adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.socialapp.Entity.ChatRoom;
import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Manager.RoomManager;
import com.example.socialapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    private List<ChatRoom> chatRoomList;
    private int myId;
    private OnItemClickListener onItemClickListener;

    public ChatRoomAdapter(List<ChatRoom> chatRoomList, int myId) {
        this.chatRoomList = RoomManager.getInstance().getRoomList();
        sortChatRoomsByTimestamp();
        this.myId = myId;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chatting_room_item, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRoomList.get(position);
        if(chatRoom.getUsers().size() < 3) {        // 1대1 개인 채팅 시
            holder.imageView.setVisibility(View.VISIBLE);
            holder.llGroupProfile.setVisibility(View.GONE);
            String name = null;
            Bitmap image = null;

            for(Profile profile: chatRoom.getUsers()) {
                if(profile.getUserId() != myId) {
                    name = profile.getUserName();
                    image = profile.getProfileImage();
                }
            }
            if(chatRoom.getRoomName().isEmpty() || chatRoom.getRoomName().equals("") || chatRoom.getRoomName().equals(null) || chatRoom.getRoomName().equals("null")) {
                holder.roomName.setText(name);
                holder.lastMessage.setText(chatRoom.getLastMessage());
                holder.timestamp.setText(dateFormat(chatRoom.getTimestamp()));
                holder.imageView.setImageBitmap(image);
            }else {
                holder.roomName.setText(chatRoom.getRoomName());
                holder.lastMessage.setText(chatRoom.getLastMessage());
                holder.timestamp.setText(dateFormat(chatRoom.getTimestamp()));
                holder.imageView.setImageBitmap(image);
            }

        } else {        // 단체 채팅방
            StringBuilder nameBuilder = new StringBuilder();
            int count=0;
            holder.imageView.setVisibility(View.GONE);
            holder.llGroupProfile.setVisibility(View.VISIBLE);
            displayProfileImages(chatRoom.getUsers(),holder.llGroupProfile);
            for (Profile profile : chatRoom.getUsers()) {   // 단톡방 이름 생성(이름 4명까지 + 추가인원 수)
                if (count >= 4) {
                    nameBuilder.append(" +" + (chatRoom.getUsers().size() - 4));
                    break;
                }
                if (nameBuilder.length() > 1) { // 처음이 아니면 쉼표 추가
                    nameBuilder.append(", ");
                }
                nameBuilder.append(profile.getUserName());
                count++;
            }
            String name = nameBuilder.toString();
            if(chatRoom.getRoomName().isEmpty() || chatRoom.getRoomName().equals("") || chatRoom.getRoomName().equals(null) || chatRoom.getRoomName().equals("null")){
                holder.roomName.setText(name);
                holder.lastMessage.setText(chatRoom.getLastMessage());
                holder.timestamp.setText(dateFormat(chatRoom.getTimestamp()));
            }else {
                holder.roomName.setText(chatRoom.getRoomName());
                holder.lastMessage.setText(chatRoom.getLastMessage());
                holder.timestamp.setText(dateFormat(chatRoom.getTimestamp()));
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    if(chatRoom.getUsers().size() < 3) {
                        onItemClickListener.onItemClick(chatRoom, null);
                    } else {
                        onItemClickListener.onItemClick(chatRoom, holder.roomName.getText().toString());
                    }

                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatRoomList.size();
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        public TextView roomName;
        public TextView lastMessage;
        public TextView timestamp;
        public ImageView imageView;
        public LinearLayout llGroupProfile;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.tv_room_name);
            lastMessage = itemView.findViewById(R.id.tv_last_message);
            timestamp = itemView.findViewById(R.id.tv_timestamp);
            imageView = itemView.findViewById(R.id.iv_room_profile);
            llGroupProfile = itemView.findViewById(R.id.ll_group_image);
        }
    }

    private void displayProfileImages(List<Profile> users, LinearLayout profileContainer) {     // 채팅방 이미지 생성(인원 4명까지)
        profileContainer.removeAllViews(); // 기존의 View 제거

        int imagesPerRow = 2; // 한 행에 배치할 이미지 개수
        int maxProfiles = 4;  // 최대 4명 표시

        LinearLayout currentRow = null;
        int count = 0;

        for (Profile user : users) {
            if(user.getUserId() == myId) continue;
            if (count >= maxProfiles) break;

            if (count % imagesPerRow == 0) {
                currentRow = new LinearLayout(profileContainer.getContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                profileContainer.addView(currentRow); // 새로운 행 추가
            }

            ImageView imageView = new ImageView(profileContainer.getContext());
            Bitmap profileImage = user.getProfileImage();

            if (profileImage != null) {
                imageView.setImageBitmap(profileImage);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView.setImageResource(R.drawable.profile_img); // 기본 이미지 설정
            }

            LinearLayout.LayoutParams params;
            params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(0, 0, 0, 0);

            imageView.setLayoutParams(params);

            if (currentRow != null) {
                currentRow.addView(imageView); // 현재 행에 ImageView 추가
            }

            count++;
        }
    }

    private void sortChatRoomsByTimestamp() {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Collections.sort(this.chatRoomList, new Comparator<ChatRoom>() {
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

    private String dateFormat(String date) {

        Date current = new Date();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat dayFormat = new SimpleDateFormat("HH시 mm분");
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM월 dd일");
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy년 MM월 dd일");

        dayFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        monthFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        yearFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        try {
            Date parseDate = inputFormat.parse(date);
            long diffDate = current.getTime() - parseDate.getTime();
            diffDate/=1000;

            if(diffDate/(24 * 3600) == 0) {
                String formatDate = dayFormat.format(parseDate);
                return formatDate;
            }else if(diffDate/(24 * 3600) == 1) {
                return "어제";
            }else if(diffDate/(24 * 3600 * 365) == 0){
                String formatDate = monthFormat.format(parseDate);
                return formatDate;
            }else{
                String formatDate = yearFormat.format(parseDate);
                return formatDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface OnItemClickListener {
        void onItemClick(ChatRoom chatRoom, String title);
    }
}

