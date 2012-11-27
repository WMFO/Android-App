package com.tufts.wmfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;

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
		final CheckBoxPreference scrobblePreference = (CheckBoxPreference) findPreference("lastFMScrobble");

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

		qualityPreference.setSummary(appPreferences.getString("qualityLevel", null) + "kbps");
		qualityFallbackLevel.setSummary(appPreferences.getString("qualityFallbackLevel", null) + "kbps");
		
		qualityPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				qualityPreference.setSummary((String) newValue + "kbps");
				return false;
			}});

		qualityFallbackLevel.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				qualityFallbackLevel.setSummary((String) newValue + "kbps");
				return false;
			}});
		
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

		final Preference lastFMSetup = (Preference) findPreference("lastFMSetup"); 
		if (appPreferences.getString("setting_LastFM_Session_Key", null) == null){
			lastFMSetup.setSummary("Log in to Last.FM");
			scrobblePreference.setEnabled(false);
		} else {
			lastFMSetup.setSummary("Log out of Last.FM");
			scrobblePreference.setEnabled(true);
		}
		lastFMSetup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (appPreferences.getString("setting_LastFM_Session_Key", null) == null){
					Intent intent = new Intent(SettingsActivity.this, LastFMLoginActivity.class);
					startActivity(intent);
				} else {
					appPreferencesEditor.putString("setting_LastFM_Session_Key", null);
					lastFMSetup.setSummary("Log in to Last.FM");
					Log.d("WMFO:SETTINGS", "Removed Last.FM key");
				}
				return false;
			}
		});

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
		final Preference lastFMSetup = (Preference) findPreference("lastFMSetup"); 
		final CheckBoxPreference scrobblePreference = (CheckBoxPreference) findPreference("lastFMScrobble");
		if (appPreferences.getString("setting_LastFM_Session_Key", null) == null){
			lastFMSetup.setSummary("Log in to Last.FM");
			scrobblePreference.setEnabled(false);
		} else {
			lastFMSetup.setSummary("Log out of Last.FM");
			scrobblePreference.setEnabled(true);
		}
	}

}
