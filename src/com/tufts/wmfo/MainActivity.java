package com.tufts.wmfo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

/*
 * 
 * Pull twitter
 * 
 */

public class MainActivity extends TabActivity {

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
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
		super.onSaveInstanceState(savedInstanceState);
		if (twitterJSON != null){
			savedInstanceState.putString("tweets", twitterJSON.toString());
		}

		TextView Track = (TextView) findViewById(R.id.mainscreen_Track);
		savedInstanceState.putString("nowplaying_track", Track.getText().toString());
		TextView Artist = (TextView) findViewById(R.id.mainscreen_Artist);
		savedInstanceState.putString("nowplaying_artist", Artist.getText().toString());
		TextView Album = (TextView) findViewById(R.id.mainscreen_Album);
		savedInstanceState.putString("nowplaying_album", Album.getText().toString());
		TextView DJ = (TextView) findViewById(R.id.mainscreen_DJ);
		savedInstanceState.putString("nowplaying_dj", DJ.getText().toString());
		TextView Show = (TextView) findViewById(R.id.mainscreen_Show);
		savedInstanceState.putString("nowplaying_show", Show.getText().toString());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			setContentView(R.layout.activity_main);
		} else {
			setContentView(R.layout.activity_main_landscape);
		}

		saveStateIndependentSetup();

		if (savedInstanceState == null){
			noSaveStateSetup();
		} else {
			saveStateSetup(savedInstanceState);
		}

	}


	private void saveStateIndependentSetup(){

		final ImageView playButton = (ImageView) findViewById(R.id.mainscreen_Button_play);
		final ImageView phoneButton = (ImageView) findViewById(R.id.mainscreen_Button_phone);

		/*
		 * Phone in button 
		 */
		phoneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.setData(Uri.parse(getResources().getString(R.string.WMFO_PHONE_NUMBER)));
				if (AudioService.isRunning != null && AudioService.isRunning){
					stopService(new Intent(MainActivity.this, AudioService.class));
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
				}
				MainActivity.this.startActivity(intent);
			}
		});

		/*
		 * Play audio button
		 * - Show correct icon for service state
		 * - Add click listener to start/stop service 
		 */
		if (AudioService.isRunning != null && AudioService.isRunning){
			playButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
		} else {
			playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
		}

		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AudioService.isRunning != null && AudioService.isRunning){
					stopService(new Intent(MainActivity.this, AudioService.class));
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
				} else {
					Intent startIntent = new Intent(MainActivity.this, AudioService.class);
					startIntent.putExtra("source", getString(R.string.WMFO_STREAM_URL_HQ));
					//http://wmfo-duke.orgs.tufts.edu/cgi-bin/castbotv2?s-year=2012&s-month=11&s-day=18&s-hour=02&e-year=2012&e-month=11&e-day=18&e-hour=03/archive.mp3
					startService(startIntent);
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));

					if (updateCurrentTimer == null){
						updateCurrentTimer = new Timer();
						updateCurrentTimer.scheduleAtFixedRate(new TimerTask(){
							@Override
							public void run() {
								setNowPlaying();
								//updateCurrentTimer.schedule(this, 1000);
							}}, 0, 10000);
					}
				}
			}
		});


		/*
		 * Update timers
		 * - Now playing timer should run if live streaming
		 */
		if (AudioService.isRunning != null && AudioService.isRunning && !AudioService.isLive){
			//Do not pull now playing live, since it will not be relevant
		} else {
			updateCurrentTimer = new Timer();
			updateCurrentTimer.scheduleAtFixedRate(new TimerTask(){
				@Override
				public void run() {
					setNowPlaying();
					//updateCurrentTimer.schedule(this, 1000);
				}}, 0, 10000);
		}


		/*
		 * Volume bar setup
		 */

		final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		final ImageView volumeIcon = (ImageView) findViewById(R.id.mainscreen_volume_icon);
		//audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
		SeekBar volumeBar = (SeekBar) findViewById(R.id.mainscreen_volume_control);
		volumeBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		volumeBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
		volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

				if (progress > 0 && progress < seekBar.getMax() / 2){
					volumeIcon.setImageResource(R.drawable.volume_1);
				} else if (progress == 0){
					volumeIcon.setImageResource(R.drawable.volume_0);
				} else {
					volumeIcon.setImageResource(R.drawable.volume_2);
				}
			}
		});


		/*
		 * Tab setup
		 */
		TabHost ourTabHost = getTabHost();//(TabHost) findViewById(android.R.id.tabhost);
		TabSpec playlistTab = ourTabHost.newTabSpec("Playlist");
		playlistTab.setIndicator("Playlist");
		playlistTab.setContent(R.id.mainscreen_playlistLayout);
		ourTabHost.addTab(playlistTab);

		TabSpec tweetsTab = ourTabHost.newTabSpec("Tweets");
		tweetsTab.setIndicator("Tweets");
		tweetsTab.setContent(R.id.mainscreen_twitterListLayout);
		ourTabHost.addTab(tweetsTab);

		TabSpec archiveTab = ourTabHost.newTabSpec("Archives");
		archiveTab.setIndicator("Archives");
		archiveTab.setContent(R.id.mainscreen_archivePickerLayout);
		ourTabHost.addTab(archiveTab);

		setupArchivePlayerView();
		
	}
	
	private void setupArchivePlayerView(){
		final ImageView playButton = (ImageView) findViewById(R.id.mainscreen_Button_play);
		
		final Spinner fromDay = (Spinner) findViewById(R.id.spinner_fromDay);
		final Spinner fromMonth = (Spinner) findViewById(R.id.spinner_fromMonth);
		final Spinner fromYear = (Spinner) findViewById(R.id.spinner_fromYear);
		final Spinner fromHour = (Spinner) findViewById(R.id.spinner_fromHour);

		final Spinner toDay = (Spinner) findViewById(R.id.spinner_toDay);
		final Spinner toMonth = (Spinner) findViewById(R.id.spinner_toMonth);
		final Spinner toYear = (Spinner) findViewById(R.id.spinner_toYear);
		final Spinner toHour = (Spinner) findViewById(R.id.spinner_toHour);

		GregorianCalendar today = new GregorianCalendar();
		fromDay.setSelection(today.get(Calendar.DAY_OF_MONTH) -1 );
		fromMonth.setSelection(today.get(Calendar.MONTH));
		toDay.setSelection(today.get(Calendar.DAY_OF_MONTH-1));
		toMonth.setSelection(today.get(Calendar.MONTH));

		Button playArchiveButton = (Button) findViewById(R.id.archive_playButton);

		playArchiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final GregorianCalendar fromDate = new GregorianCalendar(Integer.parseInt((String)fromYear.getSelectedItem()), fromMonth.getSelectedItemPosition(), fromDay.getSelectedItemPosition() + 1, fromHour.getSelectedItemPosition(), 0);
				final GregorianCalendar toDate = new GregorianCalendar(Integer.parseInt((String)toYear.getSelectedItem()), toMonth.getSelectedItemPosition(), toDay.getSelectedItemPosition() + 1, toHour.getSelectedItemPosition(), 0);
				GregorianCalendar today = new GregorianCalendar();
				if (toDate.before(fromDate)){
					//No
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									MainActivity.this);

							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("Start time can't be before the end time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is after todate (" + toDate.toString() + ")");
				} else if ((today.getTime().getTime() - fromDate.getTime().getTime()) > 1000 * 60 * 60 * 24 * 14) {
					//Over two weeks ago
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									MainActivity.this);
							alertDialogBuilder.setTitle("Sorry");
							alertDialogBuilder
							.setMessage("Due to legal reasons, we cannot keep archives over two weeks.")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is over two weeks ago");
				} else if (fromDate.compareTo(toDate) == 0) {
					//Over two weeks ago
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									MainActivity.this);
							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("Can't start and end at the same time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is over two weeks ago");
				} else if ((toDate.getTime().getTime() - fromDate.getTime().getTime()) > 1000 * 60 * 60 * 4) {
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") to (" + toDate.toString() + ") is more than four hours");
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									MainActivity.this);
							alertDialogBuilder.setTitle("Sorry");
							alertDialogBuilder
							.setMessage("Sorry, only four hours of archives can be queued at a time.")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
				} else {
					//Play dat shit
					Log.d("ARCHIVES", "Playinf from (" + fromDate.toString() + ") to (" + toDate.toString() + ")");
					if (updateCurrentTimer != null){
						updateCurrentTimer.cancel();
						updateCurrentTimer = null;
					}
					final String archiveURL = "http://wmfo-duke.orgs.tufts.edu/cgi-bin/castbotv2?" + 
							"s-year=" + fromDate.get(Calendar.YEAR) + 
							"&s-month=" + (fromDate.get(Calendar.MONTH) + 1) + 
							"&s-day=" + fromDate.get(Calendar.DAY_OF_MONTH) +
							"&s-hour=" + fromDate.get(Calendar.HOUR_OF_DAY) +
							"&e-year=" + toDate.get(Calendar.YEAR) +
							"&e-month=" + (toDate.get(Calendar.MONTH) + 1) +
							"&e-day=" + toDate.get(Calendar.DAY_OF_MONTH) +
							"&e-hour=" + toDate.get(Calendar.HOUR_OF_DAY) +
							"/archive.mp3";
					Log.d("ARCHIVES", "URL: " + archiveURL);

					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							if (AudioService.isRunning != null && AudioService.isRunning){
								stopService(new Intent(MainActivity.this, AudioService.class));
								playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
							} 
							Intent startIntent = new Intent(MainActivity.this, AudioService.class);
							startIntent.putExtra("source", archiveURL);
							startService(startIntent);
							playButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));

							TextView Track = (TextView) findViewById(R.id.mainscreen_Track);
							Track.setText("Playing archives");

							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

							TextView Artist = (TextView) findViewById(R.id.mainscreen_Artist);
							Artist.setText(sdf.format(fromDate.getTime()));

							TextView Album = (TextView) findViewById(R.id.mainscreen_Album);
							Album.setText(" to ");

							TextView DJ = (TextView) findViewById(R.id.mainscreen_DJ);
							DJ.setText(sdf.format(toDate.getTime()));

							TextView Show = (TextView) findViewById(R.id.mainscreen_Show);
							Show.setText("");

						}});
				}
			}
		});
	}

	private void noSaveStateSetup(){
		/*
		 * Fetch twitter data
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

	private void saveStateSetup(Bundle savedInstanceState){
		/*
		 * Restore now playing text
		 */

		if (savedInstanceState != null && savedInstanceState.containsKey("nowplaying_track")){
			TextView Track = (TextView) findViewById(R.id.mainscreen_Track);
			Track.setText(savedInstanceState.getString("nowplaying_track"));
		}
		if (savedInstanceState != null && savedInstanceState.containsKey("nowplaying_track")){
			TextView Artist = (TextView) findViewById(R.id.mainscreen_Artist);
			Artist.setText(savedInstanceState.getString("nowplaying_artist"));
		}
		if (savedInstanceState != null && savedInstanceState.containsKey("nowplaying_track")){
			TextView Album = (TextView) findViewById(R.id.mainscreen_Album);
			Album.setText(savedInstanceState.getString("nowplaying_album"));
		}
		if (savedInstanceState != null && savedInstanceState.containsKey("nowplaying_track")){
			TextView DJ = (TextView) findViewById(R.id.mainscreen_DJ);
			DJ.setText(savedInstanceState.getString("nowplaying_dj"));
		}
		if (savedInstanceState != null && savedInstanceState.containsKey("nowplaying_track")){
			TextView Show = (TextView) findViewById(R.id.mainscreen_Show);
			Show.setText(savedInstanceState.getString("nowplaying_show"));
		}

		/*
		 * Restore tweet data
		 */
		Log.d("TWEETS", "Loading from saved state");
		ListView twitterList = (ListView) findViewById(R.id.mainscreen_twitterListLayout);
		try {
			twitterJSON =new JSONArray(savedInstanceState.getString("tweets")); 
			twitterList.setAdapter(new TweetListViewAdapter(MainActivity.this, parseTwitterJSON(twitterJSON)));
		} catch (JSONException e) {
			e.printStackTrace();
		}

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
		String playlistXML = null;
		try {
			//SpinInfo = executeGET("http://wmfo-duke.orgs.tufts.edu:8000/7.html");
			SpinInfo = executeGET("http://spinitron.com/public/newestsong.php?station=wmfo");

			playlistXML = executeGET("http://spinitron.com/radio/rss.php?station=wmfo");
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		} catch (URISyntaxException e) {
		}

		if (SpinInfo != null && SpinInfo != ""){
			final SongInfo nowPlaying = new SongInfo(SpinInfo, true);
			final Playlist playlist = new Playlist(playlistXML);
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					ListView playLististView = (ListView) findViewById(R.id.mainscreen_playlistLayout);
					playLististView.setAdapter(new PlayListViewAdapter(MainActivity.this, playlist));

					TextView Track = (TextView) findViewById(R.id.mainscreen_Track);
					Track.setText(nowPlaying.title);

					TextView Artist = (TextView) findViewById(R.id.mainscreen_Artist);
					Artist.setText("By " + nowPlaying.artist);

					TextView Album = (TextView) findViewById(R.id.mainscreen_Album);
					Album.setText("From " + nowPlaying.album);

					TextView DJ = (TextView) findViewById(R.id.mainscreen_DJ);
					DJ.setText("Spun by " + nowPlaying.DJ);

					TextView Show = (TextView) findViewById(R.id.mainscreen_Show);
					Show.setText("On the show " + nowPlaying.showName);
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

