package ug.air.switchaccess;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.R.attr.key;
import static android.content.ContentValues.TAG;

/**
 * Created by jq on 10/15/17.
 */

public class SwitchboardPreferencesActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String currentProgress = SP.getString(getString(R.string.pref_wizard_progress),"wizard_step_introslides");

        if (currentProgress.equals("wizard_step_complete")) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new MyPreferenceFragment()).commit();
        }
        else if (currentProgress.equals(getString(R.string.wizard_step_start))) {
            Intent intent = new Intent(this, WizardSwitchSetup.class);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {

        @Override
        public void onResume() {
            super.onResume();

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

            // If the service is not enabled, then make sure it's indicated disabled on this page
            if (!isAccessibilityEnabled(getActivity().getApplicationContext().getContentResolver())) {
                SP.edit().putBoolean("isServiceEnabled",false).commit();
                Log.d(TAG,"Service not running so showing as disabled.");
                SwitchPreference sp = (SwitchPreference) findPreference("isServiceEnabled");
                sp.setChecked(false);
            }
        }



        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            final Preference serviceEnabledPref = (Preference) findPreference("isServiceEnabled");
            serviceEnabledPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    final boolean isServiceEnabled = serviceEnabledPref.getSharedPreferences().getBoolean("isServiceEnabled",true);
                    if (isServiceEnabled==true) {
                        // Enable the Switchboard keyboard if it's not the default
                        String pkgName = Settings.Secure.getString(getActivity().getApplicationContext().getContentResolver(),
                                Settings.Secure.DEFAULT_INPUT_METHOD);
                        if (!pkgName.contains("ug.air.switchaccess")) {
                            InputMethodManager ime=(InputMethodManager)getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if(ime!=null) {
                                ime.showInputMethodPicker();
                            }
                        }

                        // Start the Switchboard accessibility service if it's stopped
                        if (!isAccessibilityEnabled(getActivity().getApplicationContext().getContentResolver())) {
                            AlertDialog.Builder builder;

                            builder = new AlertDialog.Builder(getActivity());

                            builder.setTitle("Accessibility service needs to be started")
                                    .setMessage("To be able to use Switchboard, the accessibility service needs to be started. Go to Settings -> Accessibility and select Switchboard under the Services list.")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // ok
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();

                            SwitchPreference sp = (SwitchPreference) findPreference("isServiceEnabled");
                            sp.setChecked(false);
                        }
                    }
                    else {
                        String pkgName = Settings.Secure.getString(getActivity().getApplicationContext().getContentResolver(),
                                Settings.Secure.DEFAULT_INPUT_METHOD);
                        if (pkgName.contains("ug.air.switchaccess")) {
                            InputMethodManager ime = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (ime != null) {
                                ime.showInputMethodPicker();
                            }
                        }
                    }
                    Toast.makeText(getActivity(),"Enabled status: " + isServiceEnabled, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            Preference rerunSetupPref = (Preference) findPreference("rerun_setup");
            rerunSetupPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    SharedPreferences.Editor editor = SP.edit();
                    editor.putString(getString(R.string.pref_wizard_progress),getString(R.string.wizard_step_start));
                    editor.commit();
                    Intent intent = new Intent(getActivity(), WizardSwitchSetup.class);
                    startActivity(intent);
                    //Toast.makeText(getActivity(),"Pressed the button rerun setup", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            Preference giveFeedbackPref = (Preference) findPreference("give_feedback");
            giveFeedbackPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    rateApp();
                    return true;
                }
            });

            List<InputMethodInfo> imeList = ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).getEnabledInputMethodList();
            Log.d(TAG,"IME list: " + imeList);
        }

        // https://stackoverflow.com/questions/10816757/rate-this-app-link-in-google-play-store-app-on-the-phone
        public void rateApp()
        {
            try
            {
                Intent rateIntent = rateIntentForUrl("market://details");
                startActivity(rateIntent);
            }
            catch (ActivityNotFoundException e)
            {
                Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details");
                startActivity(rateIntent);
            }
        }

        private Intent rateIntentForUrl(String url)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, "ug.air.switchaccess")));
            int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
            intent.addFlags(flags);
            intent.addFlags(flags);
            return intent;
        }
    }

    public static boolean isAccessibilityEnabled(ContentResolver cr){
        String TAG = "PreferencesActivity";

        int accessibilityEnabled = 0;
        final String ACCESSIBILITY_SERVICE_NAME = "ug.air.switchaccess/ug.air.switchaccess.SwitchAccessibilityService";
        try {
            accessibilityEnabled = Settings.Secure.getInt(cr,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            //Log.d(TAG, "ACCESSIBILITY: " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Error finding setting, default accessibility setting not found: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled==1){
            String settingValue = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            //Log.d(TAG, "Setting: " + settingValue);
            if (settingValue != null) {
                //TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessabilityService = mStringColonSplitter.next();
                    //Log.d(TAG, "Setting: " + accessibilityService);
                    if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)){
                        //Log.d(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Some classes and methods related to enumerating which apps have been installed.


}
