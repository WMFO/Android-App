package com.tufts.wmfo;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LastFMLoginActivity extends Activity {

	SharedPreferences appPreferences;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lastfm_login);
		
		appPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
		
//		@+id/lastFM_auth_loginButton
//		@+id/lastFM_auth_password
//		@+id/lastFM_auth_username
//		@+id/lastFM_auth_errorMessage
//		@+id/lastFM_auth_inputLayout
		
		Button loginButton = (Button) findViewById(R.id.lastFM_auth_loginButton);
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final LinearLayout inputLayout = (LinearLayout) findViewById(R.id.lastFM_auth_inputLayout);
				inputLayout.setVisibility(View.INVISIBLE);
				final ProgressBar progressSpinner = (ProgressBar) findViewById(R.id.lastFM_auth_progressBar);
				progressSpinner.setVisibility(View.VISIBLE);
				final TextView errorMessage = (TextView) findViewById(R.id.lastFM_auth_errorMessage);
				final EditText lastfmUsername = (EditText) findViewById(R.id.lastFM_auth_username);
				final EditText lastfmPassword = (EditText) findViewById(R.id.lastFM_auth_password);
				
				new Thread(new Runnable(){
					@Override
					public void run() {
						JSONObject results = LastFM.processAuth(LastFMLoginActivity.this, lastfmUsername.getEditableText().toString(),
								lastfmPassword.getEditableText().toString(),
								Auth.LAST_FM_API_KEY,
								Auth.LAST_FM_API_SECRET);
						Log.d("WMFO:LASTFM", results.toString());
						if (results.has("session")){
							SharedPreferences.Editor editor = appPreferences.edit();
							try {
								editor.putString("setting_LastFM_Session_Key", results.getJSONObject("session").getString("key"));
								LastFMLoginActivity.this.finish();
							} catch (JSONException e) {
								e.printStackTrace();
							}
							editor.commit();
							runOnUiThread(new Runnable(){
								public void run() {
									progressSpinner.setVisibility(View.INVISIBLE);
								}});
						} else {
							runOnUiThread(new Runnable(){
								public void run() {
									inputLayout.setVisibility(View.VISIBLE);
									progressSpinner.setVisibility(View.INVISIBLE);
									errorMessage.setVisibility(View.VISIBLE);
								}});
						}
					}}).start();
			}
		});
	}

}
