package com.tufts.wmfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class ScrobbleRequest {

	Context appContext;
	SongInfo songInfo;
	SharedPreferences appPreferences;
	public ScrobbleRequest(Context context, SongInfo songInfo){
		this.appContext = context;
		this.songInfo = songInfo;
		appPreferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
	}

	public void send(){
		if (appPreferences.getString("setting_LastFM_Session_Key", null) == null){
			LastFM.processAuth(this.appContext, appPreferences.getString("lastFMUsername", ""),
					appPreferences.getString("lastFMPassword", ""),
					this.appContext.getResources().getString(R.string.LAST_FM_API_KEY),
					this.appContext.getResources().getString(R.string.LAST_FM_API_SECRET));
		}
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("artist", this.songInfo.artist);
		params.put("track", this.songInfo.title);
		params.put("timestamp", Integer.toString((int) (System.currentTimeMillis() / 1000L)));
		params.put("chosenByUser", "0");
		params.put("api_key", this.appContext.getResources().getString(R.string.LAST_FM_API_KEY));
		params.put("sk", appPreferences.getString("setting_LastFM_Session_Key", null));
		String sig = LastFM.createSignature("track.scrobble", params, this.appContext.getResources().getString(R.string.LAST_FM_API_SECRET));
		
		List<NameValuePair> nameValuePairs = LastFM.hashToNameValuePair(params);
		nameValuePairs.add(new BasicNameValuePair("method", "track.scrobble"));
		nameValuePairs.add(new BasicNameValuePair("api_sig", sig));
		
		String result = null;
		try {
			result = LastFM.postData(this.appContext.getResources().getString(R.string.LAST_FM_API_URL_SECURE), nameValuePairs);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (result != null){
			Log.d("SCROBBLE", result);
		}
		
	}

}
