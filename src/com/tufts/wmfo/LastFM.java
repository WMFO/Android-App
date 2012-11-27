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
import java.util.TreeMap;
import java.util.Map.Entry;

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
import android.content.res.Resources.NotFoundException;

public class LastFM {
	
	
	/*
	 * Example responses:
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
			authInfo = postData(context.getResources().getString(R.string.LAST_FM_API_URL_SECURE), postValues);
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
	
	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[512];
		int bytesRead = 1;
		while (bytesRead > 0) {
			bytesRead = in.read(buf);
			if (bytesRead > 0) {
				out.write(buf, 0, bytesRead);
			}
		}
	}
	
	public static String postData(String url, List<NameValuePair> postdata) throws IOException {
		// Create a new HttpClient and Post Header
		URL postURL = new URL(url);
		HttpURLConnection conn = null;
		if (postURL.getProtocol().toLowerCase().equals("https")) {
			trustAllHosts();
			HttpsURLConnection https = (HttpsURLConnection) postURL.openConnection();
			https.setHostnameVerifier(DO_NOT_VERIFY);
			conn = https;
		} else {
			conn = (HttpURLConnection) postURL.openConnection();
		}
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("connection", "close");
		OutputStream ostr = null;
		try {
			ostr = conn.getOutputStream();
			copy(new UrlEncodedFormEntity(postdata).getContent(), ostr);
		} finally {
			if (ostr != null)
				ostr.close();
		}

		BufferedReader reader = null;
		String response = "";
		conn.connect();
		try {
			if(conn.getInputStream() != null)
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
		} catch (IOException e) {
			if(conn.getErrorStream() != null)
				reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()), 512);
		}

		if(reader != null) {
			response = toString(reader);
			reader.close();
		}

		return response;
	} 

	private static String toString(BufferedReader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}
		return sb.toString();
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

	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

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

	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
			.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
