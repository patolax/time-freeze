package com.time.freezer.filters;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FilterManager {
    List<FilterItem> filters = new ArrayList<>();

    public List<FilterItem> load(Context context) {
        try {
            JSONArray m_jArry = new JSONArray(loadJSONFromAsset(context));
            filters.clear();
            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                String name = jo_inside.getString("name");
                String icon = jo_inside.getString("icon");
                String preview = jo_inside.getString("preview");
                String title = jo_inside.getString("title");
                boolean isFullImageFilter = jo_inside.getBoolean("isFullImageFilter");
                boolean directional = jo_inside.getBoolean("directional");
                boolean isPremium = jo_inside.getBoolean("isPremium");
                filters.add(new FilterItem(name, icon, preview, title, isFullImageFilter, directional, isPremium));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return filters;
    }

    private String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("filters.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }


}
