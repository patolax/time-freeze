package com.time.freezer.fragments;

import android.graphics.Bitmap;
import android.net.Uri;

class Video {
    private Uri uri;
    private String name;
    private String thumbnails;
    private Bitmap bitmap;
    private String realPath;

    public Video(Uri uri, String name, String t, String path) {
        this.uri = uri;
        this.name = name;
        this.thumbnails = t;
        this.realPath = path;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getThumbnails() {
        return thumbnails;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getRealPath() {
        return realPath;
    }
}
