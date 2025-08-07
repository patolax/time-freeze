package com.time.freezer.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.time.freezer.R;

public class PreviewDialog  extends DialogFragment {

    public static final String TITLE = "dataKey";
    public static final String URL = "urlKey";
    WebView myWebView;
    YouTubePlayerView youTubePlayerView;

    public static PreviewDialog newInstance(String dataToShow, String url) {
        PreviewDialog frag = new PreviewDialog();
        Bundle args = new Bundle();
        args.putString(TITLE, dataToShow);
        args.putString(URL, url);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(TITLE);
        String url = getArguments().getString(URL);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_preview, null);

        youTubePlayerView = view.findViewById(R.id.youtube_player_view);
        TextView txtFilterPreviewTitle = view.findViewById(R.id.txtFilterPreviewTitle);
        txtFilterPreviewTitle.setText(title + " Filter Preview");
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                String videoId = url;
                //youTubePlayer.loadVideo(videoId, 0f);
                Log.d("myApp", videoId);
                YouTubePlayerUtils.loadOrCueVideo(
                        youTubePlayer, getLifecycle(), videoId,0f
                );
            }
        });

        builder.setView(view);
        Dialog dialog = builder.create();

       // dialog.getWindow().setBackgroundDrawable(
        //        new ColorDrawable(Color.TRANSPARENT));

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        //ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        //params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        //params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        //getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(youTubePlayerView != null) {
            youTubePlayerView.release();
            youTubePlayerView = null;
        }
    }
}
