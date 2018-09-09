package ug.air.switchaccess;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MultiAppSelectPreference extends MultiSelectListPreference {

    public ArrayList<PInfo> installedApps;


    public MultiAppSelectPreference (Context context, AttributeSet attrs) {
        super(context, attrs);

        installedApps = getInstalledApps(false);

        CharSequence[] entries = new CharSequence[installedApps.size()];
        CharSequence[] entryValues = new CharSequence[installedApps.size()];

        for (int i=0; i<installedApps.size();i++) {
            entries[i] = installedApps.get(i).appname;
            entryValues[i] = installedApps.get(i).pname;
        }

        setEntries(entries);
        setEntryValues(entryValues);
        //setValueIndex(initializeIndex());
    }

    private void populateAppListPreference() {
        ArrayList<PInfo> apps = getInstalledApps(false);
        for (PInfo app : apps) {
            app.prettyPrint();
        }
    }

    class PInfo {
        private String appname = "";
        private String pname = "";
        private Drawable icon;
        private void prettyPrint() {
            Log.d(TAG,appname + "\t" + pname);
        }
    }

    private ArrayList<PInfo> getPackages() {
        ArrayList<PInfo> apps = getInstalledApps(false); /* false = no system packages */
        final int max = apps.size();
        for (int i=0; i<max; i++) {
            apps.get(i).prettyPrint();
        }
        return apps;
    }

    public class AppNameComparator implements Comparator<PInfo> {
        @Override
        public int compare(PInfo object1, PInfo object2) {
            return object1.appname.compareTo(object2.appname);
        }
    }

    private ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
        ArrayList<PInfo> res = new ArrayList<PInfo>();
        List<PackageInfo> packs = getContext().getPackageManager().getInstalledPackages(0);
        for(int i=0;i<packs.size();i++) {
            PackageInfo p = packs.get(i);
            if ((!getSysPackages) && (p.versionName == null)) {
                continue ;
            }
            PInfo newInfo = new PInfo();
            newInfo.appname = p.applicationInfo.loadLabel(getContext().getPackageManager()).toString();
            newInfo.pname = p.packageName;
            newInfo.icon = p.applicationInfo.loadIcon(getContext().getPackageManager());
            res.add(newInfo);
        }
        Collections.sort(res,new AppNameComparator());
        return res;
    }
}
