package com.time.freezer.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.time.freezer.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class VideoPlayerDialogFragment extends DialogFragment {

    private VideoView mVideoView;
    FloatingActionButton imgShare;
    Uri uri;
    private OnGalleryClickListener listener;

    public VideoPlayerDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static VideoPlayerDialogFragment newInstance(String uri) {
        VideoPlayerDialogFragment frag = new VideoPlayerDialogFragment();
        Bundle args = new Bundle();
        args.putString("uri", uri);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        mVideoView = (VideoView) view.findViewById(R.id.vidView);
        imgShare = (FloatingActionButton) view.findViewById(R.id.imgShare);
        String suri = getArguments().getString("uri", "");
        uri = Uri.parse(suri);

        imgShare.setOnClickListener(v -> {
            if(listener != null) {
                listener.share(uri);
                dismiss();
            }
        });

        Dialog dialog = getDialog();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.setCanceledOnTouchOutside(true);

        MediaController mediaC = new MediaController(getActivity());
        mediaC.setMediaPlayer(mVideoView);
        mediaC.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediaC);
        mVideoView.setVideoURI(uri);
        mVideoView.start();
    }

    public void setListener(OnGalleryClickListener listener) {
        this.listener = listener;
    }
}