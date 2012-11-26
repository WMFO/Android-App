package com.tufts.wmfo;

import android.util.Log;

public class SongInfo {

	String title;
	String album;
	String artist;
	String spundate;
	String label;
	String DJ;
	String showName;

	int numListeners;
	int streamStatus;
	int peakListeners;
	int maxListeners;
	int uniqueListeners;
	int bitrate;
	String rawDetails;

	@Override
	public boolean equals(Object other){
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

}