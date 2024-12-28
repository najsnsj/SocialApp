package com.example.socialapp.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Entity.Profile;
import com.example.socialapp.R;
import com.example.socialapp.Manager.UserManager;

import java.util.ArrayList;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>{
    private List<Profile> profileList = new ArrayList<>();
    private List<Integer> blockedList = new ArrayList<>();
    private List<Integer> friends = UserManager.getInstance().getFriendList();
    private OnItemClickListener onItemClickListener;

    public ProfileAdapter(List<Profile> profileList) {
        this.profileList = profileList;
        this.blockedList = UserManager.getInstance().getBlockedList();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_item, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Profile profile = profileList.get(position);

        holder.userName.setText(profile.getUserName());
        if (profile.getProfileMessage() == null | profile.getProfileMessage().equals("") | profile.getProfileMessage().equals("null")) {
            holder.profileMessage.setText("");
        } else {
            holder.profileMessage.setText(profile.getProfileMessage());
        }
        holder.profileImage.setImageBitmap(profile.getProfileImage());

        if (blockedList.contains(profile.getUserId())) {
            holder.blockCondition.setVisibility(View.VISIBLE);
        } else {
            holder.blockCondition.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(profile);
                }
            }
        });
    }

    @Override
    public int getItemCount() { return profileList.size(); }

    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public TextView profileMessage;
        public ImageView profileImage;
        public ImageView blockCondition;
        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            profileMessage = itemView.findViewById(R.id.user_message);
            profileImage = itemView.findViewById(R.id.iv_profile);
            blockCondition = itemView.findViewById(R.id.iv_block_condition);
        }
    }
    public interface  OnItemClickListener {
        void onItemClick(Profile profile);
    }

}
