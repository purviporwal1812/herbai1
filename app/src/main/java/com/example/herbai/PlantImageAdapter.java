package com.example.herbai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class PlantImageAdapter extends RecyclerView.Adapter<PlantImageAdapter.ImageViewHolder> {
    private static final String TAG = "PlantImageAdapter";
    private Context context;
    private List<String> imageUrls;

    public PlantImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
        Log.d(TAG, "PlantImageAdapter created with " + imageUrls.size() + " images");
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_plant_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        Log.d(TAG, "Binding image at position " + position + ": " + imageUrl);

        // Show loading initially
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.errorTextView.setVisibility(View.GONE);
        holder.imageView.setImageDrawable(null);

        // RequestOptions for placeholder/error
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_error_image)
                .placeholder(R.drawable.ic_placeholder_image);

        // Use a RequestListener to handle success/failure and hide progressBar
        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Glide load failed for: " + imageUrl, e);
                        // Hide progress and show error message
                        holder.progressBar.setVisibility(View.GONE);
                        holder.errorTextView.setVisibility(View.VISIBLE);
                        // Let Glide handle setting the error drawable; return false
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // Hide progress and error text when image loaded
                        holder.progressBar.setVisibility(View.GONE);
                        holder.errorTextView.setVisibility(View.GONE);
                        // Let Glide handle setting the image on the view; return false
                        return false;
                    }
                })
                .into(holder.imageView);

        // Set click listener for full screen view
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            intent.putExtra("image_url", imageUrl);
            intent.putExtra("image_position", position + 1);
            intent.putExtra("total_images", imageUrls.size());
            // If adapter was created with application context, need FLAG_NEW_TASK
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public void updateImages(List<String> newImageUrls) {
        this.imageUrls.clear();
        this.imageUrls.addAll(newImageUrls);
        notifyDataSetChanged();
        Log.d(TAG, "Images updated, new count: " + imageUrls.size());
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        ProgressBar progressBar;
        TextView errorTextView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.imageCardView);
            imageView = itemView.findViewById(R.id.plantImageView);
            progressBar = itemView.findViewById(R.id.imageProgressBar);
            errorTextView = itemView.findViewById(R.id.errorTextView);
        }
    }
}
