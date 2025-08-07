package com.time.freezer.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.time.freezer.R;
import com.time.freezer.databinding.FragmentGalleryListBinding;
import com.time.freezer.databinding.FragmentLandingBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A fragment representing a list of Items.
 */
public class GalleryFragment extends Fragment implements OnGalleryClickListener {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 2;
    Activity activity;
    List<Video> videoList = new ArrayList<Video>();
    GalleryAdapter adapter;

    RecyclerView rvGallery;

    RelativeLayout loadingGalleryLayout;

    TextView txtNoRecording;
    private FirebaseAnalytics mFirebaseAnalytics;
    FragmentGalleryListBinding binding;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GalleryFragment() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static GalleryFragment newInstance(int columnCount) {
        GalleryFragment fragment = new GalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = getActivity();
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGalleryListBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        rvGallery = binding.rvGallery;
        loadingGalleryLayout =binding.loadingGalleryLayout;
        txtNoRecording = binding.txtNoRecording;
        // Set the adapter
        Context context = view.getContext();
        RecyclerView recyclerView = rvGallery;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        int imageWidth = Math.round(outMetrics.widthPixels / mColumnCount);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), mColumnCount);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new GalleryAdapter(videoList, this, imageWidth, activity.getContentResolver());
        recyclerView.setAdapter(adapter);
        new GetGalleryData().execute("");
        return view;
    }

    private void playVid(Video video) {
        FragmentManager fm = this.getChildFragmentManager();
        VideoPlayerDialogFragment videoPlayerDialogFragment = VideoPlayerDialogFragment.newInstance(video.getUri().toString());
        videoPlayerDialogFragment.setListener(this);
        videoPlayerDialogFragment.show(fm, "fragment_edit_name");
       /* final Dialog dialog = new Dialog(activity);// add here your class name
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_video);//add your own xml with defied with and height of videoview
        dialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.copyFrom(dialog.getWindow().getAttributes());
        dialog.getWindow().setAttributes(lp);
        Uri uriPath= video.getUri();

        activity.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        Log.v("Vidoe-URI", uriPath+ "");
        final VideoView mVideoView = (VideoView) dialog.findViewById(R.id.vidView);
        MediaController  mediaC  = new MediaController(activity);
        mediaC.setMediaPlayer(mVideoView);
        mediaC.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediaC);
        mVideoView.setVideoURI(uriPath);
        mVideoView.start();*/
    }

    private List<Video> getVids() {
        Uri collection;
        List<Video> tempList = new ArrayList<Video>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Thumbnails.DATA,
                MediaStore.Images.Media.DATA,
        };
        String selection = MediaStore.Video.Media.DISPLAY_NAME +
                " LIKE ?";
        String[] selectionArgs = new String[]{VideoFragment.TEST_VIDEO_FILE_NAME + "%"};
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        ContentResolver resolver = activity.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int tumbColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String tumb = cursor.getString(tumbColumn);
                String path = cursor.getString(pathColumn);
                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                if (new File(path).exists()) {
                    Video vid = new Video(contentUri, name, tumb, path);
                    tempList.add(vid);
                }
            }
        }
        return tempList;
    }

    private static final String SELECTION = MediaStore.MediaColumns.DATA + "=?";
    private static final String[] PROJECTION = {BaseColumns._ID};

    public static Bitmap loadVideoThumbnail(Uri videoFilePath, ContentResolver cr) {
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
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = cr.query(videoFilePath, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            result = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MINI_KIND);
        }
        return result;
    }

    @Override
    public void onClick(Video item) {
        if (item != null) {
            playVid(item);
        }
    }

    @Override
    public void share(Uri uri) {
        logEvent("Share", "click");
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("video/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }

    private void logEvent(String eventId, String data){
        if(mFirebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, eventId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, eventId);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, data);
        mFirebaseAnalytics.logEvent(eventId, bundle);
    }

    private class  GetGalleryData extends AsyncTask<String, Void, String> {
        List<Video> vids;

        @Override
        protected String doInBackground(String... params) {
            vids = getVids();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            videoList.clear();
            videoList.addAll(vids);
            if(videoList.size() == 0 ){
                txtNoRecording.setVisibility(View.VISIBLE);
            }
            loadingGalleryLayout.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}