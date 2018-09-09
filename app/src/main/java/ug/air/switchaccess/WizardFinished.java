package ug.air.switchaccess;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class WizardFinished extends Activity {
    private String TAG = "WizardFinished";
    Button buttonDone;
    Button buttonGoBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_finished);

        /*if (getActionBar() != null) {
            getActionBar().setTitle(getResources().getString(R.string.wizard_keyboard_scan_title));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }*/

        buttonDone = (Button) findViewById(R.id.buttonWizardFinish);
        buttonGoBack = (Button) findViewById(R.id.buttonWizardGoBackToStart);

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent2 = new Intent(Intent.ACTION_MAIN);
                intent2.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent2);
            }
        });

        buttonGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SwitchboardPreferencesActivity.class);
                startActivity(intent);
            }
        });



    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String currentProgress = SP.getString(getString(R.string.pref_wizard_progress),getString(R.string.wizard_step_start));

        Log.d(TAG,"Current progress: " + currentProgress);

        if (currentProgress.equals(getString(R.string.wizard_step_complete))) {
            Intent intent = new Intent(this, SwitchboardPreferencesActivity.class);
            startActivity(intent);
        }

        SharedPreferences.Editor editor = SP.edit();
        editor.putString(getString(R.string.pref_wizard_progress),getString(R.string.wizard_step_complete));
        editor.commit();
    }
}