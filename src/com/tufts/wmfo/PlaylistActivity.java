package com.tufts.wmfo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class PlaylistActivity extends Activity {

	private Timer updateCurrentTimer;
	private String playlistXML;
	private SongInfo nowPlaying = null;
	
	private final static String TAG = "PlaylistActivity";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_playlist);
		
		if (savedInstanceState != null){
			SaveStateSetup(savedInstanceState);
		}
		
		ListView playListView = (ListView) findViewById(R.id.playlistActivity_ListView);
		playListView.setEmptyView(findViewById(R.id.playlistActivity_EmptyView));
		
		/*
		 * Update timers
		 * - Now playing timer should run if live streaming
		 */
		updateCurrentTimer = new Timer();
		updateCurrentTimer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				setNowPlaying();
			}}, 0, 10000);
		
	}
	
	void SaveStateSetup(Bundle savedInstanceState){
		/*
		 * Restore now playing info
		 */

		if (savedInstanceState.containsKey("nowPlaying")){
			Log.d("SAVESTATESETUP", "Saved state has now playing; restoring.");
			try {
				this.nowPlaying = new SongInfo(new JSONObject(savedInstanceState.getString("nowPlaying")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		/*
		 * Restore playlist info
		 */

		String playlistXML = savedInstanceState.getString("playlistXML");
		if (playlistXML != null){
			ListView playListView = (ListView) findViewById(R.id.playlistActivity_ListView);
			playListView.setAdapter(new PlayListViewAdapter(PlaylistActivity.this, new Playlist(playlistXML)));
			((PlayListViewAdapter)playListView.getAdapter()).insert(nowPlaying, 0);
		}
	}
	
	void setNowPlaying(){
		final ListView playListView = (ListView) findViewById(R.id.playlistActivity_ListView);
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

			PlaylistActivity.this.playlistXML = playlistXML;

			final Playlist playlist = new Playlist(playlistXML);
			if (SpinInfo != null && SpinInfo != ""){
				nowPlaying = new SongInfo(SpinInfo, true);
				if (playListView.getAdapter() != null && playListView.getAdapter().getItem(0) != null && nowPlaying.equals(playListView.getAdapter().getItem(0))){
					Log.d(TAG, "Songs are identical, not fetching art");
					nowPlaying = (SongInfo) playListView.getAdapter().getItem(0);
				} else {
					JSONObject albumInfo = LastFM.getAlbumInfo(PlaylistActivity.this, nowPlaying.artist, nowPlaying.album);
					if (albumInfo.has("album")){
						try {
							albumInfo = albumInfo.getJSONObject("album");
							if (albumInfo.has("image")){
								JSONArray images = albumInfo.getJSONArray("image");
								nowPlaying.parseLastFMAlbumArt(images);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				playlist.addNowPlaying(nowPlaying);
			}
			runOnUiThread(new Runnable(){
				@Override
				public void run() {

					int playListIndex = 0;
					if (playListView.getAdapter() != null){
						playListIndex = playListView.getFirstVisiblePosition();
					}
					playListView.setAdapter(new PlayListViewAdapter(PlaylistActivity.this, playlist));
					playListView.setSelectionFromTop(playListIndex, 0);
				}
			});
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putString("playlistXML", playlistXML);

		if (nowPlaying != null){
			savedInstanceState.putString("nowPlaying", nowPlaying.toJSONObject().toString());
		}
	}
	
}
