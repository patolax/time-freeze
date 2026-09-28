package com.time.freezer.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.time.freezer.R;
import com.time.freezer.base.utils.SharedPreferencesManager;
import com.time.freezer.base.view.colorpicker.ColorUtils;
import com.time.freezer.filters.FilterItem;

public class ScanSettings implements Parcelable {
    private int scannerColor;
    private int direction;
    private int shape;
    private int speed;
    private int saveImage;
    private FilterItem filter;
    private boolean voiceCommandsEnabled = true;
    private boolean removeWatermark = false;
    private String imagePath;
    private Bitmap backgroundImage;

    public ScanSettings() {
        scannerColor = Color.RED;
        direction = Constants.SCAN_DIRECTION_VERTICAL;
        shape = Constants.SCAN_SHAPE_LINE;
        speed = Constants.SCAN_SPEED_1X;
        saveImage = Constants.SCAN_DONT_SAVE_IMAGE;
        filter = getDefaultFilter();
        imagePath = null;
    }

    private static FilterItem getDefaultFilter(){
        return new FilterItem("com.time.freezer.filters.NoFilter", "none",
                "https://youtu.be/l8xR5XiTXmM", "No", false, false, false);
    }

    protected ScanSettings(Parcel in) {
        this();
    }

    public static final Creator<ScanSettings> CREATOR = new Creator<ScanSettings>() {
        @Override
        public ScanSettings createFromParcel(Parcel in) {
            return new ScanSettings(in);
        }

        @Override
        public ScanSettings[] newArray(int size) {
            return new ScanSettings[size];
        }
    };

    public int getScannerColor() {
        return scannerColor;
    }

    public void setScannerColor(int scannerColor) {
        this.scannerColor = scannerColor;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getShape() {
        return shape;
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public FilterItem getFilter() {
        return filter;
    }

    public void setFilter(FilterItem filter) {
        this.filter = filter;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(scannerColor);
        dest.writeInt(direction);
        dest.writeInt(shape);
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public boolean isVoiceCommandsEnabled() {
        return voiceCommandsEnabled;
    }

    public void setVoiceCommandsEnabled(boolean voiceCommandsEnabled) {
        this.voiceCommandsEnabled = voiceCommandsEnabled;
    }

    public boolean isRemoveWatermark() {
        return removeWatermark;
    }

    public void setRemoveWatermark(boolean removeWatermark) {
        this.removeWatermark = removeWatermark;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Bitmap getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(Bitmap backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public boolean isBackgroundFilter() {
        if (filter != null && filter.getName().contains("BackgroundImageFilter")) {
            return true;
        }
        return false;
    }
    public int getSaveImage() {
        return saveImage;
    }

    public boolean isSaveImage() {
        return getSaveImage() == Constants.SCAN_SAVE_IMAGE;
    }

    public void setSaveImage(int saveImage) {
        this.saveImage = saveImage;
    }

    public void readFromPreferences(Activity activity){
        int shape = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_SHAPE, this.getShape());
        this.setShape(shape);
        int direction = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_DIRECTION, this.getDirection());
        this.setDirection(direction);
        int speed = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_SPEED, this.getSpeed());
        this.setSpeed(speed);
        int color = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_COLOR, ColorUtils.getColor(activity, R.color.selectedColor));
        this.setScannerColor(color);
        String path = SharedPreferencesManager.getString(activity, SharedPreferencesManager.SCAN_FILTER_IMAGE, null);
        this.setImagePath(path);
        int saveImage = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_SAVE_IMAGE, this.getSaveImage());
        this.setSaveImage(saveImage);
        if (filter != null) {
            String  fname = SharedPreferencesManager.getString(activity, SharedPreferencesManager.SCAN_FILTER_NAME, filter.getName());
            if(fname == null || fname == ""){
                filter = getDefaultFilter();
                return;
            }
            filter.setName(fname);
            int fullImageFilter = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_FILTER_FULLIMAGE, 0);
            filter.setFullImageFilter(fullImageFilter == 1 ? true : false);
        }
    }
}
