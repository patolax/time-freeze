package com.time.freezer.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.time.freezer.R;
import com.time.freezer.filters.FilterItem;

import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {

    List<FilterItem> filters;
    Context context;
    OnFilterClickListener listener;
    FilterItem selected;
    boolean premiumUser;
    private int selectedPosition = 0;

    public FilterAdapter(Context c, List<FilterItem> f, OnFilterClickListener l, boolean p) {
        context = c;
        filters = f;
        listener = l;
        selected = f.get(0);
        premiumUser = p;
    }

    @Override
    public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.filter_item, parent, false);
        FilterViewHolder vh = new FilterViewHolder(v); // pass the view to View Holder
        return vh;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(FilterViewHolder holder, final int position) {
        FilterItem current = filters.get(position);
        holder.imageView.setImageDrawable(getIcon(current.getIcon()));
        if (selected.equals(current)) {
            holder.layoutHighlight.setVisibility(View.VISIBLE);
            selectedPosition = position;
        } else {
            holder.layoutHighlight.setVisibility(View.GONE);
        }
        holder.btnLock.setVisibility(View.INVISIBLE);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterItem clicked = filters.get(position);
                listener.onFilterClick(clicked);
                selected = clicked;
                notifyDataSetChanged();
            }
        });
    }

    public void setSelected(FilterItem selectedItem) {
        selected = selectedItem;
        notifyDataSetChanged();
    }

    public void setPremiumUser(boolean user) {
        premiumUser = user;
        notifyDataSetChanged();
    }

    private Drawable getIcon(String icon) {
        Resources res = context.getResources();
        int resID = res.getIdentifier(icon, "drawable", context.getPackageName());
        return res.getDrawable(resID);
    }
    @Override
    public int getItemCount() {
        return filters.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public class FilterViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView imageView;
        LinearLayout layoutHighlight;
        ImageView btnLock;

        public FilterViewHolder(View itemView) {
            super(itemView);
            imageView = (com.google.android.material.imageview.ShapeableImageView) itemView.findViewById(R.id.imgFilter);
            btnLock = (ImageView) itemView.findViewById(R.id.btnLock);
            layoutHighlight = (LinearLayout) itemView.findViewById(R.id.layoutHighlight);
        }
    }
}
