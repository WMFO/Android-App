package com.tufts.wmfo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.CalendarView.OnDateChangeListener;

public class ArchiveActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_archives);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			Log.d("API", "Api version " + Build.VERSION.SDK_INT + ", loading calendar");
			setupArchivePlayerView();
		} else {
			Log.d("API", "Api version " + Build.VERSION.SDK_INT + ", loading spinners");
			setupArchivePlayerViewV8();
		}

		
	}
	
	@TargetApi(11)
	private void setupArchivePlayerView(){
		final Spinner playFromHour = (Spinner) findViewById(R.id.spinner_playFromHour);
		final Spinner playDuration = (Spinner) findViewById(R.id.spinner_playDuration);

		Button playArchiveButton = (Button) findViewById(R.id.archive_playButton);

		final GregorianCalendar fromDate = new GregorianCalendar();
		final GregorianCalendar toDate = new GregorianCalendar();

		CalendarView myCalendar = (CalendarView) findViewById(R.id.archive_datePickerCalendar);
		myCalendar.setOnDateChangeListener(new OnDateChangeListener(){
			public void onSelectedDayChange(CalendarView view, int year, int month, int day){
				fromDate.set(year, month, day);
				toDate.set(year, month, day);
			}
		});

		playArchiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fromDate.set(GregorianCalendar.HOUR_OF_DAY, playFromHour.getSelectedItemPosition());
				fromDate.set(GregorianCalendar.MINUTE, 0);
				toDate.set(GregorianCalendar.HOUR_OF_DAY, playFromHour.getSelectedItemPosition());
				toDate.add(GregorianCalendar.HOUR_OF_DAY, playDuration.getSelectedItemPosition() + 1);
				toDate.set(GregorianCalendar.MINUTE, 0);

				GregorianCalendar today = new GregorianCalendar();
				if (toDate.before(fromDate)){
					//No
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);

							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("End time can't be before the start time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is after todate (" + toDate.toString() + ")");
				} else if (toDate.after(today) || fromDate.after(today)) {
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);

							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("You cannot listen to the future!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}});
				} else if ((today.getTime().getTime() - fromDate.getTime().getTime()) > 1000 * 60 * 60 * 24 * 14) {
					//Over two weeks ago
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);
							alertDialogBuilder.setTitle("Sorry");
							alertDialogBuilder
							.setMessage("Due to legal reasons, we cannot keep archives over two weeks.")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is over two weeks ago");
				} else if (fromDate.compareTo(toDate) == 0) {
					//Over two weeks ago
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);
							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("Can't start and end at the same time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is over two weeks ago");
				} else if ((toDate.getTime().getTime() - fromDate.getTime().getTime()) > 1000 * 60 * 60 * 4) {
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") to (" + toDate.toString() + ") is more than four hours");
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);
							alertDialogBuilder.setTitle("Sorry");
							alertDialogBuilder
							.setMessage("Sorry, only four hours of archives can be queued at a time.")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
				} else {
					//Play dat shit
					Log.d("ARCHIVES", "Playing from (" + fromDate.toString() + ") to (" + toDate.toString() + ")");
					final String archiveURL = "http://wmfo-duke.orgs.tufts.edu/cgi-bin/castbotv2?" + 
							"s-year=" + fromDate.get(Calendar.YEAR) + 
							"&s-month=" + (fromDate.get(Calendar.MONTH) + 1) + 
							"&s-day=" + fromDate.get(Calendar.DAY_OF_MONTH) +
							"&s-hour=" + fromDate.get(Calendar.HOUR_OF_DAY) +
							"&e-year=" + toDate.get(Calendar.YEAR) +
							"&e-month=" + (toDate.get(Calendar.MONTH) + 1) +
							"&e-day=" + toDate.get(Calendar.DAY_OF_MONTH) +
							"&e-hour=" + toDate.get(Calendar.HOUR_OF_DAY) +
							"/archive.mp3";
					Log.d("ARCHIVES", "URL: " + archiveURL);

					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							if (AudioService.isRunning != null && AudioService.isRunning){
								stopService(new Intent(ArchiveActivity.this, AudioService.class));
							} 
							Intent startIntent = new Intent(ArchiveActivity.this, AudioService.class);
							startIntent.putExtra("source", archiveURL);
							startService(startIntent);
						}});
				}
			}
		});

	}

	private void setupArchivePlayerViewV8(){
		final Spinner fromDay = (Spinner) findViewById(R.id.spinner_fromDay);
		final Spinner fromMonth = (Spinner) findViewById(R.id.spinner_fromMonth);
		final Spinner fromYear = (Spinner) findViewById(R.id.spinner_fromYear);
		final Spinner fromHour = (Spinner) findViewById(R.id.spinner_fromHour);

		final Spinner toDay = (Spinner) findViewById(R.id.spinner_toDay);
		final Spinner toMonth = (Spinner) findViewById(R.id.spinner_toMonth);
		final Spinner toYear = (Spinner) findViewById(R.id.spinner_toYear);
		final Spinner toHour = (Spinner) findViewById(R.id.spinner_toHour);

		GregorianCalendar today = new GregorianCalendar();
		fromDay.setSelection(today.get(Calendar.DAY_OF_MONTH) -1 );
		fromMonth.setSelection(today.get(Calendar.MONTH));
		toDay.setSelection(today.get(Calendar.DAY_OF_MONTH-1));
		toMonth.setSelection(today.get(Calendar.MONTH));

		Button playArchiveButton = (Button) findViewById(R.id.archive_playButton);

		playArchiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final GregorianCalendar fromDate = new GregorianCalendar(Integer.parseInt((String)fromYear.getSelectedItem()), fromMonth.getSelectedItemPosition(), fromDay.getSelectedItemPosition() + 1, fromHour.getSelectedItemPosition(), 0);
				final GregorianCalendar toDate = new GregorianCalendar(Integer.parseInt((String)toYear.getSelectedItem()), toMonth.getSelectedItemPosition(), toDay.getSelectedItemPosition() + 1, toHour.getSelectedItemPosition(), 0);
				GregorianCalendar today = new GregorianCalendar();
				if (toDate.before(fromDate)){
					//No
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);

							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("End time can't be before the start time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is after todate (" + toDate.toString() + ")");
				} else if ((today.getTime().getTime() - fromDate.getTime().getTime()) > 1000 * 60 * 60 * 24 * 14) {
					//Over two weeks ago
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);
							alertDialogBuilder.setTitle("Sorry");
							alertDialogBuilder
							.setMessage("Due to legal reasons, we cannot keep archives over two weeks.")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is over two weeks ago");
				} else if (fromDate.compareTo(toDate) == 0) {
					//Over two weeks ago
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);
							alertDialogBuilder.setTitle("Oops");
							alertDialogBuilder
							.setMessage("Can't start and end at the same time!")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") is over two weeks ago");
				} else if ((toDate.getTime().getTime() - fromDate.getTime().getTime()) > 1000 * 60 * 60 * 4) {
					Log.d("ARCHIVES", "Fromdate (" + fromDate.toString() + ") to (" + toDate.toString() + ") is more than four hours");
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
									ArchiveActivity.this);
							alertDialogBuilder.setTitle("Sorry");
							alertDialogBuilder
							.setMessage("Sorry, only four hours of archives can be queued at a time.")
							.setCancelable(false)
							.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();

						}});
				} else {
					//Play dat shit
					Log.d("ARCHIVES", "Playing from (" + fromDate.toString() + ") to (" + toDate.toString() + ")");
					final String archiveURL = "http://wmfo-duke.orgs.tufts.edu/cgi-bin/castbotv2?" + 
							"s-year=" + fromDate.get(Calendar.YEAR) + 
							"&s-month=" + (fromDate.get(Calendar.MONTH) + 1) + 
							"&s-day=" + fromDate.get(Calendar.DAY_OF_MONTH) +
							"&s-hour=" + fromDate.get(Calendar.HOUR_OF_DAY) +
							"&e-year=" + toDate.get(Calendar.YEAR) +
							"&e-month=" + (toDate.get(Calendar.MONTH) + 1) +
							"&e-day=" + toDate.get(Calendar.DAY_OF_MONTH) +
							"&e-hour=" + toDate.get(Calendar.HOUR_OF_DAY) +
							"/archive.mp3";
					Log.d("ARCHIVES", "URL: " + archiveURL);

					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							if (AudioService.isRunning != null && AudioService.isRunning){
								stopService(new Intent(ArchiveActivity.this, AudioService.class));
							} 
							Intent startIntent = new Intent(ArchiveActivity.this, AudioService.class);
							startIntent.putExtra("source", archiveURL);
							startService(startIntent);
						}});
				}
			}
		});
	}
	
}
