package com.saravagi.piyush.modieuslauncher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import static com.saravagi.piyush.modieuslauncher.R.id.grid_item_text;

public class GridAdapter extends BaseAdapter {
    PackageManager packageManager;
    Context context;
    List<ApplicationInfo> listOfApps;
    String tag;

    public GridAdapter(Context context, List<ApplicationInfo> listOfApps, PackageManager packageManager, String tag) {
        this.context = context;
        this.listOfApps = listOfApps;
        this.packageManager = packageManager;
        this.tag = tag;
    }

    @Override

    public int getCount() {
        return listOfApps.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View gridItem = new View(context);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (tag.equals("all")) {
            gridItem = layoutInflater.inflate(R.layout.grid_item_layout_all_apps, null);
        } else if (tag.equals("recommended")) {
            gridItem = layoutInflater.inflate(R.layout.grid_item_layout_recommended_apps, null);
        }
        ImageView gridItemImage = (ImageView) gridItem.findViewById(R.id.grid_item_image);
        TextView gridItemText = (TextView) gridItem.findViewById(grid_item_text);
        gridItemText.setText(packageManager.getApplicationLabel(listOfApps.get(position)));
        gridItemImage.setImageDrawable(packageManager.getApplicationIcon(listOfApps.get(position)));
        gridItem.setOnClickListener(new OpenApplicationRequestHandler(context, packageManager, position, listOfApps));
        return gridItem;
    }
}
