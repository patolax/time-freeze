package com.time.freezer.fragments;

import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.time.freezer.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Video}.
 * TODO: Replace the implementation with code for your data type.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private final List<Video> mValues;
    OnGalleryClickListener listener;
    int imageWidth = 90;
    ContentResolver resolver;

    public GalleryAdapter(List<Video> items, OnGalleryClickListener listener, int imageWidth, ContentResolver resolver) {
        this.listener = listener;
        mValues = items;
        this.imageWidth = imageWidth;
        this.resolver = resolver;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        //Bitmap bitmap = mValues.get(position).getBitmap();
        holder.mContentView.setImageBitmap(mValues.get(position).getBitmap());
        holder.mContentView.getLayoutParams().width = imageWidth;
        holder.mContentView.getLayoutParams().height = (int) (imageWidth * (16 * 1.0f / 9));
        if (holder.mItem.getBitmap() == null) {
            new BitmapWorkerTask(holder.mContentView, holder.imgGalleryShare).execute(holder.mItem);
            holder.imgGalleryShare.setVisibility(View.INVISIBLE);
        }
        holder.mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(holder.mItem);
            }
        });

        holder.imgGalleryShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.share(holder.mItem.getUri());
            }
        });
        holder.layoutFake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.share(holder.mItem.getUri());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mContentView;
        public final FloatingActionButton imgGalleryShare;
        public final LinearLayout layoutFake;

        //public final RelativeLayout parentView;
        public Video mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (ImageView) view.findViewById(R.id.imgTumb);
            imgGalleryShare = (FloatingActionButton) view.findViewById(R.id.imgGalleryShare);
            layoutFake = (LinearLayout) view.findViewById(R.id.layoutFake);
        }
    }

    class BitmapWorkerTask extends AsyncTask<Video, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<FloatingActionButton> fabReference;
        private Video video;

        public BitmapWorkerTask(ImageView imageView, FloatingActionButton fab) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            fabReference = new WeakReference<FloatingActionButton>(fab);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Video... params) {
            video = params[0];
            Bitmap bitmap = loadVideoThumbnail(video.getUri(), video.getRealPath(), resolver);
            video.setBitmap(bitmap);
            return bitmap;
        }

        public Bitmap loadVideoThumbnail(Uri videoFilePath, String picturePath, ContentResolver cr) {
            Bitmap result = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Size mSize = new Size(128, 128);
                CancellationSignal ca = new CancellationSignal();
                try {
                    result = cr.loadThumbnail(videoFilePath, mSize, ca);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                result = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MINI_KIND);
            }
            return result;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
                final FloatingActionButton fabR = fabReference.get();
                if (fabR != null) {
                    fabR.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}