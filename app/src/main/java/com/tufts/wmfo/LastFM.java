package com.tufts.wmfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class LastFM {


	public static JSONObject getAlbumInfo(Context context, String artistName, String albumName){
		/*
		 * http://ws.audioscrobbler.com/2.0/
		 * ?method=album.getinfo
		 * &api_key=6e96448bf11affb604a0bdef2b811214
		 * &artist=Cher
		 * &album=Believe
		 */
		
		String getURL = context.getResources().getString(R.string.LAST_FM_API_URL);
		try {
			getURL += "&method=album.getinfo"
					+ "&api_key=" + Auth.LAST_FM_API_KEY
					+ "&artist=" + URLEncoder.encode(artistName, "utf-8")
					+ "&album=" + URLEncoder.encode(albumName, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}
		String albumInfoString = null;
		Log.d("WMFO:PIC", getURL);
		try {
			albumInfoString = Network.getURL(getURL);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		if (albumInfoString != null){
			try {
				return new JSONObject(albumInfoString);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return new JSONObject();
	}

	/*
	 * Example responses (for auth):
	 * Error (bad username):
	 * {"message":"Invalid username. No last.fm account associated with that name.","error":4,"links":[]}
	 * Error (bad password):
	 * {"message":"Invalid password. Please check username\/password supplied","error":4,"links":[]}
	 * Success:
	 * {"session":{"subscriber":"0","key":"<key>","name":"RossSchlaikjer"}}
	 * 
	 */

	public static JSONObject processAuth(Context context, String username, String password, String api_key, String api_secret){
		List<NameValuePair> postValues = getMobileSession(username, password, api_key, api_secret);

		String authInfo = null;
		try {
			authInfo = Network.postData(context.getResources().getString(R.string.LAST_FM_API_URL_SECURE), postValues);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (authInfo != null){
			try {
				return new JSONObject(authInfo);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
		}
		return null;
	}

	public static List<NameValuePair> hashToNameValuePair(Map<String, String> params){
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		for (Entry<String, String> entry : params.entrySet()) {
			nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		return nameValuePairs;
	}

	private static List<NameValuePair> getMobileSession(String username, String password, String apiKey, String secret) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("api_key", apiKey);
		params.put("method", "auth.getMobileSession");
		params.put("username", username);
		params.put("password", password);
		String sig = createSignature("auth.getMobileSession", params, secret);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
		nameValuePairs.add(new BasicNameValuePair("username", username));
		nameValuePairs.add(new BasicNameValuePair("password", password));
		nameValuePairs.add(new BasicNameValuePair("method", "auth.getMobileSession"));
		nameValuePairs.add(new BasicNameValuePair("api_key", apiKey));
		nameValuePairs.add(new BasicNameValuePair("api_sig", sig));
		return nameValuePairs;
	}

	public static String createSignature(String method, Map<String, String> params, String secret) {
		params = new TreeMap<String, String>(params);
		params.put("method", method);
		StringBuilder b = new StringBuilder(100);
		for (Entry<String, String> entry : params.entrySet()) {
			b.append(entry.getKey());
			b.append(entry.getValue());
		}
		b.append(secret);
		return md5(b.toString());
	}

	public static String md5(String source){
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] bytes = digest.digest(source.getBytes("UTF-8"));
			StringBuilder b = new StringBuilder(32);
			for (byte aByte : bytes) {
				String hex = Integer.toHexString((int) aByte & 0xFF);
				if (hex.length() == 1)
					b.append('0');
				b.append(hex);
			}
			return b.toString();
		} catch (UnsupportedEncodingException e) {
			// utf-8 always available
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}



}
