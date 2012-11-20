package com.tufts.wmfo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/*
 * 
 * Pull twitter
 * 
 */

public class MainActivity extends Activity {
	
	Timer updateCurrentTimer;
	JSONArray twitterJSON;

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
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ImageView playButton = (ImageView) findViewById(R.id.mainscreen_Button_play);
		final ImageView phoneButton = (ImageView) findViewById(R.id.mainscreen_Button_phone);
		phoneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_CALL);
				intent.setData(Uri.parse(getResources().getString(R.string.WMFO_PHONE_NUMBER)));
				if (AudioService.isRunning != null && AudioService.isRunning){
					stopService(new Intent(MainActivity.this, AudioService.class));
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
				}
				MainActivity.this.startActivity(intent);
			}
		});
		if (AudioService.isRunning != null && AudioService.isRunning){
			playButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
		} else {
			playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
		}

		updateCurrentTimer = new Timer();
		updateCurrentTimer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				setNowPlaying();
				//updateCurrentTimer.schedule(this, 1000);
			}}, 0, 10000);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AudioService.isRunning != null && AudioService.isRunning){
					stopService(new Intent(MainActivity.this, AudioService.class));
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
				} else {
					startService(new Intent(MainActivity.this, AudioService.class));
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
				}
			}
		});

		/*
		 * Get Twitter JSON Data 
		 */
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					String twitterJSONString = executeGET("https://api.twitter.com/1/statuses/user_timeline.json?include_entities=true&include_rts=true&screen_name=wmfo&count=10");
					twitterJSON = new JSONArray(twitterJSONString);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						ListView twitterList = (ListView) findViewById(R.id.mainscreen_twitterListLayout);
						twitterList.setAdapter(new TweetListViewAdapter(MainActivity.this, parseTwitterJSON(twitterJSON)));
					}});
			}}).start();
	}

	ArrayList<TwitterStatus> parseTwitterJSON(JSONArray twitterJSON){
		ArrayList<TwitterStatus> tweets = new ArrayList<TwitterStatus>();
		if (twitterJSON == null){
			return tweets;
		}
		for (int i=0; i < twitterJSON.length(); i++){
				try {
					TwitterStatus status = new TwitterStatus(twitterJSON.getJSONObject(i));
					tweets.add(status);
				} catch (JSONException e) {
					e.printStackTrace();
				}
		}
		return tweets;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_settings:
	        	startActivity(new Intent(this, SettingsActivity.class));
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	void setNowPlaying(){
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
			final SongInfo nowPlaying = new SongInfo(SpinInfo, true);
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					TextView Track = (TextView) findViewById(R.id.mainscreen_Track);
					Track.setText(nowPlaying.title);
					
					TextView Artist = (TextView) findViewById(R.id.mainscreen_Artist);
					Artist.setText("By " + nowPlaying.artist);
					
					TextView Album = (TextView) findViewById(R.id.mainscreen_Album);
					Album.setText("From " + nowPlaying.album);
				
					TextView DJ = (TextView) findViewById(R.id.mainscreen_DJ);
					DJ.setText("Spun by " + nowPlaying.DJ);
					
					TextView Show = (TextView) findViewById(R.id.mainscreen_Show);
					Show.setText("On " + nowPlaying.showName);
				}});
		}
	}

	public String executeGET(String getURL) throws ClientProtocolException, IOException, URISyntaxException{

		DefaultHttpClient client = new DefaultHttpClient(DEFAULT_PARAMS);
		HttpGet req = new HttpGet(getURL);

		HttpResponse res = null;
		try {
			res = client.execute(req);
		} catch (org.apache.http.conn.ConnectTimeoutException e){
			Log.w("GET", "Timeout fetching " + getURL);
		}
		if (res != null && res.getStatusLine().getStatusCode() != 200) {
			Log.e(TAG, "Status code != 200!");
		}
		if (res != null){
			HttpEntity entity = res.getEntity();
			return EntityUtils.toString(entity);
		} else {
			return "";
		}
	}


}

