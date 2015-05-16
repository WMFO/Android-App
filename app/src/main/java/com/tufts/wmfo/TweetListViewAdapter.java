package com.tufts.wmfo;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TweetListViewAdapter extends ArrayAdapter<TwitterStatus>{

	private final Context context;
	private final ArrayList<TwitterStatus> values;

	public TweetListViewAdapter(Context context, ArrayList<TwitterStatus> values) {
		super(context, R.layout.tweet_listview_layout, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final View rowView = inflater.inflate(R.layout.tweet_listview_layout, parent, false);
		TextView nameText = (TextView) rowView.findViewById(R.id.largetext);
		nameText.setText(values.get(position).tweetText + " - " + values.get(position).tweetDate);

		return rowView;
	}
}
