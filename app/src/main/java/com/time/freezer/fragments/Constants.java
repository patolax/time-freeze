package com.time.freezer.fragments;

import com.time.freezer.R;

public class Constants {
    public static String FRAGMENT_INPUT_KEY = "FRAGMENT_INPUT_KEY";

    public final static int SCAN_DIRECTION_HORIZONTAL = 1;
    public final static int SCAN_DIRECTION_VERTICAL = 2;

    public final static int SCAN_SHAPE_LINE = 1;
    public final static int SCAN_SHAPE_CURVE = 2;
    public final static int SCAN_SHAPE_ZIGZAG = 3;

    public final static int SCAN_SPEED_1X = 1;
    public final static int SCAN_SPEED_2X = 2;
    public final static int SCAN_SPEED_3X = 3;

    public final static int SCAN_SAVE_IMAGE = 1;
    public final static int SCAN_DONT_SAVE_IMAGE = 0;

    public static int convertDirectionToText(int toggle) {
        if (toggle == R.id.toggle_horizontal) {
            return R.string.direction_horizontal;
        } else {
            return R.string.direction_vertical;
        }
    }

    public static int convertDirectionToConstant(int toggle) {
        if (toggle == R.id.toggle_horizontal) {
            return SCAN_DIRECTION_HORIZONTAL;
        } else {
            return SCAN_DIRECTION_VERTICAL;
        }
    }

    public static int convertDirectionToToggleId(int toggle) {
        if (toggle == SCAN_DIRECTION_HORIZONTAL) {
            return R.id.toggle_horizontal;
        } else {
            return R.id.toggle_vertical;
        }
    }
    public static int convertSpeedToText(int toggle) {
        if (toggle == R.id.toggle_1x) {
            return R.string.speed_1x;
        } else if (toggle == R.id.toggle_2x) {
            return R.string.speed_2x;
        } else {
            return R.string.speed_3x;
        }
    }

    public static int convertSaveImageToText(boolean state) {
        if (state) {
            return R.string.save_image;
        } else {
            return R.string.dont_save;
        }
    }

    public static int convertSpeedToConstant(int toggle) {
        if (toggle == R.id.toggle_1x) {
            return SCAN_SPEED_1X;
        } else if (toggle == R.id.toggle_2x) {
            return SCAN_SPEED_2X;
        } else {
            return SCAN_SPEED_3X;
        }
    }

    public static int convertSpeedToToggleId(int toggle) {
        if (toggle == SCAN_SPEED_1X) {
            return R.id.toggle_1x;
        } else if (toggle == SCAN_SPEED_2X) {
            return R.id.toggle_2x;
        } else {
            return R.id.toggle_3x;
        }
    }


    public static int convertShapeToText(int toggle) {
        if (toggle == R.id.toggle_line) {
            return R.string.shape_line;
        } else if (toggle == R.id.toggle_curve) {
            return R.string.shape_curve;
        } else {
            return R.string.shape_zigzag;
        }
    }

    public static int convertShapeToConstant(int toggle) {
        if (toggle == R.id.toggle_line) {
            return SCAN_SHAPE_LINE;
        } else if (toggle == R.id.toggle_curve) {
            return SCAN_SHAPE_CURVE;
        } else {
            return SCAN_SHAPE_ZIGZAG;
        }
    }

    public static int convertShapeToToggleId(int toggle) {
        if (toggle == SCAN_SHAPE_LINE) {
            return R.id.toggle_line;
        } else if (toggle == SCAN_SHAPE_CURVE) {
            return R.id.toggle_curve;
        } else {
            return R.id.toggle_zigzag;
        }
    }
}
