package com.time.freezer.fragments;

import android.net.Uri;

public interface OnGalleryClickListener {
    void onClick(Video item);
    void share(Uri uri);
}
