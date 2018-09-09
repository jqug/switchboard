package ug.air.switchaccess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.SeekBar;

import java.util.Random;

public class WizardPointScan extends Activity {
    private String TAG = "WizardPointScan";
    Button buttonDone;
    Button buttonTarget;
    SeekBar pointScanSpeed;
    SharedPreferences prefs;
    SharedPreferences.Editor edit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_point_scan);

        /*if (getActionBar() != null) {
            getActionBar().setTitle(getResources().getString(R.string.wizard_point_scan_title));
            //getActionBar().setDisplayHomeAsUpEnabled(true);
        }*/

        // If we're not auto-scanning, then don't show a speed slider
        boolean autoScan = true;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        autoScan = SP.getString("screen_scan_type", "auto").equals("auto");

        if (!autoScan) {
            findViewById(R.id.layoutSpeedSliderPointScanWizard).setVisibility(View.INVISIBLE);
        }

        buttonDone = (Button) findViewById(R.id.buttonPointScanDone);

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WizardKeyboard.class);
                startActivity(intent);
            }
        });

        buttonTarget = (Button) findViewById(R.id.buttonPointScanTarget);

        buttonTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getBaseContext(),"Got it!",Toast.LENGTH_SHORT).show();
                Random rnd = new Random();
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                v.setBackgroundColor(color);
            }
        });

        pointScanSpeed = (SeekBar) findViewById(R.id.seekBarPointScanSpeed);
        prefs = getBaseContext().getSharedPreferences(SwitchboardPreferences.GENERAL_SETTINGS_FILE_KEY, MODE_PRIVATE);
        edit = prefs.edit();
        if (prefs.contains(SwitchboardPreferences.PREFS_SCAN_SPEED)) {
            pointScanSpeed.setProgress(prefs.getInt(SwitchboardPreferences.PREFS_SCAN_SPEED,50));
        }
        else {
            edit.putInt(SwitchboardPreferences.PREFS_SCAN_SPEED,50);
            edit.commit();
        }

        pointScanSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                edit.clear();
                edit.putInt(SwitchboardPreferences.PREFS_SCAN_SPEED,progress);
                edit.commit();

                AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
                if(manager.isEnabled()){
                    AccessibilityEvent event = AccessibilityEvent.obtain();
                    event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                    event.setClassName(getClass().getName());
                    event.setPackageName(getPackageName());
                    event.getText().add(SwitchAccessibilityService.POINT_SCAN_SETTINGS_CHANGE);
                    manager.sendAccessibilityEvent(event);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}