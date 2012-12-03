package com.tufts.wmfo;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

		View rowView = inflater.inflate(R.layout.tweet_listview_layout, parent, false);
		TextView nameText = (TextView) rowView.findViewById(R.id.largetext);
		nameText.setText(playlist.Songs.get(position).rawDetails);

		return rowView;
	}
}
