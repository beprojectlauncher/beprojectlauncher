package com.saravagi.piyush.modieuslauncher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SearchHandler implements SearchView.OnQueryTextListener {

    Context context;
    GridView gridViewAllApps;
    GridView gridViewRecommendedApps;
    PackageManager packageManager;
    LinearLayout linearLayout;
    TextView horizontalBar;
    List<ApplicationInfo> listOfUserApps;
    List<ApplicationInfo> listOfSearchApps;
    GridAdapter gridAdapterAllApps;

    SearchHandler(Context context, PackageManager packageManager, GridView gridViewAllApps, GridView gridViewRecommendedApps, GridAdapter gridAdapterAllApps, LinearLayout linearLayout, TextView horizontalBar, List<ApplicationInfo> listOfUserApps) {
        this.context = context;
        this.gridViewAllApps = gridViewAllApps;
        this.gridViewRecommendedApps = gridViewRecommendedApps;
        this.linearLayout = linearLayout;
        this.horizontalBar = horizontalBar;
        this.listOfUserApps = listOfUserApps;
        listOfSearchApps = new ArrayList<ApplicationInfo>();
        this.gridAdapterAllApps = gridAdapterAllApps;
        this.packageManager = packageManager;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.equals("")) {
            linearLayout.removeView(gridViewRecommendedApps);
            linearLayout.addView(gridViewRecommendedApps, 1);
            horizontalBar.setVisibility(View.VISIBLE);
            gridAdapterAllApps.listOfApps = listOfUserApps;
        } else {
            CharSequence charSequence = newText.subSequence(0, newText.length());
            linearLayout.removeView(gridViewRecommendedApps);
            horizontalBar.setVisibility(View.INVISIBLE);
            listOfSearchApps.clear();
            listOfSearchApps = new ArrayList<>();
            for (ApplicationInfo app : listOfUserApps) {
                if (packageManager.getApplicationLabel(app).toString().toLowerCase().contains(charSequence)) {
                    listOfSearchApps.add(app);
                } else {
                    if (listOfSearchApps.indexOf(app) != -1) {
                        listOfSearchApps.remove(listOfSearchApps.indexOf(app));
                    }
                }
            }
            gridAdapterAllApps.listOfApps = listOfSearchApps;
        }
        gridViewAllApps.invalidateViews();
        return false;
    }
}
