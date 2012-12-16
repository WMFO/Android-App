package com.tufts.wmfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class LastFMNowPlayingRequest {

	Context appContext;
	SongInfo songInfo;
	SharedPreferences appPreferences;
	public LastFMNowPlayingRequest(Context context, SongInfo songInfo){
		this.appContext = context;
		this.songInfo = songInfo;
		appPreferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
	}

	public void send(){
		if (appPreferences.getString("setting_LastFM_Session_Key", null) == null){
			Log.w("WMFO:LASTFM", "Tried to send now playing notification wihtout auth");
			return;
		}
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("artist", this.songInfo.artist);
		params.put("track", this.songInfo.title);
		params.put("api_key", Auth.LAST_FM_API_KEY);
		params.put("sk", appPreferences.getString("setting_LastFM_Session_Key", null));
		String sig = LastFM.createSignature("track.updateNowPlaying", params, Auth.LAST_FM_API_SECRET);
		
		List<NameValuePair> nameValuePairs = LastFM.hashToNameValuePair(params);
		nameValuePairs.add(new BasicNameValuePair("method", "track.updateNowPlaying"));
		nameValuePairs.add(new BasicNameValuePair("api_sig", sig));
		
		String result = null;
		try {
			result = Network.postData(this.appContext.getResources().getString(R.string.LAST_FM_API_URL_SECURE), nameValuePairs);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
