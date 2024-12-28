package com.example.socialapp.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Entity.Profile;
import com.example.socialapp.R;

import java.util.List;

public class GuestAdapter extends RecyclerView.Adapter<GuestAdapter.GuestViewHolder> {

    private List<Profile> profileList;
    private OnItemClickListener onItemClickListener;

    public GuestAdapter(List<Profile> profileList) { this.profileList = profileList; }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public GuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.add_room_added_item, parent, false);
        return new GuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuestViewHolder holder, int position) {
        Profile profile = profileList.get(position);
        holder.addedName.setText(profile.getUserName());
        holder.addedProfile.setImageBitmap(profile.getProfileImage());

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

    public static class GuestViewHolder extends RecyclerView.ViewHolder {
        public ImageButton addedProfile;
        public TextView addedName;
        public GuestViewHolder(@NonNull View itemView) {
            super(itemView);
            addedProfile = itemView.findViewById(R.id.ib_added_profile);
            addedName = itemView.findViewById(R.id.tv_added_name);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Profile profile);
    }
}
