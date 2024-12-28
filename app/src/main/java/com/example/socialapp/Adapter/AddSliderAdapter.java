package com.example.socialapp.Adapter;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialapp.R;

import java.util.List;

public class AddSliderAdapter extends RecyclerView.Adapter<AddSliderAdapter.ViewHolder> {

    private List<Uri> imageUriList;
    private List<Bitmap> imageBitmapList;


    public AddSliderAdapter(List<Uri> imageUriList, boolean check) {
        if(!check) {
            this.imageUriList = imageUriList;
        }
    }

    public AddSliderAdapter(List<Bitmap> imageBitmapList) { this.imageBitmapList = imageBitmapList; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(imageUriList != null) {
            Uri imageUri = imageUriList.get(position);
            holder.imageView.setImageURI(imageUri);
        } else if(imageBitmapList != null) {
            Bitmap bitmap = imageBitmapList.get(position);
            holder.imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public int getItemCount() {
        if(imageUriList != null) {
            return imageUriList.size();
        } else if(imageBitmapList != null) {
            return imageBitmapList.size();
        } else {
            return 0;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_story_item);
        }
    }
}
