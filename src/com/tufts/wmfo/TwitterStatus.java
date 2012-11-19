package com.tufts.wmfo;

import org.json.JSONException;
import org.json.JSONObject;

public class TwitterStatus  {

	String tweetText;
	String tweetDate;
	
	public TwitterStatus(JSONObject data) {
		try {
			this.tweetText = data.getString("text");
			this.tweetDate = data.getString("created_at").substring(0, 16);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
