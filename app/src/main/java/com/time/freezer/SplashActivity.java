package com.time.freezer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.time.freezer.base.view.splash.SplashSurfaceView;
import com.time.freezer.fragments.SoundEffectsManager;

public class SplashActivity extends Activity implements SplashSurfaceView.SplashAnimationListner {

    SplashSurfaceView surface;
    Intent mainIntent;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.splash);
        surface = (SplashSurfaceView) findViewById(R.id.baseSurface);
        surface.setListner(this);
        mainIntent = new Intent(SplashActivity.this, MainActivity.class);
        SoundEffectsManager.getInstance(getApplicationContext());

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        surface.startDrawThread();
    }

    @Override
    protected void onPause() {
        surface.stopDrawThread();
        super.onPause();
    }

    @Override
    public void animationDone() {
        SplashActivity.this.startActivity(mainIntent);
        SplashActivity.this.finish();
    }
}

