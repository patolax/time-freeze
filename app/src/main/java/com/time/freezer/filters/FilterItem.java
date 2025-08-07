package com.time.freezer.filters;

public class FilterItem {
    String name;
    String icon;
    String preview;
    boolean isFullImageFilter;
    String title;
    boolean directional;
    boolean isPremium;

    public FilterItem(String n, String i, String p, String t, boolean f, boolean d, boolean u) {
        name = n;
        icon = i;
        preview = p;
        title = t;
        isFullImageFilter = f;
        directional = d;
        isPremium = u;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public boolean isFullImageFilter() {
        return isFullImageFilter;
    }

    public void setFullImageFilter(boolean fullImageFilter) {
        isFullImageFilter = fullImageFilter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDirectional() {
        return directional;
    }

    public void setDirectional(boolean directional) {
        this.directional = directional;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}
