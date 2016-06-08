package com.example.yonggu.module2;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Yonggu on 2015-12-01.
 */
public class SettingActivity extends PreferenceActivity implements View.OnClickListener,
        Preference.OnPreferenceChangeListener{

    private final DatabaseConnector dc = new DatabaseConnector(this);
    private SwitchPreference vibrate_preference;
    private SwitchPreference sound_preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        setContentView(R.layout.activity_settings);

        addPreferencesFromResource(R.xml.preference);

        TextView backBtn = (TextView)findViewById(R.id.back);
        backBtn.setOnClickListener(this);

        SwitchPreference notify_preference = (SwitchPreference)findPreference(getString(R.string.isNotification));
        vibrate_preference = (SwitchPreference)findPreference(getString(R.string.isVibration));
        sound_preference = (SwitchPreference)findPreference(getString(R.string.isSound));

        if(notify_preference.getSharedPreferences().
                getBoolean(getString(R.string.isNotification), false) == false){
            vibrate_preference.setEnabled(false);
            sound_preference.setEnabled(false);
        }

        notify_preference.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back :
                finish();
                break;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if((boolean)newValue){
            vibrate_preference.setEnabled(true);
            sound_preference.setEnabled(true);
        }else{
            vibrate_preference.setEnabled(false);
            sound_preference.setEnabled(false);
        }
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

}
