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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CalendarView.OnDateChangeListener;
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

	boolean isLive;
	String archiveStart;
	String archiveEnd;

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

		savedInstanceState.putBoolean("isLive", this.isLive);

	}

	@Override
	public void onResume(){
		super.onResume();
		//Make sure the volume bar is accurate
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				SeekBar volumeBar = (SeekBar) findViewById(R.id.mainscreen_volume_control);
				AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				if (volumeBar != null && audioManager != null ){
					volumeBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
				}
			}});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		saveStateIndependentSetup();

		if (savedInstanceState == null){
			noSaveStateSetup();
		} else {
			saveStateSetup(savedInstanceState);
		}

		postSaveStateDependentSetup();
		
	}


	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP ){ 
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					SeekBar volumeBar = (SeekBar) findViewById(R.id.mainscreen_volume_control);
					AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
					if (volumeBar != null && audioManager != null ){
						volumeBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
					}
				}});
		} 
		return super.onKeyDown(keyCode, event);
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
					startService(startIntent);
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));

					if (updateCurrentTimer == null){
						updateCurrentTimer = new Timer();
						updateCurrentTimer.scheduleAtFixedRate(new TimerTask(){
							@Override
							public void run() {
								setNowPlaying();
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
				}}, 0, 10000);
		}


		/*
		 * Volume bar setup
		 */

		final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		final ImageView volumeIcon = (ImageView) findViewById(R.id.mainscreen_volume_icon);
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
		TabHost ourTabHost = getTabHost();
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			Log.d("API", "Api version " + Build.VERSION.SDK_INT + ", loading calendar");
			setupArchivePlayerView();
		} else {
			Log.d("API", "Api version " + Build.VERSION.SDK_INT + ", loading spinners");
			setupArchivePlayerViewV8();
		}
		
	}

	private void postSaveStateDependentSetup(){
		/*
		 * Twitter link parsing
		 */
		ListView twitterList = (ListView) findViewById(R.id.mainscreen_twitterListLayout);
		twitterList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				TextView nameText = (TextView) arg1.findViewById(R.id.largetext);
				Log.d("TWEET:CLICKED", "Text: " + nameText.getText().toString());
				final List<String> URLs = extractUrls(nameText.getText().toString());
				if (URLs.size() > 0){
					Log.d("TWEET:CLICKED", "Found URL " + URLs.get(0));
					
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							MainActivity.this);

					alertDialogBuilder.setTitle("Open Link?");
					alertDialogBuilder
					.setMessage("Would you like to open this?\n" + URLs.get(0))
					.setCancelable(false)
					.setNegativeButton("No",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
						}
					})
					.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse(URLs.get(0)));
							startActivity(i);
						}
					});
					
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					
				}
			}});
	}
	
	@TargetApi(11)
	private void setupArchivePlayerView(){
		final ImageView playButton = (ImageView) findViewById(R.id.mainscreen_Button_play);
		final Spinner playFromHour = (Spinner) findViewById(R.id.spinner_playFromHour);
		final Spinner playDuration = (Spinner) findViewById(R.id.spinner_playDuration);

		Button playArchiveButton = (Button) findViewById(R.id.archive_playButton);

		final GregorianCalendar fromDate = new GregorianCalendar();
		final GregorianCalendar toDate = new GregorianCalendar();

		CalendarView myCalendar = (CalendarView) findViewById(R.id.archive_datePickerCalendar);
		myCalendar.setOnDateChangeListener(new OnDateChangeListener(){
			public void onSelectedDayChange(CalendarView view, int year, int month, int day){
				fromDate.set(year, month, day);
				toDate.set(year, month, day);
			}
		});

		playArchiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fromDate.set(GregorianCalendar.HOUR_OF_DAY, playFromHour.getSelectedItemPosition());
				fromDate.set(GregorianCalendar.MINUTE, 0);
				toDate.set(GregorianCalendar.HOUR_OF_DAY, playFromHour.getSelectedItemPosition());
				toDate.add(GregorianCalendar.HOUR_OF_DAY, playDuration.getSelectedItemPosition() + 1);
				toDate.set(GregorianCalendar.MINUTE, 0);

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
							.setMessage("End time can't be before the start time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is after todate (" + toDate.toString() + ")");
				} else if (toDate.after(today) || fromDate.after(today)) {
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									MainActivity.this);

							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("You cannot listen to the future!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}});
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
					Log.d("ARCHIVES", "Playing from (" + fromDate.toString() + ") to (" + toDate.toString() + ")");
					MainActivity.this.isLive = false;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					MainActivity.this.archiveStart = sdf.format(fromDate.getTime());
					MainActivity.this.archiveEnd = sdf.format(toDate.getTime());
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
						}});
				}
			}
		});

	}

	private void setupArchivePlayerViewV8(){
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
							.setMessage("End time can't be before the start time!")
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
					MainActivity.this.isLive = false;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					MainActivity.this.archiveStart = sdf.format(fromDate.getTime());
					MainActivity.this.archiveEnd = sdf.format(toDate.getTime());
					Log.d("ARCHIVES", "Playing from (" + fromDate.toString() + ") to (" + toDate.toString() + ")");
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

						}});
				}
			}
		});
	}


	private void noSaveStateSetup(){

		this.isLive = true;

		/*
		 * Fetch twitter data
		 */
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					String twitterJSONString = Network.getURL("https://api.twitter.com/1/statuses/user_timeline.json?include_entities=true&include_rts=true&screen_name=wmfo&count=30");
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

	public static List<String> extractUrls(String input) {
        List<String> result = new ArrayList<String>();

        Pattern pattern = Pattern.compile(
            "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" + 
            "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" + 
            "|mil|biz|info|mobi|name|aero|jobs|museum" + 
            "|travel|[a-z]{2}))(:[\\d]{1,5})?" + 
            "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" + 
            "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" + 
            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" + 
            "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" + 
            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" + 
            "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");

        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }
	
	private void saveStateSetup(Bundle savedInstanceState){

		if (savedInstanceState != null && savedInstanceState.containsKey("isLive")){
			this.isLive = savedInstanceState.getBoolean("isLive");
		}

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
		if (savedInstanceState.containsKey("tweets")){
			Log.d("TWEETS", "Loading from saved state");
			ListView twitterList = (ListView) findViewById(R.id.mainscreen_twitterListLayout);
			try {
				twitterJSON =new JSONArray(savedInstanceState.getString("tweets")); 
				twitterList.setAdapter(new TweetListViewAdapter(MainActivity.this, parseTwitterJSON(twitterJSON)));
				twitterList.setOnItemClickListener(new OnItemClickListener(){
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
						TextView nameText = (TextView) arg1.findViewById(R.id.largetext);
						Pattern patt = Pattern.compile(regex);
						Matcher matcher = patt.matcher(nameText.getText().toString());
						if (matcher.matches()){
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse(matcher.group()));
							startActivity(i);
						}
					}});
			} catch (JSONException e) {
				e.printStackTrace();
			}
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
		case R.id.menu_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	void setNowPlaying(){

		String SpinInfo = null;
		String playlistXML = null;
		try {
			SpinInfo = Network.getURL("http://spinitron.com/public/newestsong.php?station=wmfo");
			playlistXML = Network.getURL("http://spinitron.com/radio/rss.php?station=wmfo");
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		} catch (URISyntaxException e) {
		}
		if (playlistXML != null && !playlistXML.equals("")){
			final Playlist playlist = new Playlist(playlistXML);
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					ListView playListView = (ListView) findViewById(R.id.mainscreen_playlistLayout);
					int playListIndex = 0;
					if (playListView.getAdapter() != null){
						playListIndex = playListView.getFirstVisiblePosition();
					}
					playListView.setAdapter(new PlayListViewAdapter(MainActivity.this, playlist));
					playListView.setSelectionFromTop(playListIndex, 0);
					}
			});
		}
		if (SpinInfo != null && SpinInfo != ""){
			final SongInfo nowPlaying = new SongInfo(SpinInfo, true);
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					TextView Track = (TextView) findViewById(R.id.mainscreen_Track);
					TextView Artist = (TextView) findViewById(R.id.mainscreen_Artist);
					TextView Album = (TextView) findViewById(R.id.mainscreen_Album);
					TextView DJ = (TextView) findViewById(R.id.mainscreen_DJ);
					TextView Show = (TextView) findViewById(R.id.mainscreen_Show);

					if (isLive){
						Track.setText(nowPlaying.title);
						Artist.setText("By " + nowPlaying.artist);
						Album.setText("From " + nowPlaying.album);
						DJ.setText("Spun by " + nowPlaying.DJ);
						Show.setText("On the show " + nowPlaying.showName);
					} else {
						Track.setText("Playing archives");
						Artist.setText(archiveStart);
						Album.setText(" to ");
						DJ.setText(archiveEnd);
						Show.setText("");
					}
				}});
		}
	}

}

