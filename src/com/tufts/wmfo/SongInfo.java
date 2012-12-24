package com.tufts.wmfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

public class SongInfo {

	String title = null;
	String album = null;
	String artist = null;
	String spundate = null;
	String label = null;
	String DJ = null;
	String showName = null;
	String rawDetails = null;
	
	int numListeners;
	int streamStatus;
	int peakListeners;
	int maxListeners;
	int uniqueListeners;
	int bitrate;
	
	Bitmap artwork_medium = null;
	Bitmap artwork_large = null;
	
	@Override
	public boolean equals(Object other){
		if (other == null || this.artist == null || ((SongInfo) other).artist == null || this.title == null || ((SongInfo) other).title == null){
			return false;
		}
		if (((SongInfo) other).artist.equals(this.artist) && ((SongInfo) other).title.equals(this.title)){
			return true;
		}
		return false;
	}
	
	public SongInfo(String SpinapiInfo, boolean Spinitron){
		
		try {
			this.title = stripTags(getSpan("songpart", SpinapiInfo));
			this.title = replaceHTMLCodes(this.title);
		} catch (Exception e){
		}
		try {
			this.album = stripTags(getSpan("diskpart", SpinapiInfo));
			this.album = this.album.substring(5, this.album.length());
			this.album = replaceHTMLCodes(this.album);
		} catch (Exception e){
		}
		try {
			this.artist = stripTags(getSpan("artistpart", SpinapiInfo));
			this.artist = this.artist.substring(3, this.artist.length());
			this.artist = replaceHTMLCodes(this.artist);
		} catch (Exception e){
		}
		try {
			this.label = stripTags(getSpan("labelpart", SpinapiInfo));
			this.label = this.label.substring(1, this.label.length()-1);
			this.label = replaceHTMLCodes(this.label);
		} catch (Exception e){
		}
		try {
			this.DJ = stripTags(getSpan("djpart", SpinapiInfo));
			this.DJ = this.DJ.substring(3, this.DJ.length());
			this.DJ = replaceHTMLCodes(this.DJ);
		} catch (Exception e){
		}
		try {
			this.showName = stripTags(getSpan("showname", SpinapiInfo));
			this.showName = replaceHTMLCodes(this.showName);
		} catch (Exception e){
		}
	}

	private String replaceHTMLCodes(String input){
		return input.replaceAll("&amp;", "&");
	}

	private String getSpan(String spanName, String Data){
		try {
			return Data.substring(Data.indexOf("<span class=\"" + spanName + "\">"), Data.indexOf("</span>", Data.indexOf("<span class=\"" + spanName + "\">")));
		} catch (Exception e){
		}
		return "";
	}

	private String stripTags(String input){
		//Strip out HTML Tags
		while (input.indexOf('>') > 0){
			String toReplace = input.substring(input.indexOf('<'), input.indexOf('>') + 1);
			input = input.replace(toReplace, "");
		}
		return input;
	}

	public SongInfo(String ICYInfo){
		
		//5,1,25,32,5,256,Cleveland, Ohio by Granicus, from Granicus
		//1 - Number of listeners
		//2 - Stream status (1 means you're on the air, 0 means the source isn't there)
		//3 - Peak number of listeners for this server run
		//4 - Max number of simultaneous listeners the server is configured to allow
		//5 - The unique number of listeners, based on IP
		//6 - Current bitrate in kilobits
		//7 - The title. (Note, even if you have a comma in your title, it isn't escaped or anything.)

		//Strip out HTML Tags
		while (ICYInfo.indexOf('>') > 0){
			String toReplace = ICYInfo.substring(ICYInfo.indexOf('<'), ICYInfo.indexOf('>') + 1);
			ICYInfo = ICYInfo.replace(toReplace, "");
		}

		int index, lastIndex;

		//Number of listeners
		index = ICYInfo.indexOf(',');
		numListeners = Integer.parseInt(ICYInfo.substring(0, index));

		//Stream status
		lastIndex = index+1;
		index = ICYInfo.indexOf(',', lastIndex);
		streamStatus = Integer.parseInt(ICYInfo.substring(lastIndex, index));

		//Peak Listeners
		lastIndex = index+1;
		index = ICYInfo.indexOf(',', lastIndex);
		peakListeners = Integer.parseInt(ICYInfo.substring(lastIndex, index));

		//Max Listeners
		lastIndex = index+1;
		index = ICYInfo.indexOf(',', lastIndex);
		maxListeners = Integer.parseInt(ICYInfo.substring(lastIndex, index));

		//# Unique Listeners
		lastIndex = index+1;
		index = ICYInfo.indexOf(',', lastIndex);
		uniqueListeners = Integer.parseInt(ICYInfo.substring(lastIndex, index));

		//Bit rate
		lastIndex = index+1;
		index = ICYInfo.indexOf(',', lastIndex);
		bitrate = Integer.parseInt(ICYInfo.substring(lastIndex, index));

		//Raw details
		lastIndex = index+1;
		index = ICYInfo.indexOf(',', lastIndex);
		Log.d("SongInfo", "rawDetails: " + ICYInfo.substring(lastIndex, ICYInfo.length()));
		rawDetails = ICYInfo.substring(lastIndex, ICYInfo.length());

		//Song
		title = rawDetails.substring(0, rawDetails.indexOf(" by"));
		Log.d("Title", title);

		//Artist
		artist = rawDetails.substring(rawDetails.indexOf(" by") + 4, rawDetails.indexOf(", from"));
		Log.d("Artist", artist);

		//Album
		album = rawDetails.substring(rawDetails.indexOf(", from") + 7, rawDetails.length());
		Log.d("Album", album);

	}

	
	
