package com.tufts.wmfo;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;

public class AudioService extends Service implements AudioManager.OnAudioFocusChangeListener, OnPreparedListener{

	//Service-wide tools
	WifiLock wifiLock;
	MediaPlayer mediaPlayer;
	Timer updateCurrentTimer;
	public static Boolean isRunning;
	SharedPreferences appPreferences;
	SongInfo CurrentSong;
	long lastSawInternet;
	boolean connectedOK;
	boolean switching;
	
	ConnectivityManager connManager;
	
	private static final String DEFAULT_USER_AGENT =
			"Mozilla/5.0 (compatible; StreamScraper/1.0; +http://code.google.com/p/streamscraper/)";
	private static final HttpParams DEFAULT_PARAMS;

	static {
		DEFAULT_PARAMS = new BasicHttpParams();
		HttpProtocolParams.setVersion(DEFAULT_PARAMS, HttpVersion.HTTP_1_0);
		HttpProtocolParams.setUserAgent(DEFAULT_PARAMS, DEFAULT_USER_AGENT);
		HttpConnectionParams.setConnectionTimeout(DEFAULT_PARAMS, 10000);
		HttpConnectionParams.setSoTimeout(DEFAULT_PARAMS, 10000);
	}

	final static String TAG = "WMFO:SERVICE";

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		isRunning = true;
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		appPreferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		updateCurrentTimer = new Timer();
		registerReceiver(networkBroadCastReciever, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		connectedOK = true;
		switching = false;
		this.lastSawInternet = System.currentTimeMillis();
	}

	@Override
	public void onDestroy() {
		isRunning = false;
		Log.d(TAG, "onDestroy");
		updateCurrentTimer.cancel();
		mediaPlayer.stop();
		mediaPlayer.release();
		mediaPlayer = null;
		unregisterReceiver(networkBroadCastReciever);
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Log.d(TAG, "onStart");
		initMediaPlayer();
	}

