package com.tufts.wmfo;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@TargetApi(9)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_about);
		
		PackageInfo appInfo = null;
		try {
			appInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		if (appInfo != null){
			TextView versionCode = (TextView) findViewById(R.id.about_buildCode);
			TextView versionName = (TextView) findViewById(R.id.about_buildVersion);
			TextView buildDate = (TextView) findViewById(R.id.about_buildDate);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
				buildDate.setText(SimpleDateFormat.getDateInstance().format(new java.util.Date(appInfo.lastUpdateTime)));
			} else {
				buildDate.setVisibility(View.INVISIBLE);
			}
			versionCode.setText(String.valueOf(appInfo.versionCode));
			versionName.setText(appInfo.versionName);
		}
	}
	
}