	public SongInfo(Node item) {
		
		NodeList details = item.getChildNodes();
		this.rawDetails = null;
		for (int i=0; i < details.getLength(); i++){
			if (details.item(i).getNodeName().equals("description")){
				this.rawDetails = details.item(i).getTextContent();
			}
		}
		
	}

	public void parseLastFMAlbumArt(JSONArray images) {
		for (int i=0; i < images.length(); i++){
			JSONObject image;
			try {
				image = (JSONObject) images.get(i);
				Log.d("SONGINFO:ART", "Found " + image.getString("size") + " image");
				if (image.getString("size").equals("medium")){
					Log.d("SONGINFO:ART", "Attempting to fetch " + image.getString("#text"));
					this.artwork_medium = Network.downloadBitmap(image.getString("#text"));
				} else if (image.getString("size").equals("large")){
					this.artwork_large = Network.downloadBitmap(image.getString("#text"));
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
			}
			
		}
	}
	
	public JSONObject toJSONObject(){
		JSONObject ret = new JSONObject();
		try { ret.put("title", title); } catch (JSONException e) {}
		try { ret.put("album", album); } catch (JSONException e) {}
		try { ret.put("artist", artist); } catch (JSONException e) {}
		try { ret.put("spundate", spundate); } catch (JSONException e) {}
		try { ret.put("label", label); } catch (JSONException e) {}
		try { ret.put("DJ", DJ); } catch (JSONException e) {}
		try { ret.put("showName", showName); } catch (JSONException e) {}
		try { ret.put("rawDetails", rawDetails); } catch (JSONException e) {}
		
		try { ret.put("numListeners", numListeners); } catch (JSONException e) {}
		try { ret.put("streamStatus", streamStatus); } catch (JSONException e) {}
		try { ret.put("peakListeners", peakListeners); } catch (JSONException e) {}
		try { ret.put("maxListeners", maxListeners); } catch (JSONException e) {}
		try { ret.put("uniqueListeners", uniqueListeners); } catch (JSONException e) {}
		try { ret.put("bitrate", bitrate); } catch (JSONException e) {}
		
		if (artwork_medium != null){
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			artwork_medium.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] artwork_medium_bytes = stream.toByteArray();
			String artwork_medium_string = Base64.encodeToString(artwork_medium_bytes, Base64.DEFAULT);
			try { ret.put("artwork_medium", artwork_medium_string); } catch (JSONException e) {}
		}
		
		if (artwork_large != null){
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			artwork_large.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] artwork_medium_bytes = stream.toByteArray();
			String artwork_medium_string = Base64.encodeToString(artwork_medium_bytes, Base64.DEFAULT);
			try { ret.put("artwork_large", artwork_medium_string); } catch (JSONException e) {}
		}
		
		return ret;
	}
	
	public SongInfo(JSONObject SongJSON){
		try { this.title = SongJSON.getString("title"); } catch (JSONException e) {}
		try { this.album = SongJSON.getString("album"); } catch (JSONException e) {}
		try { this.artist = SongJSON.getString("artist"); } catch (JSONException e) {}
		try { this.spundate = SongJSON.getString("spundate"); } catch (JSONException e) {}
		try { this.label = SongJSON.getString("label"); } catch (JSONException e) {}
		try { this.DJ = SongJSON.getString("DJ"); } catch (JSONException e) {}
		try { this.showName = SongJSON.getString("showName"); } catch (JSONException e) {}
		try { this.rawDetails = SongJSON.getString("rawDetails"); } catch (JSONException e) {}
		
		try { this.numListeners = SongJSON.getInt("numListeners"); } catch (JSONException e) {}
		try { this.streamStatus = SongJSON.getInt("streamStatus"); } catch (JSONException e) {}
		try { this.peakListeners = SongJSON.getInt("peakListeners"); } catch (JSONException e) {}
		try { this.maxListeners = SongJSON.getInt("maxListeners"); } catch (JSONException e) {}
		try { this.uniqueListeners = SongJSON.getInt("uniqueListeners"); } catch (JSONException e) {}
		try { this.bitrate = SongJSON.getInt("bitrate"); } catch (JSONException e) {}

		if (SongJSON.has("artwork_medium")){
			byte[] artwork_medium_bytes;
			try {
				artwork_medium_bytes = Base64.decode(SongJSON.getString("artwork_medium"), Base64.DEFAULT);
				this.artwork_medium = BitmapFactory.decodeByteArray(artwork_medium_bytes, 0, artwork_medium_bytes.length);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		if (SongJSON.has("artwork_large")){
			byte[] artwork_large_bytes;
			try {
				artwork_large_bytes = Base64.decode(SongJSON.getString("artwork_large"), Base64.DEFAULT);
				this.artwork_large = BitmapFactory.decodeByteArray(artwork_large_bytes, 0, artwork_large_bytes.length);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
			
	}

}
