package com.tufts.wmfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayListViewAdapter extends ArrayAdapter<SongInfo>{

	private final Context context;
	private final Playlist playlist;

	public PlayListViewAdapter(Context context, Playlist playlist) {
		super(context, R.layout.playlist_listview_layout, playlist.Songs);
		this.context = context;
		this.playlist = playlist;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;

		if (position == 0 && playlist != null && playlist.Songs != null && playlist.Songs.get(position).title != null){
			rowView = inflater.inflate(R.layout.playlist_nowplaying_layout, parent, false);
			TextView trackText = (TextView) rowView.findViewById(R.id.playlist_currentTrack);
			TextView artistText = (TextView) rowView.findViewById(R.id.playlist_currentArtist);
			TextView DJShow = (TextView) rowView.findViewById(R.id.playlist_showInfo);
			TextView ArtNotFound = (TextView) rowView.findViewById(R.id.playlist_currentArtworkNotFound);
			ImageView albumArt = (ImageView) rowView.findViewById(R.id.playlist_currentArtwork);
			
			trackText.setText(playlist.Songs.get(position).title);
			artistText.setText(playlist.Songs.get(position).artist + " - " + playlist.Songs.get(position).album);
			DJShow.setText("DJ " + playlist.Songs.get(position).DJ + " on " + playlist.Songs.get(position).showName);
			if (playlist.Songs.get(position).artwork_large != null){
				albumArt.setImageBitmap(playlist.Songs.get(position).artwork_large);
				ArtNotFound.setVisibility(View.INVISIBLE);
			} else if (playlist.Songs.get(position).artwork_medium != null){
				albumArt.setImageBitmap(playlist.Songs.get(position).artwork_medium);
				ArtNotFound.setVisibility(View.INVISIBLE);
			} else {
				albumArt.setImageResource(R.drawable.wmfo_icon_20);
				ArtNotFound.setVisibility(View.VISIBLE);
			}
			
		} else {
			rowView = inflater.inflate(R.layout.tweet_listview_layout, parent, false);
			TextView nameText = (TextView) rowView.findViewById(R.id.largetext);
			nameText.setText(playlist.Songs.get(position).rawDetails);
		}
		
		return rowView;
	}
}
