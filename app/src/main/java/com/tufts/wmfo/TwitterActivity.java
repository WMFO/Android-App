package com.tufts.wmfo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class TwitterActivity extends Activity {

	private JSONArray twitterJSON;
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
		super.onSaveInstanceState(savedInstanceState);
		if (twitterJSON != null){
			savedInstanceState.putString("tweets", twitterJSON.toString());
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_tweets);
		
		ListView twitterList = (ListView) findViewById(R.id.twitterActivity_ListView);
		
		twitterList.setEmptyView(findViewById(R.id.twitterActivity_EmptyView));
		
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
							TwitterActivity.this);

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
		
		if (savedInstanceState != null){
			SaveStateSetup(savedInstanceState);
		} else {
			SavedStateIndependentSetup();
		}
		
	}
	
	private void SavedStateIndependentSetup() {
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
						ListView twitterList = (ListView) findViewById(R.id.twitterActivity_ListView);
						twitterList.setAdapter(new TweetListViewAdapter(TwitterActivity.this, parseTwitterJSON(twitterJSON)));

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
	
	private void SaveStateSetup(Bundle savedInstanceState){

		/*
		 * Restore tweet data
		 */
		if (savedInstanceState.containsKey("tweets")){
			Log.d("TWEETS", "Loading from saved state");
			ListView twitterList = (ListView) findViewById(R.id.twitterActivity_ListView);
			try {
				twitterJSON =new JSONArray(savedInstanceState.getString("tweets")); 
				twitterList.setAdapter(new TweetListViewAdapter(TwitterActivity.this, parseTwitterJSON(twitterJSON)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
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
	
}
