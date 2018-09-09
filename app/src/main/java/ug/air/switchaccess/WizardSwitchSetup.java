package ug.air.switchaccess;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WizardSwitchSetup extends Activity {
    private String TAG = "WizardSwitchSetup";
    Button buttonAdd;
    Button buttonClear;
    Button buttonDone;
    TextView listeningText;
    TextView switchesText;
    TableLayout switchesTable;
    View listeningForSwitch;
    SharedPreferences SP;

    private boolean mListening = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setContentView(R.layout.wizard_switch_setup);

        /*SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String currentProgress = SP.getString(getString(R.string.pref_wizard_progress),getString(R.string.wizard_step_start));

        if (true) { //currentProgress==getString(R.string.wizard_step_complete)) {
            Intent intent = new Intent(this, SwitchboardPreferencesActivity.class);
            startActivity(intent);
        }
        else {

        }*/
        listeningForSwitch = findViewById(R.id.viewListeningForSwitch);
        buttonAdd = (Button) findViewById(R.id.buttonWizardSwitchSetupAdd);
        //buttonClear = (Button) findViewById(R.id.buttonWizardSwitchSetupClear);
        buttonDone = (Button) findViewById(R.id.buttonWizardSwitchSetupDone);
        //switchesText = (TextView) findViewById(R.id.textViewWizardSwitchSetup);
        switchesTable = (TableLayout) findViewById(R.id.setupWizardTable);

        listeningForSwitch.setVisibility(View.GONE);

        SP.edit().putBoolean("isServiceEnabled",false).commit();

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mListening) {
                    buttonAdd.setVisibility(View.GONE);
                    listeningForSwitch.setVisibility(View.VISIBLE);
                }
                mListening = true;
            }
        });

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If we have at least one primary switch then good to go on to the next step.
                if (SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_SELECTITEM).length>0) {

                    // Find out if we should default to auto-scan (1 switch) or manual-scan (2 switches) mode
                    if (SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_NEXTITEM).length>0) {
                        SP.edit().putString("screen_scan_type","manual").commit();
                        SP.edit().putString("keyboard_scan_type","manual").commit();
                    }
                    else {
                        SP.edit().putString("screen_scan_type","auto").commit();
                        SP.edit().putString("keyboard_scan_type","auto").commit();
                    }

                    SP.edit().putBoolean("isServiceEnabled",true).commit();

                    Intent intent = new Intent(WizardSwitchSetup.this, WizardSystemSettings.class);
                    startActivity(intent);
                }
                else {
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(WizardSwitchSetup.this);

                    // Is it that there are no switches at all configured, or none with select function?
                    if (SwitchboardPreferences.getAllAssignedSwitchCodes(getBaseContext()).length>0) {
                        builder.setTitle("At least one primary switch is needed.")
                                .setMessage("To be able to use Switchboard, you need at least one switch with the 'Select Item' function.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ok
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    else {
                        builder.setTitle("At least one switch is needed.")
                                .setMessage("You need at least one switch set up. Press the 'Add a Switch' button on this screen, and then press your switch.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ok
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();

                    }
                }
            }
        });

        updateSwitchView();
    }



    private void addNewSwitch(String switchName, int switchCode, int switchFunction) {

        TableRow newRow = (TableRow) getLayoutInflater().inflate(R.layout.switch_table_row, null);
        switchesTable.addView(newRow);

        // Set the switch name
        TextView switchLabel = (TextView) newRow.getChildAt(0);
        switchLabel.setText(switchName);

        // Set the switch function
        Spinner s = (Spinner) ((LinearLayout) newRow.getChildAt(1)).getChildAt(0);
        s.setSelection(switchFunction);

        // Set the switch code (not viewed, but used for reference in callback)
        TextView textViewSwitchCode = (TextView) ((LinearLayout) newRow.getChildAt(1)).getChildAt(1);
        textViewSwitchCode.setText(String.valueOf(switchCode));

        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                LinearLayout parentLayout = (LinearLayout) parentView.getParent();
                TextView siblingText = (TextView) parentLayout.getChildAt(1);
                int switchCode = Integer.parseInt(String.valueOf(siblingText.getText()));

                SwitchboardPreferences.unassignSwitchCode(getBaseContext(),switchCode);

                String switchType = SwitchboardPreferences.switchTypeFromIndex(position);
                int[] switchCodesOfSameType = SwitchboardPreferences.getSwitchCodes(getBaseContext(),switchType);
                int[] newSwitchCodes = new int[switchCodesOfSameType.length + 1];
                for (int i = 0; i < switchCodesOfSameType.length; i++) {
                    newSwitchCodes[i] = switchCodesOfSameType[i];
                }
                newSwitchCodes[newSwitchCodes.length - 1] = switchCode;
                SwitchboardPreferences.setSwitchCodes(newSwitchCodes,getBaseContext(),switchType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // don't care
            }
        });

        final int thisSwitchCode = switchCode;

        // Configure the delete button
        ImageView deleteButton = (ImageView) newRow.getChildAt(2);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SwitchboardPreferences.unassignSwitchCode(getBaseContext(),thisSwitchCode);
                updateSwitchView();
                mListening = false;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode!=KeyEvent.KEYCODE_BACK && keyCode!=KeyEvent.KEYCODE_POWER
                && keyCode!=KeyEvent.KEYCODE_HOME) {

            if (mListening) {

                listeningForSwitch.setVisibility(View.GONE);

                buttonAdd.setVisibility(View.VISIBLE);

                int[] switchCodes = SwitchboardPreferences.getAllAssignedSwitchCodes(getBaseContext());
                // Test if the switch code is already there
                boolean codeAlreadyThere = false;
                for (int i = 0; i < switchCodes.length; i++) {
                    if (switchCodes[i] == keyCode) {
                        codeAlreadyThere = true;
                        Toast.makeText(getBaseContext(),"That switch has already been registered.",Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (!codeAlreadyThere) {
                    String switchType = SwitchboardPreferences.nextSwitchTypeToAssign(getBaseContext());
                    int[] switchCodesOfSameType = SwitchboardPreferences.getSwitchCodes(getBaseContext(),switchType);
                    int[] newSwitchCodes = new int[switchCodesOfSameType.length + 1];
                    for (int i = 0; i < switchCodesOfSameType.length; i++) {
                        newSwitchCodes[i] = switchCodesOfSameType[i];
                    }
                    newSwitchCodes[newSwitchCodes.length - 1] = keyCode;

                    SwitchboardPreferences.setSwitchCodes(newSwitchCodes, getBaseContext(),switchType);
                    updateSwitchView();
                }
                mListening = false;
                return true;
            }
        }
        return false;
    }

    /**
     * Populate the list of switches on screen with anything already in preferences.
     */
    public void updateSwitchView() {

        // First empty the table (apart from the header row
        //TableLayout switchTable = (TableLayout) findViewById(R.id.setupWizardTable);
        while (switchesTable.getChildCount()>1) {
            switchesTable.removeView(switchesTable.getChildAt(1));
        }

        // Replace the rows with whatever switches are now configured
        for (int switchTypeIdx=0;switchTypeIdx<6;switchTypeIdx++) {
            String switchType = SwitchboardPreferences.switchTypeFromIndex(switchTypeIdx);
            int[] switchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),switchType);
            String switchStr = "";
            for (int i = 0; i < switchCodes.length; i++) {
                switchStr = KeyEvent.keyCodeToString(switchCodes[i]);
                addNewSwitch(switchStr, switchCodes[i], switchTypeIdx);
            }
        }

        if (SwitchboardPreferences.getAllAssignedSwitchCodes(getBaseContext()).length>0) {
            switchesTable.setVisibility(View.VISIBLE);
            buttonAdd.setText("Add another switch");
        }
        else {
            switchesTable.setVisibility(View.GONE);
            buttonAdd.setText("Add a switch");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String currentProgress = SP.getString(getString(R.string.pref_wizard_progress), getString(R.string.wizard_step_start));

        if (currentProgress.equals(getString(R.string.wizard_step_complete))) {
            Log.d(TAG,"Redirecting to settings");
            Intent intent = new Intent(this, SwitchboardPreferencesActivity.class);
            startActivity(intent);
        }
    }
}