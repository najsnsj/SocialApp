package com.example.socialapp.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Entity.Profile;
import com.example.socialapp.Manager.RoomManager;
import com.example.socialapp.R;

import java.util.List;

public class AddRoomAdapter extends RecyclerView.Adapter<AddRoomAdapter.AddRoomViewHolder> {
    private List<Profile> profileList;
    private OnItemClickListener onItemClickListener;

    public AddRoomAdapter(List<Profile> profileList) {
        this.profileList = profileList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public AddRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.add_room_item, parent, false);
        return new AddRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddRoomViewHolder holder, int position) {
        Profile profile = profileList.get(position);
        holder.userName.setText(profile.getUserName());
        if (profile.getProfileMessage() == null | profile.getProfileMessage().equals("") | profile.getProfileMessage().equals("null")) {
            holder.profileMessage.setText("");
        } else {
            holder.profileMessage.setText(profile.getProfileMessage());
        }
        holder.profileImage.setImageBitmap(profile.getProfileImage());

        if(!RoomManager.getInstance().checkMemberList(profile)) {
            holder.btnCheck.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.none_check));
        } else {
            holder.btnCheck.setBackground(holder.itemView.getContext().getDrawable(R.drawable.yes_check));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    if(!RoomManager.getInstance().checkMemberList(profile)) {
                        holder.btnCheck.setBackground(holder.itemView.getContext().getDrawable(R.drawable.yes_check));
                        onItemClickListener.onItemChecked(profile);
                    } else {
                        holder.btnCheck.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.none_check));
                        onItemClickListener.offItemChecked(profile);
                    }
                }
            }
        });
    }
    @Override
    public int getItemCount() { return profileList.size(); }

    public static class AddRoomViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public TextView profileMessage;
        public ImageView profileImage;
        public ImageView btnCheck;
        public RecyclerView rvAdded;

        public AddRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_addroom_name);
            profileMessage = itemView.findViewById(R.id.user_addroom_message);
            profileImage = itemView.findViewById(R.id.iv_addroom_profile);
            btnCheck = itemView.findViewById(R.id.iv_addroom_check);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Profile profile);
        void onItemChecked(Profile profile);
        void offItemChecked(Profile profile);
    }
}
