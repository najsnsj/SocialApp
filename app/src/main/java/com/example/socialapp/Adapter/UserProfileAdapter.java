package com.example.socialapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.R;
import com.example.socialapp.Entity.Story;

import java.util.ArrayList;
import java.util.List;

public class UserProfileAdapter extends RecyclerView.Adapter<UserProfileAdapter.UserProfileViewHolder>{
    private Context context;
    private OnItemClickListener onItemClickListener;
    List<Story> storiesList = new ArrayList<>();
    public UserProfileAdapter(List<Story> storiesList, Context context) {
        this.storiesList = storiesList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public UserProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.story_profile_item, parent, false);

        return new UserProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserProfileViewHolder holder, int position) {
        Story story = storiesList.get(position);
        if (story.getStory_imgs() != null && !story.getStory_imgs().isEmpty()) {
            holder.imageView.setImageBitmap(story.getStory_imgs().get(0));
        }
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int itemSize = screenWidth / 3;
        ViewGroup.LayoutParams layoutParams = holder.imageView.getLayoutParams();
        layoutParams.width = itemSize;
        layoutParams.height = itemSize;
        holder.imageView.setLayoutParams(layoutParams);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(story);
                }
            }
        });
    }
    @Override
    public int getItemCount() {
        return storiesList.size();
    }
    public static class UserProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public UserProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_story_preview);
        }
    }
    public interface  OnItemClickListener {
        void onItemClick(Story story);
    }
}
