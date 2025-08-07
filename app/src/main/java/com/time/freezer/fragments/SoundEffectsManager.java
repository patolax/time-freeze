package com.time.freezer.fragments;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.time.freezer.R;

import static android.media.AudioAttributes.*;

public class SoundEffectsManager {
    SoundPool soundPool;
    int soundIds[] = new int[1];
    public static SoundEffectsManager instance;

    public static SoundEffectsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundEffectsManager(context);
        }
        return instance;
    }

    private SoundEffectsManager(Context context) {
        AudioAttributes attrs = new Builder()
                .setUsage(USAGE_ASSISTANT)
                .setContentType(CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(15)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {

                // Play the sound when loaded
                playScan();
            }
        });

        soundIds[0] = soundPool.load(context, R.raw.scan, 1);
    }

    public void play(int id) {
        soundPool.play(id, 0.4f, 0.4f, 1, 0, 1.0f);
    }


    public void playScan() {
        play(soundIds[0]);
    }

}
