package ug.air.switchaccess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class WizardSystemSettings extends Activity {
    String TAG = "WizardSystemSettings";
    Button buttonEnable;

    TextView settingsProgressText;
    TextView settingExplanationText;
    TextView settingInstructionText;

    Handler handler = new Handler();

    int STEP_ENABLE_SERVICE = 0;
    int STEP_ENABLE_KEYBOARD = 1;
    int STEP_SELECT_KEYBOARD = 2;
    int STEP_FINISHED = 3;

    int mCurrentStep;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_system_settings);

        buttonEnable = (Button) findViewById(R.id.buttonWizardSettingsEnable);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putBoolean("isServiceEnabled", true).commit();

        settingInstructionText = (TextView) findViewById(R.id.textViewWizardSettingInstruction);

        // Work out which step we're at based on current settings
        setCurrentStep();
        updateInterfaceWithCurrentStep();

        // Loop to move to next item when each step is done.
        Runnable r = new Runnable() {
            public void run() {
                if (mCurrentStep == STEP_SELECT_KEYBOARD && isKeyboardSelected()) {
                    setCurrentStep();
                    updateInterfaceWithCurrentStep();
                } else {
                    if (mCurrentStep == STEP_ENABLE_SERVICE && isAccessibilityEnabled()) {
                        setCurrentStep();
                        updateInterfaceWithCurrentStep();
                        Intent intent = new Intent(getApplicationContext(), WizardSystemSettings.class);
                        startActivity(intent);
                    } else if (mCurrentStep == STEP_ENABLE_KEYBOARD && isKeyboardEnabled()) {
                        setCurrentStep();
                        updateInterfaceWithCurrentStep();
                        Intent intent = new Intent(getApplicationContext(), WizardSystemSettings.class);
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.postDelayed(r, 100);
    }

    private void setCurrentStep() {
        if (!isAccessibilityEnabled()) {
            mCurrentStep = STEP_ENABLE_SERVICE;
        } else if (!isKeyboardEnabled()) {
            mCurrentStep = STEP_ENABLE_KEYBOARD;
        } else if (!isKeyboardSelected()) {
            mCurrentStep = STEP_SELECT_KEYBOARD;
        } else {
            mCurrentStep = STEP_FINISHED;
        }
    }

    private void updateInterfaceWithCurrentStep() {
        if (mCurrentStep == STEP_ENABLE_SERVICE) {
            settingInstructionText.setText("Enable Services \u2192 Switchboard in the next screen."); //getResources().getString(R.string.wizard_setup_permissions_allow_service));
            buttonEnable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
                }
            });
        } else if (mCurrentStep == STEP_ENABLE_KEYBOARD) {
            settingInstructionText.setText("Now enable the Switchboard keyboard in the next screen.");
            buttonEnable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
                }
            });
        } else if (mCurrentStep == STEP_SELECT_KEYBOARD) {
            settingInstructionText.setText("...and select Switchboard in the next screen.");
            buttonEnable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm =
                            (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (null != imm) {
                        imm.showInputMethodPicker();
                    }
                }
            });
        } else if (mCurrentStep == STEP_FINISHED) {
            goToNextActivity();
        }
    }

    void goToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), WizardPointScan.class);
        startActivity(intent);
    }

    public boolean isAccessibilityEnabled(){
        int accessibilityEnabled = 0;
        final String ACCESSIBILITY_SERVICE_NAME = "ug.air.switchaccess/ug.air.switchaccess.SwitchAccessibilityService";
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled==1){
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Log.d(TAG, "Setting: " + settingValue);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessabilityService = mStringColonSplitter.next();
                    if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isKeyboardEnabled() {
        String packageLocal = getPackageName();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        List<InputMethodInfo> list = inputMethodManager.getEnabledInputMethodList();

        // check if our keyboard is enabled as input method
        for (InputMethodInfo inputMethod : list) {
            String packageName = inputMethod.getPackageName();
            if (packageName.equals(packageLocal)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKeyboardSelected() {
        String pkgName = Settings.Secure.getString(getBaseContext().getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        return pkgName != null && pkgName.contains(this.getPackageName());
    }
}
