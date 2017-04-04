package com.saravagi.piyush.modieuslauncher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.facebook.stetho.Stetho;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase database;
    Cursor cursor;
    PackageManager packageManager;
    List<ApplicationInfo> listOfRecommendedApps;
    List<ApplicationInfo> listOfUserApps;
    GridAdapter gridAdapterAllApps;
    GridAdapter gridAdapterRecommendedApps;
    GridView gridViewAllApps;
    GridView gridViewRecommendedApps;
    SearchView searchView;
    LinearLayout linearLayout;
    TextView horizontalBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linearLayout = (LinearLayout) findViewById(R.id.activity_main);
        searchView = (SearchView) findViewById(R.id.searchView);
        horizontalBar = (TextView) findViewById(R.id.horizontal_bar);

        gridViewAllApps = (GridView) findViewById(R.id.gridview_all_apps);
        gridViewRecommendedApps = (GridView) findViewById(R.id.gridview_recommended_apps);
        listOfUserApps = new ArrayList<ApplicationInfo>();
        packageManager = getPackageManager();

        // Create/ Open the database when the app is launched
        openDatabaseToBeUsed();
        Stetho.initialize(Stetho.newInitializerBuilder(this).enableDumpapp(Stetho.defaultDumperPluginsProvider(this)).enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this)).build());

        List<ApplicationInfo> listOfApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        listOfRecommendedApps = new ArrayList<ApplicationInfo>();

        //<editor-fold desc="Calculating listOfUserApps and inserting new apps into all the databases">
        for (ApplicationInfo app : listOfApps) {
            //checks for flags; if flagged, check if updated system app
            if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                listOfUserApps.add(app);
                //----------------------------------------------------------------------------------
                //Insert into the database
                //----------------------------------------------------------------------------------
                cursor = database.rawQuery("SELECT * FROM applicationFrequencyData WHERE packageName = '" + app.packageName + "'", null);
                if (cursor.getCount() == 0) {
                    database.execSQL("INSERT INTO applicationFrequencyData VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "',0,0);");
                    database.execSQL("INSERT INTO applicationWifiData VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "',0);");
                    database.execSQL("INSERT INTO applicationTimeData(packageName, applicationName) VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "');");
                    database.execSQL("INSERT INTO applicationScoreData VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "',0);");
                }
                //it's a system app, not interested
            } else if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                //Discard this one
                //in this case, it should be a user-installed app
            } else {
                listOfUserApps.add(app);
                cursor = database.rawQuery("SELECT * FROM applicationFrequencyData WHERE packageName = '" + app.packageName + "'", null);
                if (cursor.getCount() == 0) {
                    database.execSQL("INSERT INTO applicationFrequencyData VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "',0,0);");
                    database.execSQL("INSERT INTO applicationWifiData VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "',0);");
                    database.execSQL("INSERT INTO applicationTimeData(packageName, applicationName) VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "');");
                    database.execSQL("INSERT INTO applicationScoreData VALUES('" + app.packageName + "','" + packageManager.getApplicationLabel(app) + "',0);");
                }
            }
        }
        //</editor-fold>

        calculateRecommendations(listOfRecommendedApps);

        gridAdapterAllApps = new GridAdapter(getBaseContext(), listOfUserApps, packageManager, "all");
        gridViewAllApps.setAdapter(gridAdapterAllApps);

        gridAdapterRecommendedApps = new GridAdapter(this, listOfRecommendedApps, packageManager, "recommended");
        gridViewRecommendedApps.setAdapter(gridAdapterRecommendedApps);

        searchView.setOnQueryTextListener(new SearchHandler(getBaseContext(), packageManager, gridViewAllApps, gridViewRecommendedApps, gridAdapterAllApps, linearLayout, horizontalBar, listOfUserApps));
    }

    private void openDatabaseToBeUsed() {
        database = openOrCreateDatabase("ModieusDatabase", Context.MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS applicationFrequencyData(packageName VARCHAR, applicationName VARCHAR, Count INTEGER, Score INTEGER);");
        database.execSQL("CREATE TABLE IF NOT EXISTS applicationWifiData(packageName VARCHAR, applicationName VARCHAR, NoWifi INTEGER);");
        database.execSQL("CREATE TABLE IF NOT EXISTS applicationScoreData(packageName VARCHAR, applicationName VARCHAR, Score INTEGER DEFAULT(0));");
        database.execSQL("CREATE TABLE IF NOT EXISTS applicationTimeData(packageName VARCHAR, applicationName VARCHAR," +
                "Time00 INTEGER DEFAULT(0), Time01 INTEGER DEFAULT(0),Time02 INTEGER DEFAULT(0),Time03 INTEGER DEFAULT(0),Time04 INTEGER DEFAULT(0)," +
                "Time05 INTEGER DEFAULT(0), Time06 INTEGER DEFAULT(0),Time07 INTEGER DEFAULT(0),Time08 INTEGER DEFAULT(0),Time09 INTEGER DEFAULT(0)," +
                "Time10 INTEGER DEFAULT(0), Time11 INTEGER DEFAULT(0),Time12 INTEGER DEFAULT(0),Time13 INTEGER DEFAULT(0),Time14 INTEGER DEFAULT(0)," +
                "Time15 INTEGER DEFAULT(0), Time16 INTEGER DEFAULT(0),Time17 INTEGER DEFAULT(0),Time18 INTEGER DEFAULT(0),Time19 INTEGER DEFAULT(0)," +
                "Time20 INTEGER DEFAULT(0), Time21 INTEGER DEFAULT(0),Time22 INTEGER DEFAULT(0),Time23 INTEGER DEFAULT(0));");
    }

    private void calculateRecommendations(List<ApplicationInfo> listOfRecommendedApps) {

        Cursor cursorForTime = database.rawQuery("SELECT * from applicationTimeData", null);
        Cursor cursorForWifi = database.rawQuery("SELECT * from applicationWifiData", null);
        Cursor cursorForFrequency = database.rawQuery("SELECT * from applicationFrequencyData", null);
        cursorForTime.moveToFirst();
        cursorForWifi.moveToFirst();
        cursorForFrequency.moveToFirst();

        cursor = null;
        cursor = database.rawQuery("SELECT * from applicationFrequencyData", null);
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            int score = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String s = "Time" + sdf.format(new Date());
            String pn = listOfUserApps.get(i).packageName;

            //-----------ADDING SCORE OF TIME--------------
            int t = cursorForTime.getInt(cursorForTime.getColumnIndex(s));
            score = score + cursorForTime.getInt(cursorForTime.getColumnIndex(s));

            //-----------ADDING SCORE OF WIFI--------------
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int w = 0;
            if (wifi.getConnectionInfo().getNetworkId() == -1) {/* not connected */
                w = cursorForWifi.getInt(cursorForWifi.getColumnIndex("NoWifi"));
                score = score + cursorForWifi.getInt(cursorForWifi.getColumnIndex("NoWifi"));
            } else {/* connected */
                String SSID = wifi.getConnectionInfo().getSSID();
                SSID = SSID.substring(1, SSID.length() - 1);
                SSID = SSID.replaceAll("\\s+", "_");
                SSID = SSID.replaceAll("\\W", "_");

                if (cursorForWifi.getColumnIndex("SSID") != -1) {
                    w = cursorForWifi.getInt(cursorForWifi.getColumnIndex(SSID));
                    score = score + cursorForWifi.getInt(cursorForWifi.getColumnIndex(SSID));
                }
            }

            //-----------ADDING SCORE OF FREQUENCY--------------
            int f = cursorForFrequency.getInt(cursorForFrequency.getColumnIndex("Count"));
            score = score + cursorForFrequency.getInt(cursorForFrequency.getColumnIndex("Count"));

            database.execSQL("UPDATE applicationScoreData SET Score = " + score + " WHERE packageName = '" + pn + "';");

            cursor.moveToNext();
            cursorForTime.moveToNext();
            cursorForWifi.moveToNext();
            cursorForFrequency.moveToNext();
        }

        cursor = null;
        cursor = database.rawQuery("SELECT * from applicationScoreData ORDER BY Score DESC LIMIT 4", null);
        cursor.moveToFirst();
        listOfRecommendedApps.clear();
        for (int i = 0; i < 4; i++) {
            try {
                ApplicationInfo app = packageManager.getApplicationInfo(cursor.getString(0), 0);
                listOfRecommendedApps.add(app);
                //Toast.makeText(this, cursor.getInt(2) + " for " + packageManager.getApplicationLabel(app), Toast.LENGTH_SHORT).show();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            cursor.moveToNext();
        }

        if (gridViewRecommendedApps != null)
            gridViewRecommendedApps.invalidateViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        calculateRecommendations(listOfRecommendedApps);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
        cursor.close();
    }
}
