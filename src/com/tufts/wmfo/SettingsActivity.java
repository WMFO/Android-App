package com.tufts.wmfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences appPreferences;
	private SharedPreferences.Editor appPreferencesEditor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);
		// Get the custom preference
		final ListPreference qualityPreference = (ListPreference) findPreference("qualityLevel");
		final ListPreference qualityFallbackLevel = (ListPreference) findPreference("qualityFallbackLevel");
		final CheckBoxPreference dropQualityPreference = (CheckBoxPreference) findPreference("dropQuality");

		appPreferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		appPreferencesEditor = appPreferences.edit();
		
		if (appPreferences.getBoolean("dropQuality", false)){
			qualityFallbackLevel.setEnabled(true);
		}
		
		if (appPreferences.getString("qualityLevel", null) == null){
			appPreferencesEditor.putString("qualityLevel", "256");
		}
		if (appPreferences.getString("qualityFallbackLevel", null) == null){
			appPreferencesEditor.putString("qualityFallbackLevel", "256");
		}
		
		//dropQualityPreference.setChecked(appPreferences.getBoolean("SETTING_dropQuality", false));

		dropQualityPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if ((Boolean) newValue == true){
					qualityFallbackLevel.setEnabled(true);
				} else {
					qualityFallbackLevel.setEnabled(false);
				}
				return true;
			}
		});
		
		//qualityPreference.set
		
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void onStop(){
		Log.d("PREFS", "Saving preferences");
		appPreferencesEditor.commit();
		super.onStop();
	}

	@Override
	public void onPause(){
		super.onPause();
	}

	@Override
	public void onResume(){
		super.onResume();
	}

}
