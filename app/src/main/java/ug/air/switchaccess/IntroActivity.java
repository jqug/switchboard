package ug.air.switchaccess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;

public class IntroActivity extends AppIntro {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroFragment.newInstance("Welcome to Switchboard", "Switchboard allows physically disabled users to fully control their Android device with a single hardware button or switch.", R.drawable.button_press, Color.parseColor("#026e8c"))); //2196F3
        addSlide(AppIntroFragment.newInstance("Hardware compatible", "Works with most Bluetooth or USB switches, or nearly anything with a physical button.", R.drawable.switches, Color.parseColor("#9cafb7")));
        addSlide(AppIntroFragment.newInstance("Select anything easily", "Select items by scanning the screen, from left to right and top to bottom", R.drawable.phone_crosshairs, Color.parseColor("#c57b57")));
        addSlide(AppIntroFragment.newInstance("Write quickly", "Text entry mode is specially designed for switch operation.", R.drawable.phone_texting, Color.parseColor("#63a375")));
        addSlide(AppIntroFragment.newInstance("Get started", "Next, you can set up your switch device and activate Switchboard.", R.drawable.button_press, Color.parseColor("#2c666e")));

        showSkipButton(true);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        //Intent intent = new Intent(getApplicationContext(), WizardSwitchSetup.class);
        //startActivity(intent);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = SP.edit();
        editor.putString(getString(R.string.pref_wizard_progress),getString(R.string.wizard_step_start));
        editor.commit();
        Intent intent = new Intent(this, WizardSwitchSetup.class);
        startActivity(intent);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = SP.edit();
        editor.putString(getString(R.string.pref_wizard_progress),getString(R.string.wizard_step_start));
        editor.commit();

        // Do something when users tap on Done button.
        Intent intent = new Intent(getApplicationContext(), WizardSwitchSetup.class);
        startActivity(intent);
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}