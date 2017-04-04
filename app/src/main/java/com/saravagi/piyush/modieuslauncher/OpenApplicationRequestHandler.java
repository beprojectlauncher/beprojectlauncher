package com.saravagi.piyush.modieuslauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class OpenApplicationRequestHandler implements View.OnClickListener {
    Context context;
    PackageManager packageManager;
    int position;
    List<ApplicationInfo> listOfUserApps;

    public OpenApplicationRequestHandler(Context context, PackageManager packageManager, int
            position, List<ApplicationInfo> listOfUserApps) {
        this.context = context;
        this.packageManager = packageManager;
        this.position = position;
        this.listOfUserApps = listOfUserApps;
    }

    @Override
    public void onClick(View v) {
        launchApp();
        saveToDatabase();
    }

    private void launchApp() {
        Intent intent = packageManager.getLaunchIntentForPackage((listOfUserApps.get(position).packageName));
        context.startActivity(intent);
    }

    //Datbase structure is as follows
    //Package name | Application name | Time in 1 hour slot
    private void saveToDatabase() {
        SQLiteDatabase database = context.openOrCreateDatabase("ModieusDatabase", Context.MODE_PRIVATE, null);

        //<editor-fold desc="Saving in Frequency Database">
        String pn = listOfUserApps.get(position).packageName;
        database.execSQL("UPDATE applicationFrequencyData SET Count = Count + 1 WHERE packageName = '" + pn + "'");
        //</editor-fold>

        //<editor-fold desc="Saving in Wifi Database">
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi.getConnectionInfo().getNetworkId() == -1) {/* not connected */
            database.execSQL("UPDATE applicationWifiData SET NoWifi = NoWifi + 1 WHERE packageName = '" + pn + "'");
        } else {/* connected */
            String SSID = wifi.getConnectionInfo().getSSID();
            SSID = SSID.substring(1, SSID.length() - 1);
            SSID = SSID.replaceAll("\\s+", "_");
            SSID = SSID.replaceAll("\\W", "_");

            Cursor cursor = database.rawQuery("SELECT * FROM applicationWifiData WHERE packageName = '" + pn + "'", null);
            int columnIndex = cursor.getColumnIndex(SSID);
            if (columnIndex == -1) {
                database.execSQL("ALTER TABLE applicationWifiData ADD COLUMN " + SSID + " INTEGER DEFAULT(0);");
            }
            database.execSQL("UPDATE applicationWifiData SET " + SSID + " = " + SSID + " + 1 WHERE packageName = '" + pn + "'");
        }
        //</editor-fold>

        //<editor-fold desc="Saving in Time Database">
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        String s = "Time" + sdf.format(new Date());
        database.execSQL("UPDATE applicationTimeData SET " + s + " = " + s + " + 1 WHERE packageName = '" + pn + "'");
        //</editor-fold>
    }
}
