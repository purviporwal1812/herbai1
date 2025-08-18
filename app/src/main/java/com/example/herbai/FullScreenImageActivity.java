package com.example.herbai;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

public class FullScreenImageActivity extends AppCompatActivity {
    private static final String TAG = "FullScreenImage";

    private PhotoView photoView;
    private ProgressBar progressBar;
    private TextView imageInfoTextView;
    private ImageView closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        initializeViews();
        loadImageData();
        setupCloseButton();
    }

    private void initializeViews() {
        photoView = findViewById(R.id.fullScreenImageView);
        progressBar = findViewById(R.id.fullScreenProgressBar);
        imageInfoTextView = findViewById(R.id.imageInfoTextView);
        closeButton = findViewById(R.id.closeButton);
    }

    private void loadImageData() {
        String imageUrl = getIntent().getStringExtra("image_url");
        int imagePosition = getIntent().getIntExtra("image_position", 1);
        int totalImages = getIntent().getIntExtra("total_images", 1);

        // Set image info
        imageInfoTextView.setText("Image " + imagePosition + " of " + totalImages);

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // Load full resolution image with a RequestListener to handle start/failure/success
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_error_image);

        Glide.with(this)
                .load(imageUrl)
                .apply(requestOptions)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // Hide progress and let Glide set the error drawable (because we returned false)
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Image load failed: " + imageUrl, e);
                        return false; // return false so Glide will handle setting the error drawable
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // Hide progress when image is loaded
                        progressBar.setVisibility(View.GONE);
                        return false; // return false so Glide will handle setting the image on the view
                    }
                })
                .into(photoView);

        Log.d(TAG, "Loading full screen image: " + imageUrl);
    }

    private void setupCloseButton() {
        closeButton.setOnClickListener(v -> finish());

        // Also allow tapping the image to close
        photoView.setOnClickListener(v -> finish());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
