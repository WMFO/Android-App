package com.tufts.wmfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Network {

	private static final String DEFAULT_USER_AGENT =
			"Mozilla/5.0 (compatible; WMFO/1.0; +https://play.google.com/store/apps/details?id=com.tufts.wmfo)";
	private static final HttpParams DEFAULT_PARAMS;

	static {
		DEFAULT_PARAMS = new BasicHttpParams();
		HttpProtocolParams.setVersion(DEFAULT_PARAMS, HttpVersion.HTTP_1_0);
		HttpProtocolParams.setUserAgent(DEFAULT_PARAMS, DEFAULT_USER_AGENT);
		HttpConnectionParams.setConnectionTimeout(DEFAULT_PARAMS, 10000);
		HttpConnectionParams.setSoTimeout(DEFAULT_PARAMS, 10000);
	}

	public static String getURL(String URL) throws ClientProtocolException, IOException, URISyntaxException{
		DefaultHttpClient client = new DefaultHttpClient(DEFAULT_PARAMS);
		HttpGet req = new HttpGet(URL);

		HttpResponse res = null;
		try {
			res = client.execute(req);
		} catch (org.apache.http.conn.ConnectTimeoutException e){
			Log.w("GET", "Timeout fetching " + URL);
		}
		if (res != null && res.getStatusLine().getStatusCode() != 200) {
			Log.e("GET", "Status code != 200!");
		}
		if (res != null){
			HttpEntity entity = res.getEntity();
			return EntityUtils.toString(entity);
		} else {
			return null;
		}
	}

	public static Bitmap downloadBitmap(String fileUrl) throws IOException{
		URL myFileUrl = null; 
		Bitmap bmImg = null;
		myFileUrl= new URL(fileUrl);
		HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
		conn.setDoInput(true);
		conn.connect();
		int length = conn.getContentLength();
		if (length > 0){
			InputStream is = conn.getInputStream();
			bmImg =  BitmapFactory.decodeStream(is);
		}
		return bmImg;
	}

	private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

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

	private static String toString(BufferedReader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}
		return sb.toString();
	}
}