	public void onPrepared(MediaPlayer mp) {
		mp.start();
		connectedOK = true;
		switching = false;
		updateCurrentTimer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				updateNowPlaying();
				//updateCurrentTimer.schedule(this, 1000);
			}}, 0, 5000);
		Log.d(TAG, "onPrepared");
	}
	
	public void setNotification(SongInfo... nowPlaying){
		Notification notification = new Notification();
//		notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification_layout);
//		if (nowPlaying.length > 0) {
//			notification.contentView.setTextViewText(R.id.notificationLayout_artist, nowPlaying[0].artist);
//			notification.contentView.setTextViewText(R.id.notificationLayout_details, nowPlaying[0].title);
//		} else {
//			// assign the song name to songName
//			notification.contentView.setTextViewText(R.id.notificationLayout_artist, "WMFO");
//			notification.contentView.setTextViewText(R.id.notificationLayout_details, "Currently streaming");
//		}
//		//Handle the stop button
//		Intent stopService = new Intent(getApplicationContext(), MainActivity.class); 
//		stopService.putExtra("ACTION", "STOP");
//		notification.contentView.setOnClickPendingIntent(R.id.notificationLayout_stopButton, 
//				PendingIntent.getActivity(getApplicationContext(), 0, stopService, 0));

		notification.icon = R.drawable.ic_launcher;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	            new Intent(this, MainActivity.class), 0);
		if (mediaPlayer != null && mediaPlayer.isPlaying()){
			notification.tickerText = "Now Listening To WMFO";
			notification.setLatestEventInfo(this, ((nowPlaying.length > 0) ? nowPlaying[0].artist : "WMFO") , ((nowPlaying.length > 0) ? nowPlaying[0].title : "Currently streaming"), contentIntent);
		} else {
			notification.tickerText = "Loading WMFO Stream...";
			notification.setLatestEventInfo(this, "WMFO" , "Buffering...", contentIntent);
		}
		startForeground(R.id.WMFO_NOTIFICATION_ID, notification);
	}

	public void showStreamError(){
		Notification notification = new Notification();
		notification.icon = R.drawable.ic_launcher;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	            new Intent(this, MainActivity.class), 0);
			notification.tickerText = "WMFO Streaming Error";
			notification.setLatestEventInfo(this, "Stream Error" , "Could not play the stream", contentIntent);
		startForeground(R.id.WMFO_NOTIFICATION_ID, notification);
	}
	
	public void initMediaPlayer(){
		switching = true;
		//Lock WIFI on
		wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
		wifiLock.acquire();

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnErrorListener(new OnErrorListener(){
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e("WMFO:MEDIA", "Media player has crashed! Code " + what + " Extra: " + extra);
				//mp.release();
				/*if (extra == -110){
					Log.d("WMFO:MEDIA", "Connection timed out");
					initMediaPlayer();
				}*/
				updateCurrentTimer.cancel();
				showStreamError();
				AudioService.this.stopSelf();
				return false;
			}});
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnInfoListener(new OnInfoListener(){
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				Log.i("WMFO:MEDIA", "Info available: " + what + ", extra: " + extra);
				if(what == 703){
					updateCurrentTimer.cancel();
					showStreamError();
					AudioService.this.stopSelf();
				}
				return false;
			}});
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		   
		try {
			if (connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected() || !appPreferences.getBoolean("dropQuality", false)) {
				Log.d(TAG, "Wifi On or no fallback");
				String qualityLevel = appPreferences.getString("qualityLevel", "256");
				if (qualityLevel.equals("256")){
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_HQ));
				} else if (qualityLevel.equals("128")){
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_MQ));
				} else if (qualityLevel.equals("64")){
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_LQ));
				} else {
					//dafuq
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_HQ));
				}
			} else {
				Log.d(TAG, "Wifi Off and fallback");
				String qualityLevel = appPreferences.getString("qualityFallbackLevel", "128");
				if (qualityLevel.equals("256")){
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_HQ));
				} else if (qualityLevel.equals("128")){
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_MQ));
				} else if (qualityLevel.equals("64")){
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_LQ));
				} else {
					//dafuq
					mediaPlayer.setDataSource(getString(R.string.WMFO_STREAM_URL_HQ));
				}
			}
			mediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
			mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			setNotification();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}       
		//To nullify the player, use mediaPlayer.release();
	}

	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			// resume playback
			if (mediaPlayer == null) initMediaPlayer();
			else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
			mediaPlayer.setVolume(1.0f, 1.0f);
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			// Lost focus for an unbounded amount of time: stop playback and release media player
			if (mediaPlayer.isPlaying()) mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			// Lost focus for a short time, but we have to stop
			// playback. We don't release the media player because playback
			// is likely to resume
			if (mediaPlayer.isPlaying()) mediaPlayer.pause();
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			// Lost focus for a short time, but it's ok to keep playing
			// at an attenuated level
			if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
			break;
		}
	}

	void updateNowPlaying(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				String SpinInfo = null;
				try {
					//SpinInfo = executeGET("http://wmfo-duke.orgs.tufts.edu:8000/7.html");
					SpinInfo = executeGET("http://spinitron.com/public/newestsong.php?station=wmfo");
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				if (SpinInfo != null && SpinInfo != ""){
					AudioService.this.connectedOK=true;
					SongInfo nowPlaying = new SongInfo(SpinInfo, true);
					setNotification(nowPlaying);
						if (CurrentSong != null && !CurrentSong.equals(nowPlaying)){
						if (appPreferences.getBoolean("lastFMScrobble", false)){
							new ScrobbleRequest(AudioService.this, CurrentSong).send();
						}
					}
					CurrentSong = nowPlaying;
				} else {
					AudioService.this.connectedOK=false;
				}
				Log.d(TAG, "Done updating NP");
			}}).start();
	}

	public String executeGET(String getURL) throws ClientProtocolException, IOException, URISyntaxException{

		DefaultHttpClient client = new DefaultHttpClient(DEFAULT_PARAMS);
		HttpGet req = new HttpGet(getURL);

		HttpResponse res;
		res = client.execute(req);
		if (res.getStatusLine().getStatusCode() != 200) {
			Log.e(TAG, "Status code != 200!");
		}
		HttpEntity entity = res.getEntity();
		return EntityUtils.toString(entity);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private BroadcastReceiver networkBroadCastReciever =
	        new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getExtras()!=null) {
				NetworkInfo ni=(NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
				if(ni!=null && ni.getState()==NetworkInfo.State.CONNECTED) {
					Log.i("WMFO:NET","Network "+ni.getTypeName()+" connected");
					if (!AudioService.this.connectedOK && (System.currentTimeMillis() - AudioService.this.lastSawInternet) < 15000){
						Log.d("WMFO:NET", "Less than 15 seconds since we lost data, reconnect");
						AudioService.this.mediaPlayer.release();
						AudioService.this.mediaPlayer = null;
						initMediaPlayer();
					} else if (ni.getTypeName().equals("WIFI") && !AudioService.this.switching){
						Log.d("WMFO:NET", "Found WIFI! Connect to it");
						AudioService.this.lastSawInternet = System.currentTimeMillis();
						AudioService.this.mediaPlayer.release();
						AudioService.this.mediaPlayer = null;
						initMediaPlayer();
					} else if ((System.currentTimeMillis() - AudioService.this.lastSawInternet) > 15000){
						Log.d("WMFO:NET", "Too much time since we last had internet (" + (System.currentTimeMillis() - AudioService.this.lastSawInternet) + ") - stop");
						updateCurrentTimer.cancel();
						showStreamError();
						AudioService.this.stopSelf();
					}
				}
			}
			if(intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY,Boolean.FALSE)) {
				Log.d("WMFO:NET", "Setting last saw audio time to " + System.currentTimeMillis());
				AudioService.this.lastSawInternet = System.currentTimeMillis();
			}
		}
		
	};

	
}
