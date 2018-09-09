package ug.air.switchaccess;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class WizardKeyboard extends Activity {
    private String TAG = "WizardPointScan";
    Button buttonDone;
    SeekBar keyboardScanSpeed;
    SharedPreferences prefs;
    SharedPreferences.Editor edit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_keyboard);

        /*if (getActionBar() != null) {
            getActionBar().setTitle(getResources().getString(R.string.wizard_keyboard_scan_title));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }*/

        buttonDone = (Button) findViewById(R.id.buttonKeyboardDone);

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WizardFinished.class);
                startActivity(intent);
            }
        });

        boolean autoScan = true;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        autoScan = SP.getString("screen_scan_type", "auto").equals("auto");

        if (!autoScan) {
            findViewById(R.id.layoutSpeedSliderKeyboardWizard).setVisibility(View.INVISIBLE);
        }
        else {

            keyboardScanSpeed = (SeekBar) findViewById(R.id.seekBarKeyboardSpeed);
            prefs = getBaseContext().getSharedPreferences(SwitchboardPreferences.GENERAL_SETTINGS_FILE_KEY, MODE_PRIVATE);
            edit = prefs.edit();
            if (prefs.contains(SwitchboardPreferences.PREFS_SCAN_SPEED)) {
                keyboardScanSpeed.setProgress(prefs.getInt(SwitchboardPreferences.PREFS_SCAN_SPEED, 50));
            } else {
                edit.putInt(SwitchboardPreferences.PREFS_SCAN_SPEED, 50);
                edit.commit();
            }

            keyboardScanSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    edit.clear();
                    edit.putInt(SwitchboardPreferences.PREFS_SCAN_SPEED, progress);
                    edit.commit();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}