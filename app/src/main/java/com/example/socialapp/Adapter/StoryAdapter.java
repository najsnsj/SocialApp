package com.example.socialapp.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.socialapp.Activity.CommentActivity;
import com.example.socialapp.Activity.UserProfileActivity;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Manager.ProfileManager;
import com.example.socialapp.Manager.StoryManager;
import com.example.socialapp.R;
import com.example.socialapp.Entity.Story;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {
    private Context context;
    private List<Story> storyList;
    private int myId;
    private OnItemClickListener onItemClickListener;


    public StoryAdapter(List<Story> storyList, int myId, Context context, OnItemClickListener onItemClickListener) {
        this.storyList = storyList;
        this.myId = myId;
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.story_item, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);
        holder.userName.setText(story.getName());
        if(story.getProfile_img() != null){
            holder.profileImage.setImageBitmap(story.getProfile_img());
        }else {
            holder.profileImage.setImageBitmap(BitmapFactory.decodeResource(holder.itemView.getContext().getResources(), R.drawable.profile_img));
        }
        List<Bitmap> storyImages = story.getStory_imgs();
        ImageSliderAdapter imageSliderAdapter = new ImageSliderAdapter(storyImages);
        holder.storyImage.setAdapter(imageSliderAdapter);
        holder.description.setText(story.getPost());
        holder.likeNum.setText(String.valueOf(story.getLikes()));
        holder.date.setText(dateFormat(story.getCreated_at()));
        if(myId != story.getUserId()) {
            holder.revise.setVisibility(View.GONE);
        }else{
            holder.revise.setVisibility(View.VISIBLE);
        }
        if(story.getCheck() == true) {
            holder.like.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.love));
        } else {
            holder.like.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.blanklove));
        }

        holder.profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, UserProfileActivity.class);
                intent.putExtra("userId",story.getUserId());
                intent.putExtra("condition", false);
                context.startActivity(intent);
            }
        });

        holder.like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.like.getBackground().getConstantState().equals(holder.itemView.getContext().getResources().getDrawable(R.drawable.love).getConstantState())){
                    holder.like.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.blanklove));
                    holder.likeNum.setText(String.valueOf(Integer.parseInt(holder.likeNum.getText().toString())-1));
                    story.setLikes(false);
                    story.setCheck(false);
                    likeChange(story.getId(), myId, true);
                }else{
                    holder.like.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.love));
                    holder.likeNum.setText(String.valueOf(Integer.parseInt(holder.likeNum.getText().toString())+1));
                    story.setLikes(true);
                    story.setCheck(true);
                    likeChange(story.getId(), myId, false);
                }
            }
        });

        holder.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.itemView.getContext(), CommentActivity.class);
                intent.putExtra("storyId", story.getId());
                holder.itemView.getContext().startActivity(intent);
                ((Activity) holder.itemView.getContext()).overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
            }
        });

        holder.revise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.menu_options, popupMenu.getMenu());

                // 메뉴 항목 클릭 시 동작 설정
                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_edit) {
                        onItemClickListener.onEditClicked(story.getId(), story.getPost());
                        Toast.makeText(v.getContext(), "스토리 수정", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (itemId == R.id.action_delete) {
                        deleteStory(story.getId());
                        StoryManager.getInstance().deleteStoryList(story.getId());
                        ProfileManager.getInstance().deleteStoryList(story.getId());
                        notifyDataSetChanged();
                        Toast.makeText(v.getContext(), "스토리가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;

                });
                // 팝업 메뉴 보여주기
                popupMenu.show();
            }
        });
    }

    @Override
    public int getItemCount() { return storyList.size(); }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public ImageView profileImage;
        public ViewPager2 storyImage;
        public TextView description;
        public ImageButton like;
        public ImageButton revise;
        public TextView likeNum;
        public TextView date;
        public ImageButton comment;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.tv_name);
            profileImage = itemView.findViewById(R.id.iv_story_profile);
            storyImage = itemView.findViewById(R.id.vp_story_images);
            description = itemView.findViewById(R.id.tv_post);
            like = itemView.findViewById(R.id.ib_like);
            revise = itemView.findViewById(R.id.btn_story_delete);
            likeNum = itemView.findViewById(R.id.tv_like_num);
            date = itemView.findViewById(R.id.tv_date);
            comment = itemView.findViewById(R.id.ib_comment);
        }
    }

    private String dateFormat(String date) {
        Date current = new Date();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date parseDate = inputFormat.parse(date);
            long diffDate = current.getTime() - parseDate.getTime();
            diffDate/=1000;
            if(diffDate/(24 * 3600) == 0) {
                if((diffDate % (24 * 3600)) / 3600 == 0) {
                    return String.valueOf((diffDate % 3600) / 60)+"분 전";
                }
                return String.valueOf((diffDate % (24 * 3600)) / 3600)+"시간 전";
            }else {
                String formatDate = dateFormat.format(parseDate);
                return formatDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteStory(int storyId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.deletestory(storyId);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Log.e("StoryAdapter", "Failed to delete story", t);
            }
        });
    }

    private void likeChange(int storyId, int userId, boolean check) {
        Story likes = new Story(storyId, userId);
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        if(check) {
            retrofit2.Call<Void> call = retrofitService.deleteLike(userId, storyId);
            call.enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

                }
                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    Log.e("StoryAdapter", "Failed to change like", t);
                }
            });
        }else {
            retrofit2.Call<Void> call = retrofitService.postLike(likes);
            call.enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {

                }

                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    Log.e("StoryAdapter", "Failed to send message", t);
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onEditClicked(int storyId, String post);
    }
}
