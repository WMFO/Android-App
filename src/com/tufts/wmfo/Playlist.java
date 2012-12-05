package com.tufts.wmfo;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class Playlist {
	//http://spinitron.com/radio/rss.php?station=wmfo

	ArrayList<SongInfo> Songs;

	public Playlist(String RSS){
		Songs = new ArrayList<SongInfo>();
		if (RSS != null && !RSS.equals("")){
			Document playlistXML = getDomElement(RSS);
			NodeList entries = playlistXML.getElementsByTagName("item");
			for (int i=0; i < entries.getLength(); i++){
				Songs.add(new SongInfo(entries.item(i)));
			}
		}
	}

	public Document getDomElement(String xml){
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			doc = db.parse(is); 

		} catch (ParserConfigurationException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		} catch (SAXException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		}
		// return DOM
		return doc;
	}

}
