package com.example.socialapp.Adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Entity.Message;
import com.example.socialapp.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private  int userId;
    private String myName;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public MessageAdapter(List<Message> messageList, int userId, String myName) {
        this.messageList = messageList;
        this.userId = userId;
        this.myName = myName;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if(viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sent_item_message, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.receive_item_message, parent, false);
        }

        return new MessageViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId() == userId) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message, userId, myName);

        int displayWidth = holder.itemView.getContext().getResources().getDisplayMetrics().widthPixels;
        int maxWidth = (int) (displayWidth * 0.7);

        // TextView 너비 설정
        if (message.getSenderId() == userId) {
            holder.sentMessage.setMaxWidth(maxWidth);

            ViewGroup.LayoutParams imageParams = holder.sentImage.getLayoutParams();
            imageParams.width = maxWidth;
            holder.sentImage.setLayoutParams(imageParams);
            holder.sentImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.sentImage.setAdjustViewBounds(true);

            ViewGroup.LayoutParams videoParams = holder.sentVideo.getLayoutParams();
            videoParams.width = maxWidth;
            holder.sentVideo.setLayoutParams(videoParams);
            holder.sentVideo.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
        } else {
            holder.receivedMessage.setMaxWidth(maxWidth);

            ViewGroup.LayoutParams imageParams = holder.receivedImage.getLayoutParams();
            imageParams.width = maxWidth;
            holder.receivedImage.setLayoutParams(imageParams);
            holder.receivedImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.receivedImage.setAdjustViewBounds(true);

            ViewGroup.LayoutParams videoParams = holder.receivedVideo.getLayoutParams();
            videoParams.width = maxWidth;
            holder.receivedVideo.setLayoutParams(videoParams);
            holder.receivedVideo.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
        }
    }

    @Override
    public void onViewRecycled(@NonNull MessageViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer();
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView sentMessage;
        public TextView receivedMessage;
        public TextView receiveTime;
        public TextView sentTime;
        public TextView receiveName;
        public TextView sentName;
        public ImageView receivedImage;
        public ImageView sentImage;
        public PlayerView receivedVideo;
        public PlayerView sentVideo;

        private ExoPlayer exoPlayer;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            receivedMessage = itemView.findViewById(R.id.receive_message);
            sentMessage = itemView.findViewById(R.id.sent_message);
            receiveTime = itemView.findViewById(R.id.receive_message_time);
            sentTime = itemView.findViewById(R.id.sent_message_time);
            receiveName = itemView.findViewById(R.id.tv_receive_name);
            sentName = itemView.findViewById(R.id.tv_sent_name);
            receivedImage = itemView.findViewById(R.id.receive_image);
            sentImage = itemView.findViewById(R.id.sent_image);

            receivedVideo = itemView.findViewById(R.id.receive_video);
            sentVideo = itemView.findViewById(R.id.sent_video);
        }

        public void bind(Message message, int userId, String myName) {
            String serverURL = RetrofitInstance.getBaseUrl();
            Uri videoUri = Uri.parse(serverURL + message.getMessageText());
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
            }

            if (message.getSenderId() == userId) {
                if("text".equals(message.getType())) {
                    sentMessage.setVisibility(View.VISIBLE);
                    sentImage.setVisibility(View.GONE);
                    sentVideo.setVisibility(View.GONE);
                    sentMessage.setText(message.getMessageText());
                    sentTime.setVisibility(View.VISIBLE);
                    sentTime.setText(dateFormat(message.getCreatedAt()));
                    sentName.setVisibility(View.VISIBLE);
                    sentName.setText(myName);
                } else if("image".equals(message.getType())) {      // 이미지 채팅
                    sentMessage.setVisibility(View.GONE);
                    sentImage.setVisibility(View.VISIBLE);
                    sentVideo.setVisibility(View.GONE);
                    Picasso.get()
                            .load(serverURL + message.getMessageText()) // 이미지 URL
                            .placeholder(R.drawable.add) // 로딩 중 표시할 이미지
                            .error(R.drawable.addpost)       // 로드 실패 시 표시할 이미지
                            .into(sentImage);
                    sentTime.setVisibility(View.VISIBLE);
                    sentTime.setText(dateFormat(message.getCreatedAt()));
                    sentName.setVisibility(View.VISIBLE);
                    sentName.setText(myName);
                } else {        // 비디오 채팅
                    sentMessage.setVisibility(View.GONE);
                    sentImage.setVisibility(View.GONE);
                    sentVideo.setVisibility(View.VISIBLE);
                    sentTime.setVisibility(View.VISIBLE);
                    sentTime.setText(dateFormat(message.getCreatedAt()));
                    sentName.setVisibility(View.VISIBLE);
                    sentName.setText(myName);

                    MediaItem mediaItem = MediaItem.fromUri(videoUri);
                    exoPlayer.setMediaItem(mediaItem);
                    sentVideo.setPlayer(exoPlayer);
                    sentVideo.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    exoPlayer.prepare();
                    exoPlayer.play();
                }
            } else {
                if("text".equals(message.getType())) {
                    receivedMessage.setVisibility(View.VISIBLE);
                    receivedImage.setVisibility(View.GONE);
                    receivedVideo.setVisibility(View.GONE);
                    receivedMessage.setText(message.getMessageText());
                    receiveTime.setVisibility(View.VISIBLE);
                    receiveTime.setText(dateFormat(message.getCreatedAt()));
                    receiveName.setVisibility(View.VISIBLE);
                    receiveName.setText(message.getName());
                } else if("image".equals(message.getType())) {
                    receivedMessage.setVisibility(View.GONE);
                    receivedImage.setVisibility(View.VISIBLE);
                    receivedVideo.setVisibility(View.GONE);
                    Picasso.get()
                            .load(serverURL + message.getMessageText()) // 이미지 URL
                            .placeholder(R.drawable.add) // 로딩 중 표시할 이미지
                            .error(R.drawable.addpost)       // 로드 실패 시 표시할 이미지
                            .into(receivedImage);
                    receiveTime.setVisibility(View.VISIBLE);
                    receiveTime.setText(dateFormat(message.getCreatedAt()));
                    receiveName.setVisibility(View.VISIBLE);
                    receiveName.setText(message.getName());
                } else {
                    receivedMessage.setVisibility(View.GONE);
                    receivedImage.setVisibility(View.GONE);
                    receivedVideo.setVisibility(View.VISIBLE);
                    receiveTime.setVisibility(View.VISIBLE);
                    receiveTime.setText(dateFormat(message.getCreatedAt()));
                    receiveName.setVisibility(View.VISIBLE);
                    receiveName.setText(message.getName());

                    MediaItem mediaItem = MediaItem.fromUri(videoUri);
                    exoPlayer.setMediaItem(mediaItem);
                    receivedVideo.setPlayer(exoPlayer);
                    receivedVideo.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    exoPlayer.prepare();
                    exoPlayer.play();
                }
            }
        }
        public void releasePlayer() {
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }
        }
        private String dateFormat(String date) {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat dayFormat = new SimpleDateFormat("HH시 mm분");

            dayFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            try {
                Date parseDate = inputFormat.parse(date);
                String formatDate = dayFormat.format(parseDate);
                return formatDate;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
