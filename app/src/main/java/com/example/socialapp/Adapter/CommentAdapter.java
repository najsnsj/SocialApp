package com.example.socialapp.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.Entity.Comment;
import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.ImageUtils;
import com.example.socialapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private List<Comment> additionList = new ArrayList<>();
    private OnCommentClickListener listener;
    private Context context;
    private int myId;

    public CommentAdapter(Context context ,List<Comment> commentList, int myId, OnCommentClickListener listener) {
        this.commentList = commentList;
        this.context = context;
        this.myId = myId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        if(comment.getUser_id() == myId) {
            holder.deleteComment.setVisibility(View.VISIBLE);
        } else {
            holder.deleteComment.setVisibility(View.GONE);
        }
        holder.addLinear.setVisibility(View.GONE);
        holder.mention.setText("");
        ViewGroup.LayoutParams params = holder.totalLinear.getLayoutParams();
        RecyclerView.LayoutParams recyclerViewParams = (RecyclerView.LayoutParams) params;
        recyclerViewParams.setMargins(0, 0, 0, 0);
        holder.totalLinear.setLayoutParams(recyclerViewParams); // 초기화

        holder.userName.setText(comment.getName());
        holder.profileImage.setImageBitmap(comment.getProfile_img());
        holder.comment.setText(comment.getComment());

        holder.comment.post(() -> {
            if(holder.comment.getLineCount() >= 3) {
                holder.maxLine.setVisibility(View.VISIBLE);
            } else {
                holder.maxLine.setVisibility(View.GONE);
            }
        });

        holder.maxLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.maxLine.getText().equals("더보기")) {
                    holder.comment.setMaxLines(Integer.MAX_VALUE);
                    holder.comment.setEllipsize(null);
                    holder.maxLine.setText("댓글 접기");
                }else {
                    holder.comment.setMaxLines(10);
                    holder.comment.setEllipsize(TextUtils.TruncateAt.END);
                    holder.maxLine.setText("더보기");
                }
            }
        });

        if(comment.getCount() > 0 && comment.getCheck() == false) {
            holder.addition.setText("답글"+String.valueOf(comment.getCount())+"개 더보기");
            holder.addLinear.setVisibility(View.VISIBLE);

           // ViewGroup.LayoutParams params = holder.totalLinear.getLayoutParams();
            if (params instanceof RecyclerView.LayoutParams) {
            //    RecyclerView.LayoutParams recyclerViewParams = (RecyclerView.LayoutParams) params;

                recyclerViewParams.setMargins(0, 0, 0, 0);
                holder.totalLinear.setLayoutParams(recyclerViewParams);
            }
        } else if(comment.getCount() > 0 && comment.getCheck() == true) {
            holder.addition.setText("답글 숨기기");
            holder.addLinear.setVisibility(View.VISIBLE);
            if (params instanceof RecyclerView.LayoutParams) {
                recyclerViewParams.setMargins(0, 0, 0, 0);
                holder.totalLinear.setLayoutParams(recyclerViewParams);
            }
        }

        if(comment.getAnswerId() != 0) {
            holder.mention.setText("@"+comment.getAnswerName());
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics(); // 화면 전체의 비율 가져오기
            int screenWidth = displayMetrics.widthPixels;

            int marginInPixels = (int) (screenWidth * 0.15);    // 너비의 15%

            if (params instanceof RecyclerView.LayoutParams) {
                recyclerViewParams.setMargins(marginInPixels,0,0,0); // 좌, 상, 우, 하
                holder.totalLinear.setLayoutParams(recyclerViewParams);
            }
            holder.totalLinear.setLayoutParams(params);
            holder.addLinear.setVisibility(View.GONE);

        }

        holder.addComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onCommentClick(comment.getName(), comment.getId());
                }
            }
        });

        holder.deleteComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = holder.getAdapterPosition(); // 현재 위치 가져오기
                if (currentPosition != RecyclerView.NO_POSITION) { // 유효한 위치인지 확인
                    deleteComment(commentList.get(currentPosition).getId());
                    commentList.remove(currentPosition);
                    notifyItemRemoved(currentPosition);
                }
            }
        });

        holder.addition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (comment.getCheck() == false) {
                    holder.addition.setText("답글 숨기기");
                    getAdditionComment(comment.getStoryId(), comment.getId(), new CommentFetchListener() {
                        @Override
                        public void onCommentsFetched(List<Comment> additionList) {
                            if (additionList != null && !additionList.isEmpty()) {
                                // 추가된 답글을 기존 commentList에 삽입
                                int insertPosition = holder.getAdapterPosition() + 1; // 현재 위치 바로 다음에 추가
                                commentList.addAll(insertPosition, additionList);

                                // 어댑터에 데이터 변경 알림 (새로 추가된 답글 부분만 갱신)
                                notifyItemRangeInserted(insertPosition, additionList.size());
                            }
                        }
                    });
                    comment.setCheck(true);
                } else {
                    int insertPosition = holder.getAdapterPosition() + 1; // 현재 위치 바로 다음
                    int countToHide = 0; // 숨길 댓글 개수

                    for (int i = insertPosition; i < commentList.size(); i++) {
                        Comment addedComment = commentList.get(i);
                        if (addedComment.getAnswerId() != 0) { // 원래 댓글의 answerId와 일치하는지 확인
                            countToHide++;
                        } else {
                            break; // 추가된 댓글이 끝나면 루프 종료
                        }
                    }
                        commentList.subList(insertPosition, insertPosition + countToHide).clear(); // 리스트에서 댓글 제거
                        notifyItemRangeRemoved(insertPosition, countToHide); // RecyclerView에 데이터 변경 알림

                    holder.addition.setText("답글"+String.valueOf(comment.getCount())+"개 더보기"); // 버튼 텍스트 변경
                    comment.setCheck(false);
                }
            }
        });
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public TextView comment;
        public TextView addComment;
        public TextView deleteComment;
        public TextView maxLine;
        public TextView addition;
        public TextView mention;
        public ImageView profileImage;
        public LinearLayout addLinear;
        public LinearLayout totalLinear;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.tv_comment_name);
            comment = itemView.findViewById(R.id.tv_comment);
            addComment = itemView.findViewById(R.id.tv_add_comment);
            deleteComment = itemView.findViewById(R.id.tv_delete_comment);
            maxLine = itemView.findViewById(R.id.tv_max_comment);
            addition = itemView.findViewById(R.id.tv_addition);
            mention = itemView.findViewById(R.id.tv_mention);

            profileImage = itemView.findViewById(R.id.iv_comment_profile);

            addLinear = itemView.findViewById(R.id.ll_addition);
            totalLinear = itemView.findViewById(R.id.ll_comment);
        }
    }

    private void getAdditionComment(int storyId, int commentId, CommentFetchListener listener) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getAdditionComments(storyId, commentId);
        call.enqueue(new retrofit2.Callback<ResponseBody>(){
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        additionList.clear();
                        for(int i=0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            int storyId = jsonObject.getInt("story_id");
                            int answerId = jsonObject.getInt("answer_id");
                            int userId = jsonObject.getInt("user_id");
                            String comment = jsonObject.getString("comment");
                            String created_at = jsonObject.getString("created_at");
                            String name = jsonObject.getString("name");
                            String answerName = jsonObject.getString("answer_name");
                            String profile = jsonObject.optString("profile_img", null);
                            Bitmap img = null;
                            if (profile != null && !profile.isEmpty() && !profile.equals("null")) {
                                img = ImageUtils.base64ToBitmap(profile);
                            } else {
                                img = BitmapFactory.decodeResource(context.getResources(), R.drawable.profile_img);
                            }
                            additionList.add(new Comment(id, storyId, answerId, answerName, userId, comment, created_at, name, img));
                        }
                        listener.onCommentsFetched(additionList);
                        notifyDataSetChanged();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) { }
        });
    }

    private void deleteComment(int commentId) {
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.deleteComments(commentId);
        call.enqueue(new retrofit2.Callback<ResponseBody>(){
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) { }
        });
    }

    public interface CommentFetchListener {
        void onCommentsFetched(List<Comment> additionList);
    }

    public interface OnCommentClickListener {
        void onCommentClick(String text, int id);
    }
}
