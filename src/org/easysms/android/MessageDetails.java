package org.easysms.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.easysms.android.data.Conversation;
import org.easysms.android.data.SMS;
import org.easysms.android.ui.FlingAndScrollViewer;
import org.easysms.android.ui.FlowLayout;
import org.easysms.android.util.TextToSpeechManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(8)
public class MessageDetails extends Activity implements OnClickListener {

	// class for the GridView of quick sender
	public class ImageAdapter extends BaseAdapter {

		private Context mContext;

		// references to our images
		private Integer[] mThumbIds = { R.drawable.ok, R.drawable.no,
				R.drawable.late, R.drawable.callme, R.drawable.nobattery,
				R.drawable.busy, R.drawable.hurryup, R.drawable.howareu,
				R.drawable.loveu, R.drawable.driving, R.drawable.whatdate,
				R.drawable.whattime };

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return mThumbIds.length;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				v = li.inflate(R.layout.icon, null);
				TextView tv = (TextView) v.findViewById(R.id.icon_text);

				if (position == 0) {
					tv.setText("Ok");
				} else if (position == 1) {
					tv.setText("Non");
				} else if (position == 2) {
					tv.setText("Retard");
				} else if (position == 3) {
					tv.setText("Appel");
				} else if (position == 4) {
					tv.setText("Vide");
				} else if (position == 5) {
					tv.setText("Occupé");
				} else if (position == 6) {
					tv.setText("Vite!");
				} else if (position == 7) {
					tv.setText("Ça va?");
				} else if (position == 8) {
					tv.setText("Amour");
				} else if (position == 9) {
					tv.setText("Voiture");
				} else if (position == 10) {
					tv.setText("Date?");
				} else if (position == 11) {
					tv.setText("Heure?");
				} else {
					tv.setText("Putain");
				}

				ImageView iv = (ImageView) v.findViewById(R.id.icon_image);
				iv.setImageResource(mThumbIds[position]);

			} else {
				v = convertView;
			}
			return v;
		}
	}

	/**
	 * Handles the response of the broadcast request about the recognizer
	 * supported languages.
	 * 
	 * The receiver is required only if the application wants to do recognition
	 * in a specific language.
	 */
	private class SupportedLanguageBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.i(TAG, "Receiving broadcast " + intent);

			final Bundle extra = getResultExtras(false);

			if (getResultCode() != Activity.RESULT_OK) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						showToast("Error code:" + getResultCode());
						TextToSpeechManager.getInstance().setLocale(language);
						TextToSpeechManager.getInstance().say(
								"Error code:" + getResultCode());
					}
				});
			}

			if (extra == null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						showToast("No extra");
					}
				});
			}

			if (extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
					}
				});
			}

			if (extra.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
					}
				});
			}
		}

		private void showToast(String text) {
			// Toast.makeText(this, text, 1000).show();
		}
	}

	// for the pop up
	private static final int CUSTOM_DIALOG = 1;
	private static final int DATE_DIALOG_ID = 0;
	private static final String[] LABELS = new String[] { "Ok. ", "Non. ",
			"Désolé je suis en retard. ", "Appelle moi. ",
			"Désolé je n'ai plus de batterie. ",
			"Je ne peux pas répondre, je suis occupé. ", "Dépêche toi! ",
			"Ça va? ", "Je pense à toi. ",
			"Je ne peux pas répondre, je suis au volant. ", "Quelle date? ",
			"Quelle heure? " };
	private static final ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
	private static final String TAG = "VoiceRecognition";
	static final int TIME_DIALOG_ID = 1;
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	private LinearLayout bubblelayoutreceivedplay;
	private Conversation conversationtest = new Conversation();
	private ImageView deletemenu;
	private ImageView flaglanguage;
	// for the snapping scroll
	private FlingAndScrollViewer flingAndScrollViewer;
	private FlowLayout flowlayout;
	private FlowLayout flowlayoutreceived;

	private FlowLayout flowlayoutspeechrecog1;
	private FileOutputStream fOut = null;
	// handler for karaoke
	private Handler handler;
	private Locale language = Locale.FRENCH;
	private ArrayList<String> matches;
	// ---------date and time
	private TextView mDateDisplay;
	private int mDay;
	// Button btnSend;
	private String messageSent = "";
	private Handler mHandler;
	private int mhour;
	// for the fake menu bar
	private ImageView micromenu;
	private ListView mList;
	// pour le bip
	private MediaPlayer mMediaPlayer;
	private int mminute;
	private int mMonth;
	private ImageView mPickDate;
	private ImageView mPickTime;
	private LinearLayout msgbubblelayout;
	private LinearLayout msgdetailslayout;
	private LinearLayout msgreceived;
	private LinearLayout msgreceived1;
	private LinearLayout msgsent;
	private LinearLayout msgsent1;
	private Spinner mSupportedLanguageView;
	private TextView mTimeDisplay;
	private int mYear;
	private String nameContact = "Numéro inconnu";
	// to know if it is message details or new message
	private Boolean newMsg;
	private EasySmsApp ob;
	private MessageDetails objectTest;
	private OutputStreamWriter osw = null;
	private String phoneNo = "";
	// get info from the previous page
	private String phoneNumContact = "Nom inconnu";
	private int PICK_CONTACT;
	private ImageView profile;
	private TextView recipient;
	private TextView recipientnum;
	private ImageView sendmenu;
	private ImageView speakButton;
	private LinearLayout speechrecolayout;
	private int timesKaraoke = 0;
	private EditText txtMessage;

	// displays all the SMSs in a conversation.
	private void createLayoutbubbleconv(Conversation conv) {

		for (final SMS sms : conv.listsms) {

			LinearLayout wholelayout = new LinearLayout(this);

			LinearLayout.LayoutParams layoutParamsWhole = new LinearLayout.LayoutParams(
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 310, getResources()
									.getDisplayMetrics()),
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutParamsWhole.setMargins(0, (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
							.getDisplayMetrics()), 0, (int) TypedValue
					.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
							getResources().getDisplayMetrics()));
			wholelayout.setLayoutParams(layoutParamsWhole);
			wholelayout.setOrientation(LinearLayout.HORIZONTAL);
			// LINEAR LAYOUT
			LinearLayout linlayout = new LinearLayout(this);
			linlayout.setLayoutParams(new LayoutParams((int) TypedValue
					.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260,
							getResources().getDisplayMetrics()),
					LayoutParams.WRAP_CONTENT));
			// linlayout.setBackgroundResource(R.drawable.bubblelast);
			linlayout.setOrientation(LinearLayout.HORIZONTAL);
			if (sms.sent == "no") {
				linlayout.setGravity(Gravity.LEFT);
			} else if (sms.sent == "yes") {
				linlayout.setGravity(Gravity.RIGHT);
			}
			// FLOWLAYOUT
			final FlowLayout fl = new FlowLayout(this);
			fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
			fl.setBackgroundResource(R.drawable.bubblelast);

			Button dateButton = new Button(this);
			dateButton.setSingleLine(false);
			String textButton = sms.datesms + "\n" + sms.timesms;
			dateButton.setText(textButton);
			dateButton.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			dateButton.setBackgroundResource(R.drawable.datebutton);
			dateButton.setGravity(Gravity.RIGHT);
			String datetimesms = sms.datesms;
			if (language == Locale.FRENCH)
				datetimesms += "a";
			datetimesms += sms.timesms;
			final String datesmsplayed = textButton;
			dateButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					// plays the audio.
					TextToSpeechManager.getInstance().setLocale(language);
					TextToSpeechManager.getInstance().say(datesmsplayed);

					Date currentDate = new Date(System.currentTimeMillis());
					String date = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
					writeSettings(getApplicationContext(), "<action><date>"
							+ date + "</date>" + "<details>"
							+ "Play the date of a sms." + "</details></action>");

				}
			});
			fl.addView(dateButton);

			StringTokenizer st = new StringTokenizer(sms.body);
			String[] tabWords = new String[100];
			int nbWords = 0;
			while (st.hasMoreElements()) {
				tabWords[nbWords] = (String) st.nextElement();
				nbWords++;
			}
			// create a button for each words and append it to the bubble
			// composition
			for (int i = 0; i < nbWords; ++i) {
				final Button btn = new Button(this);
				btn.setText(tabWords[i]);
				// btn.setTextSize((int)
				// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,10,
				// getResources().getDisplayMetrics()));
				btn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT));
				btn.setBackgroundResource(R.drawable.button);
				// btn.setTextSize(16);

				final String toSay = tabWords[i];

				// play each button
				btn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						// plays the audio.
						TextToSpeechManager.getInstance().setLocale(language);
						TextToSpeechManager.getInstance().say(toSay);

						Date currentDate = new Date(System.currentTimeMillis());
						String date = (String) android.text.format.DateFormat
								.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
						writeSettings(getApplicationContext(), "<action><date>"
								+ date + "</date>" + "<details>"
								+ "Play the word of a sms: " + toSay
								+ "</details></action>");

					}
				});
				final Vibrator vibrationrecomp = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				// recomposition
				btn.setOnLongClickListener(new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						final Button bouton = new Button(getBaseContext());
						bouton.setText(btn.getText());
						// bouton.setTextSize((int)
						// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,10,
						// getResources().getDisplayMetrics()));
						bouton.setLayoutParams(new LayoutParams(
								LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT));
						bouton.setBackgroundResource(R.drawable.button);
						flowlayout.addView(bouton, new LayoutParams(
								LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT));
						vibrationrecomp.vibrate(200);

						Date currentDate = new Date(System.currentTimeMillis());
						String date = (String) android.text.format.DateFormat
								.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
						writeSettings(getApplicationContext(),
								"<action><date>" + date + "</date>"
										+ "<details>"
										+ "Recomposition. Reused the word: "
										+ bouton.getText()
										+ "</details></action>");

						// play each button
						bouton.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {

								// plays the audio.
								TextToSpeechManager.getInstance().setLocale(
										language);
								TextToSpeechManager.getInstance().say(toSay);

								Date currentDate = new Date(System
										.currentTimeMillis());
								String date = (String) android.text.format.DateFormat
										.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
												currentDate);
								writeSettings(
										getApplicationContext(),
										"<action><date>"
												+ date
												+ "</date>"
												+ "<details>"
												+ "Play the word of the sms in the bubble composition: "
												+ toSay + "</details></action>");

							}
						});

						final Vibrator vibrationdelete = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						// on long click, delete the button
						bouton.setOnLongClickListener(new OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								flowlayout.removeView(bouton);
								vibrationdelete.vibrate(200);

								Date currentDate = new Date(System
										.currentTimeMillis());
								String date = (String) android.text.format.DateFormat
										.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
												currentDate);
								writeSettings(
										getApplicationContext(),
										"<action><date>"
												+ date
												+ "</date>"
												+ "<details>"
												+ "Delete the word of the sms in the bubble composition: "
												+ bouton.getText()
												+ "</details></action>");

								return true;
							}
						});
						return true;
					}

				});

				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);

				layoutParams.setMargins((int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_SP, 50, getResources()
								.getDisplayMetrics()), (int) TypedValue
						.applyDimension(TypedValue.COMPLEX_UNIT_SP, 80,
								getResources().getDisplayMetrics()),
						(int) TypedValue.applyDimension(
								TypedValue.COMPLEX_UNIT_SP, 50, getResources()
										.getDisplayMetrics()), (int) TypedValue
								.applyDimension(TypedValue.COMPLEX_UNIT_SP, 80,
										getResources().getDisplayMetrics()));

				fl.addView(btn, layoutParams);

			}

			// PLAYBUTTON

			ImageView playButton = new ImageView(this);
			playButton.setBackgroundResource(R.drawable.playsmsclick);
			playButton.setOnCreateContextMenuListener(this);
			playButton.setOnCreateContextMenuListener(this);
			playButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					playKaraoke(fl);
				}

			});

			linlayout.addView(fl);
			if (sms.sent == "yes") {
				wholelayout.addView(playButton);
				wholelayout.addView(linlayout);

			} else {
				wholelayout.addView(linlayout);
				wholelayout.addView(playButton);
			}

			msgdetailslayout.addView(wholelayout);
		}

	}

	/*
	 * public void playSound(Context context) throws IllegalArgumentException,
	 * SecurityException, IllegalStateException,
	 * 
	 * IOException { // Uri soundUri =
	 * RingtoneManager.getDefaultUri(RingtoneManager.);
	 * 
	 * MediaPlayer mMediaPlayer = MediaPlayer.create(this, R.raw.beep);
	 * mMediaPlayer.setLooping(false); mm mMediaPlayer.setDataSource(context,
	 * soundUri);
	 * 
	 * final AudioManager audioManager = (AudioManager)
	 * context.getSystemService(Context.AUDIO_SERVICE);
	 * 
	 * if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
	 * 
	 * mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
	 * 
	 * mMediaPlayer.setLooping(true); mMediaPlayer.prepare();
	 * 
	 * mMediaPlayer.start(); } }
	 */

	private void CreateMenu(Menu menu) {

		// plays the audio.
		TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);
		TextToSpeechManager
				.getInstance()
				.say("Sélectionner la langue pour lire ou écrire un nouveau message.");

		menu.setQwertyMode(true);
		((ContextMenu) menu).setHeaderTitle("Sélectionner la langue");
		MenuItem mnu1 = menu.add(0, 0, 0, "Francais");
		{
			// mnu1.setAlphabeticShortcut('a');
			mnu1.setIcon(R.drawable.frenchflag);

		}
		MenuItem mnu2 = menu.add(0, 1, 1, "Anglais");
		{
			// mnu1.setAlphabeticShortcut('b');
			mnu2.setIcon(R.drawable.americanflag);

		}

		MenuItem mnu3 = menu.add(0, 2, 2, "Italien");
		{
			// mnu1.setAlphabeticShortcut('b');
			mnu3.setIcon(R.drawable.italianflag);

		}

		MenuItem mnu4 = menu.add(0, 2, 2, "Allemand");
		{
			// mnu1.setAlphabeticShortcut('b');
			mnu4.setIcon(R.drawable.germanflag);

		}

	}

	private String getContactNameFromNumber(String number) {
		/*
		 * We have a phone number and we want to grab the name of the contact
		 * with that number, if such a contact exists
		 */
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		/* phoneNumber here being a variable with the phone number stored. */
		Cursor c = getBaseContext().getContentResolver().query(lookupUri,
				new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		/*
		 * If we want to get something other than the displayed name for the
		 * contact, then just use something else instead of DISPLAY_NAME
		 */
		String name = "Contact inconnu";
		while (c.moveToNext()) {
			/* If we find a match we put it in a String. */
			name = c.getString(c
					.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
		}
		return name;

	}

	private String getContactPhotoFromNumber(String number) {
		/*
		 * We have a phone number and we want to grab the name of the contact
		 * with that number, if such a contact exists
		 */
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		/* phoneNumber here being a variable with the phone number stored. */
		Cursor c = getBaseContext().getContentResolver().query(lookupUri,
				new String[] { PhoneLookup.PHOTO_ID }, null, null, null);
		// long photo =
		// c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));
		/*
		 * If we want to get something other than the displayed name for the
		 * contact, then just use something else instead of DISPLAY_NAME
		 */
		String photoId = null;
		while (c.moveToNext()) {
			// if we find a match we put it in a String.
			photoId = c
					.getString(c.getColumnIndexOrThrow(PhoneLookup.PHOTO_ID));
		}
		return photoId;
	}

	public void helpVoiceRecog() {

		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				// plays the audio.
				TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);
				TextToSpeechManager.getInstance().say("Parlez après le bip.");

				try {
					Thread.sleep(1200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				handler.post(new Runnable() {
					@Override
					public void run() {

						playAudio();

					}
				});

			}
		};
		new Thread(runnable).start();

	}

	// to check if there is an Internet connection available
	public final boolean isInternetOn() {
		ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// ARE WE CONNECTED TO THE NET
		/*
		 * connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTING
		 * || connec.getNetworkInfo(1).getState() ==
		 * NetworkInfo.State.CONNECTING ||
		 */
		if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
				|| connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {
			// MESSAGE TO SCREEN FOR TESTING (IF REQ)
			// Toast.makeText(this, connec.getNetworkInfo(0) + "connected",
			// Toast.LENGTH_LONG).show();
			return true;
		} else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
				|| connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {
			// System.out.println(“Not Connected”);
			return false;
		}
		return false;
	}

	private boolean MenuChoice(MenuItem item) {
		String select = "Vous avez sélectionné";
		String langue = "";
		Date currentDate = new Date(System.currentTimeMillis());
		String date = (String) android.text.format.DateFormat.format(
				"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);

		// sets the locale.
		TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);

		switch (item.getItemId()) {
		case 0:
			language = Locale.FRENCH;
			langue = "français";
			flaglanguage.setBackgroundResource(R.drawable.frenchflag);

			// plays the audio.
			TextToSpeechManager.getInstance().say(select + langue);

			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>"
					+ "Change the language settings and set French."
					+ "</details></action>");

			return true;

		case 1:
			language = Locale.ENGLISH;
			langue = "anglais";
			flaglanguage.setBackgroundResource(R.drawable.americanflag);

			// plays the audio.
			TextToSpeechManager.getInstance().say(select + langue);

			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>"
					+ "Change the language settings and set English."
					+ "</details></action>");

			return true;

		case 2:
			language = Locale.ITALIAN;
			langue = "italien";
			flaglanguage.setBackgroundResource(R.drawable.italianflag);

			// plays the audio.
			TextToSpeechManager.getInstance().say(select + langue);

			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>"
					+ "Change the language settings and set Italian."
					+ "</details></action>");

			return true;

		case 3:

			language = Locale.GERMAN;
			langue = "allemand";
			flaglanguage.setBackgroundResource(R.drawable.germanflag);

			// plays the audio.
			TextToSpeechManager.getInstance().say(select + langue);

			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>"
					+ "Change the language settings and set German."
					+ "</details></action>");
			return true;

		}

		return false;
	}

	/**
	 * Handle the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {

			// in this case we scroll to the right page (if we are on quick
			// sender)
			flingAndScrollViewer.scrollBy(500, 10);

			// fill the list view with the strings the recognizer thought it
			// could have heard
			matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			final Vibrator vibrationadd = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			// fill the list view with the strings the recognizer thought it
			// could have heard

			speechrecolayout.removeAllViewsInLayout();

			for (int it = 0; it < 3; it++) {

				Date currentDate = new Date(System.currentTimeMillis());
				String date = (String) android.text.format.DateFormat.format(
						"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);

				LinearLayout wholelayout = new LinearLayout(this);
				LinearLayout.LayoutParams layoutParamsWhole = new LinearLayout.LayoutParams(
						(int) TypedValue.applyDimension(
								TypedValue.COMPLEX_UNIT_DIP, 310,
								getResources().getDisplayMetrics()),
						LinearLayout.LayoutParams.WRAP_CONTENT);
				layoutParamsWhole.setMargins(0, 5, 0, 5);
				wholelayout.setLayoutParams(layoutParamsWhole);
				wholelayout.setOrientation(LinearLayout.HORIZONTAL);
				// create a new flow layout for each choice
				final FlowLayout fl = new FlowLayout(this);// bubble
															// conversation
				LayoutParams layoutParams = new LayoutParams(
						(int) TypedValue.applyDimension(
								TypedValue.COMPLEX_UNIT_DIP, 260,
								getResources().getDisplayMetrics()),
						LayoutParams.WRAP_CONTENT);
				fl.setLayoutParams(layoutParams);
				fl.setBackgroundResource(R.drawable.bubblelast);

				// add the microphone
				// ImageView micro = new ImageView(this);
				// micro.setBackgroundResource(R.drawable.androidmic);
				// fl.addView(micro);
				// add the view number
				ImageView number = new ImageView(this);
				if (it == 0) {
					number.setBackgroundResource(R.drawable.one);
					fl.addView(number);

				} else if (it == 1) {
					number.setBackgroundResource(R.drawable.two);
					fl.addView(number);

				} else if (it == 2) {
					number.setBackgroundResource(R.drawable.three);
					fl.addView(number);

				}

				String str = matches.get(it).toString();
				// write the 3 voice recognition results

				writeSettings(getApplicationContext(), "<action><date>" + date
						+ "</date>" + "<details>" + "Voice recognition result "
						+ it + ": " + str + "</details></action>");

				StringTokenizer st = new StringTokenizer(str);
				String[] tabWords = new String[100];
				int nbWords = 0;
				while (st.hasMoreElements()) {
					tabWords[nbWords] = (String) st.nextElement();
					nbWords++;
				}
				// create a button for each words and append it to the bubble
				// composition
				for (int i = 0; i < nbWords; ++i) {
					final Button btn = new Button(this);
					btn.setText(tabWords[i]);
					// btn.setTextSize((int)
					// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,10,
					// getResources().getDisplayMetrics()));
					btn.setLayoutParams(new LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT));
					btn.setBackgroundResource(R.drawable.button);

					final String toSay = tabWords[i];
					// play each button
					btn.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							// String sentence = tabWords[i];
							// btn.setBackgroundColor(Color.RED);

							// plays the audio.
							TextToSpeechManager.getInstance().setLocale(
									language);
							TextToSpeechManager.getInstance().say(toSay);

							Date currentDate = new Date(System
									.currentTimeMillis());
							String date = (String) android.text.format.DateFormat
									.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
											currentDate);
							writeSettings(
									getApplicationContext(),
									"<action><date>"
											+ date
											+ "</date>"
											+ "<details>"
											+ "Play a word in the voice recognition results: "
											+ toSay + "</details></action>");

						}
					});
					final Vibrator vibrationdelete = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					// ici code
					btn.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {

							Date currentDate = new Date(System
									.currentTimeMillis());
							String date = (String) android.text.format.DateFormat
									.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
											currentDate);
							writeSettings(
									getApplicationContext(),
									"<action><date>"
											+ date
											+ "</date>"
											+ "<details>"
											+ "Add a word from the voice recognition result to the bubble composition."
											+ "</details></action>");

							final Button bouton = new Button(getBaseContext());
							bouton.setText(btn.getText());
							bouton.setLayoutParams(new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
							bouton.setBackgroundResource(R.drawable.button);

							// play each button
							bouton.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {

									// plays the audio.
									TextToSpeechManager.getInstance()
											.setLocale(language);
									TextToSpeechManager.getInstance()
											.say(toSay);

									Date currentDate = new Date(System
											.currentTimeMillis());
									String date = (String) android.text.format.DateFormat
											.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
													currentDate);
									writeSettings(
											getApplicationContext(),
											"<action><date>"
													+ date
													+ "</date>"
													+ "<details>"
													+ "Play a word in the bubble composition: "
													+ toSay
													+ "</details></action>");

								}
							});

							final Vibrator vibrationdelete = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
							// on long click, delete the button
							bouton.setOnLongClickListener(new OnLongClickListener() {
								@Override
								public boolean onLongClick(View v) {
									flowlayout.removeView(bouton);

									Date currentDate = new Date(System
											.currentTimeMillis());
									String date = (String) android.text.format.DateFormat
											.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
													currentDate);
									writeSettings(
											getApplicationContext(),
											"<action><date>"
													+ date
													+ "</date>"
													+ "<details>"
													+ "Remove button from bubble composition"
													+ bouton.getText()
													+ "</details></action>");

									vibrationdelete.vibrate(200);
									return true;
								}
							});
							// add the button to the flow layout
							flowlayout.addView(bouton, new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
							vibrationdelete.vibrate(200);

							return true;
						}
					});

					// ((MarginLayoutParams) layoutParams).setMargins(50, 80,
					// 50, 80);
					fl.addView(btn);

					vibrationadd.vibrate(200);
				} // end for each word

				wholelayout.addView(fl);
				speechrecolayout.addView(wholelayout);
				// play button
				ImageView playbutton = new ImageView(this);
				playbutton.setBackgroundResource(R.drawable.playsmsclick);
				playbutton.setOnCreateContextMenuListener(this);
				wholelayout.addView(playbutton);
				playbutton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						playKaraoke(fl);
					}

				});
				final Vibrator vibrationvoicerecog = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

				number.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						String sentenceChoosen = "";
						for (int i = 1; i < fl.getChildCount(); ++i) {
							// get the first word of the results
							Button btn = (Button) fl.getChildAt(i);
							// create a new word with the same characteristics
							final Button bouton = new Button(getBaseContext());
							bouton.setText(btn.getText());
							// bouton.setTextSize((int)
							// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,10,
							// getResources().getDisplayMetrics()));
							bouton.setLayoutParams(new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
							bouton.setBackgroundResource(R.drawable.button);
							// add the button to the flow layout
							flowlayout.addView(bouton, new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
							sentenceChoosen += btn.getText();

						}
						vibrationvoicerecog.vibrate(200);
						Date currentDate = new Date(System.currentTimeMillis());
						String date = (String) android.text.format.DateFormat
								.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
						writeSettings(
								getApplicationContext(),
								"<action><date>"
										+ date
										+ "</date>"
										+ "<details>"
										+ "Add one of the sentence choice from the voice recognition: "
										+ sentenceChoosen
										+ "</details></action>");

						return true;
					}

				});

				fl.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						String sentenceChoosen = "";
						for (int i = 1; i < fl.getChildCount(); ++i) {
							// get the first word of the results
							Button btn = (Button) fl.getChildAt(i);
							// create a new word with the same characteristics
							final Button bouton = new Button(getBaseContext());
							bouton.setText(btn.getText());
							// bouton.setTextSize((int)
							// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,10,
							// getResources().getDisplayMetrics()));
							bouton.setLayoutParams(new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
							bouton.setBackgroundResource(R.drawable.button);
							// add the button to the flow layout
							flowlayout.addView(bouton, new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
							sentenceChoosen += btn.getText();

						}
						vibrationvoicerecog.vibrate(200);
						Date currentDate = new Date(System.currentTimeMillis());
						String date = (String) android.text.format.DateFormat
								.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
						writeSettings(
								getApplicationContext(),
								"<action><date>"
										+ date
										+ "</date>"
										+ "<details>"
										+ "Add one of the sentence choice from the voice recognition: "
										+ sentenceChoosen
										+ "</details></action>");

						return true;
					}

				});

			}// end main for loop
		}

		else if (requestCode == PICK_CONTACT) {
			if (resultCode == Activity.RESULT_OK) {

				Date currentDate = new Date(System.currentTimeMillis());
				String date = (String) android.text.format.DateFormat.format(
						"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
				writeSettings(getApplicationContext(), "<action><date>" + date
						+ "</date>" + "<details>"
						+ "Succeded to pick a contact" + "</details></action>");

				phoneNo = "";
				Uri contactData = data.getData();
				Cursor cur = getContentResolver().query(contactData, null,
						null, null, null);
				ContentResolver contect_resolver = getContentResolver();
				String nameContact = "Nom inconnu";
				String photoId = null;
				if (cur.moveToFirst()) {
					String id = cur.getString(cur
							.getColumnIndexOrThrow(Phone._ID));
					// contact name
					nameContact = cur
							.getString(cur
									.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
					// photoID
					long photo = cur
							.getLong(cur
									.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));
					String no = "Numéro inconnu";
					// if the contact has a phone number
					if (Integer
							.parseInt(cur.getString(cur
									.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
						Cursor pCur = getContentResolver()
								.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
										null,
										ContactsContract.CommonDataKinds.Phone.CONTACT_ID
												+ " = ?", new String[] { id },
										null);
						// this second loop will retrieve all the contact
						// numbers for a particular contact id
						String mobilePhone = "Inconnu";
						String homePhone = "Inconnu";
						String workPhone = "Inconnu";
						String otherPhone = "Inconnu";

						if (pCur != null) {
							while (pCur.moveToNext()) {
								// takes only the MOBILE number
								if (pCur.getInt(pCur.getColumnIndex(Phone.TYPE)) == Phone.TYPE_MOBILE) {
									/*
									 * int phNumber =
									 * pCur.getColumnIndexOrThrow(
									 * ContactsContract
									 * .CommonDataKinds.Phone.NUMBER); no =
									 * pCur.getString(phNumber);
									 */
									switch (pCur.getInt(pCur
											.getColumnIndex(Phone.TYPE))) {
									case Phone.TYPE_MOBILE:
										mobilePhone = pCur.getString(pCur
												.getColumnIndex(Phone.NUMBER));
										break;
									case Phone.TYPE_HOME:
										homePhone = pCur.getString(pCur
												.getColumnIndex(Phone.NUMBER));
										break;
									case Phone.TYPE_WORK:
										workPhone = pCur.getString(pCur
												.getColumnIndex(Phone.NUMBER));
										break;
									case Phone.TYPE_OTHER:
										otherPhone = pCur.getString(pCur
												.getColumnIndex(Phone.NUMBER));
										break;
									}
								}
							}
							if (mobilePhone != "Inconnu") {
								no = mobilePhone;
							}
							/*
							 * else if (homePhone != "Inconnu") { no =
							 * homePhone; } else if (workPhone != "Inconnu") {
							 * no = workPhone; } else if (otherPhone !=
							 * "Inconnu") { no = otherPhone; }
							 */

						}
					}

					photoId = cur.getString(cur
							.getColumnIndex(Contacts.PHOTO_ID));
					if (photo != 0) {
						Cursor photo2 = getContentResolver().query(
								// column for the blob
								Data.CONTENT_URI, new String[] { Photo.PHOTO },
								Data._ID + "=?", // select row by id
								new String[] { photoId }, // filter by photoId
								null);

						if (photo2.moveToFirst()) {
							byte[] photoBlob = photo2.getBlob(photo2
									.getColumnIndex(Photo.PHOTO));
							Bitmap photoBitmap = BitmapFactory.decodeByteArray(
									photoBlob, 0, photoBlob.length);
							profile.setImageBitmap(photoBitmap);
						}
						photo2.close();
					} else {

						profile.setImageResource(R.drawable.nophotostored);
					}

					/*
					 * if (photo != 0) { Uri uri =
					 * ContentUris.withAppendedId(People.CONTENT_URI, photo);
					 * Bitmap bitmap = People.loadContactPhoto(getBaseContext(),
					 * uri, R.drawable.icon, null);
					 * profile.setImageBitmap(bitmap); }
					 */

					// reinitialise le textview
					recipient.setText("");
					recipientnum.setText("");
					// append new name selected
					recipient.append(nameContact);
					recipientnum.append(no);
					phoneNo += no;

					id = null;
					nameContact = null;
					no = null;

					contect_resolver = null;
					cur = null;

				}
			}
		} else {

			// plays the audio.
			TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);
			TextToSpeechManager.getInstance().say(
					"Il n'y a pas de connection internete.");

		}

		super.onActivityResult(requestCode, resultCode, data);

	}

	@Override
	public void onClick(View v) {

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		return MenuChoice(item);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// -------------two cases--------------------
		Bundle bundle = getIntent().getExtras();
		newMsg = bundle.getBoolean("NewMsg");
		if (newMsg) { // if new message don't display the message details page
			setContentView(R.layout.messagecomposition);
			Date currentDate = new Date(System.currentTimeMillis());
			String date = (String) android.text.format.DateFormat.format(
					"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>" + "Start writing a new message."
					+ "</details></action>");

			profile = (ImageView) findViewById(R.id.selectcontact);
			profile.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_PICK,
							ContactsContract.Contacts.CONTENT_URI);
					startActivityForResult(intent, PICK_CONTACT);

					Date currentDate = new Date(System.currentTimeMillis());
					String date = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
					writeSettings(getApplicationContext(), "<action><date>"
							+ date + "</date>" + "<details>" + "pick a contact"
							+ "</details></action>");

				}

			});
			recipient = (TextView) findViewById(R.id.contactname);
			recipientnum = (TextView) findViewById(R.id.contactnumber);

		} else {
			// if not a new message, display the message details page of "user"
			setContentView(R.layout.messagedetails);

			phoneNumContact = (String) bundle.get("Tel");
			nameContact = (String) bundle.get("Name");

			Date currentDate = new Date(System.currentTimeMillis());
			String date = (String) android.text.format.DateFormat.format(
					"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>" + "See message details of: "
					+ nameContact + "\t" + phoneNumContact
					+ "</details></action>");

			// if (phoneNumContact != "unknown" && nameContact != "unknown") {
			// Toast.makeText(getApplicationContext(),phoneNumContact,
			// Toast.LENGTH_LONG).show();
			phoneNo = phoneNumContact;
			msgdetailslayout = (LinearLayout) findViewById(R.id.msgdetailslayout);
			TextView contactnum = (TextView) findViewById(R.id.contactnumber);
			TextView contactname = (TextView) findViewById(R.id.contactname);
			contactnum.setText(phoneNumContact);
			contactname.setText(nameContact);
			String photoid = getContactPhotoFromNumber(phoneNumContact);
			profile = (ImageView) findViewById(R.id.selectcontact);
			if (photoid == null) {
				// temp2.put("avatar",R.drawable.unknowncontact);
				profile.setBackgroundResource(R.drawable.nophotostored);
			} else {

				Cursor photo2 = getContentResolver().query(Data.CONTENT_URI,
						new String[] { Photo.PHOTO }, // column for the blob
						Data._ID + "=?", // select row by id
						new String[] { photoid }, // filter by photoId
						null);
				Bitmap photoBitmap = null;
				if (photo2.moveToFirst()) {
					byte[] photoBlob = photo2.getBlob(photo2
							.getColumnIndex(Photo.PHOTO));
					photoBitmap = BitmapFactory.decodeByteArray(photoBlob, 0,
							photoBlob.length);
					profile.setImageBitmap(photoBitmap);

				}
				photo2.close();

			}

			// populateConversationTest(conversationtest);
			List<Conversation> listallconv = populateList(smsAllRetrieve());
			String threadidconv = retrieveThreadIdFromNumberContact(phoneNumContact);
			// Toast.makeText(getApplicationContext(),threadidconv,
			// Toast.LENGTH_LONG).show();
			Conversation conv = retrieveConvFromThreadId(listallconv,
					threadidconv);
			createLayoutbubbleconv(conv);
			// }

		} // end of else

		speechrecolayout = (LinearLayout) findViewById(R.id.elsalayout);
		LinearLayout wholeLayout = new LinearLayout(this);
		wholeLayout.setLayoutParams(new LayoutParams((int) TypedValue
				.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 310,
						getResources().getDisplayMetrics()),
				LayoutParams.WRAP_CONTENT));
		// for the speech to text

		flowlayoutspeechrecog1 = new FlowLayout(this);// bubble conversation
		flowlayoutspeechrecog1.setLayoutParams(new LayoutParams(
				(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
						260, getResources().getDisplayMetrics()),
				LayoutParams.WRAP_CONTENT));
		flowlayoutspeechrecog1.setBackgroundResource(R.drawable.bubblelast);
		// microphone button
		speakButton = new ImageView(this);
		speakButton.setBackgroundResource(R.drawable.androidmic);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		layoutParams.setMargins((int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
						.getDisplayMetrics()), (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
						.getDisplayMetrics()), 0, 0);
		flowlayoutspeechrecog1.addView(speakButton, layoutParams);

		// play button
		ImageView helpplay = new ImageView(this);
		helpplay.setBackgroundResource(R.drawable.playsmsclick);
		helpplay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playKaraoke(flowlayoutspeechrecog1);
			}
		});

		final String[] messageHelp = new String[] { "Pour", "enregistrer",
				"votre", "message", "cliquez", "sur", "le", "microphone", "en",
				"bas", "à", "gauche." };

		for (int j = 0; j < messageHelp.length; ++j) {
			final Button but = new Button(this);
			but.setText(messageHelp[j]);
			but.setBackgroundResource(R.drawable.button);
			but.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					// plays the audio.
					TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);
					TextToSpeechManager.getInstance().say(
							but.getText().toString());

					Date currentDate = new Date(System.currentTimeMillis());
					// Time currentTime = new Time();
					String date = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
					// currentTime.setToNow();
					writeSettings(getApplicationContext(), "<action><date>"
							+ date + "</date>" + "<details>"
							+ "play single word in voice recognition help"
							+ "</details></action>");

				}

			});
			flowlayoutspeechrecog1.addView(but);
		}

		LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		layoutParams2.setMargins((int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
						.getDisplayMetrics()), (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 10, getResources()
						.getDisplayMetrics()), 0, (int) TypedValue
				.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
						.getDisplayMetrics()));

		wholeLayout.addView(flowlayoutspeechrecog1);
		wholeLayout.addView(helpplay);
		speechrecolayout.addView(wholeLayout, layoutParams2);

		msgbubblelayout = (LinearLayout) findViewById(R.id.bubblelayout);

		// --------------for the snapping scroll--------------------------------
		flingAndScrollViewer = (FlingAndScrollViewer) findViewById(R.id.flingScrollViewer);
		flingAndScrollViewer.setInitialPosition(0); // set the initial
													// position<br />

		// -------------------quick sender-------------------
		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(new ImageAdapter(this));
		// conversation bubble

		flowlayout = new FlowLayout(this);// bubble conversation
		flowlayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		msgbubblelayout.setBackgroundResource(R.drawable.bubblelast);

		ImageView implaybutton = new ImageView(this);
		implaybutton.setBackgroundResource(R.drawable.playsmsclick);
		implaybutton.setOnCreateContextMenuListener(this);
		implaybutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playKaraoke(flowlayout);

			}

		});
		flowlayout.addView(implaybutton);

		final Vibrator vibrationadd = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				if (position < LABELS.length) {
					String sentence = LABELS[position];

					// plays the audio.
					TextToSpeechManager.getInstance().setLocale(language);
					TextToSpeechManager.getInstance().say(sentence);

					Date currentDate = new Date(System.currentTimeMillis());
					String date = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
					writeSettings(
							getApplicationContext(),
							"<action><date>"
									+ date
									+ "</date>"
									+ "<details>"
									+ "Single click on icon quick sender. Sentence played: "
									+ sentence + "</details></action>");

				}

			}
		});

		gridview.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View v,
					int position, long id) {
				if (position < LABELS.length) {

					Date currentDate = new Date(System.currentTimeMillis());
					String date = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
					writeSettings(
							getApplicationContext(),
							"<action><date>"
									+ date
									+ "</date>"
									+ "<details>"
									+ "Long click on icon quick sender. Sentence added to bubble: "
									+ LABELS[position] + "</details></action>");

					// parse the sentence into words and put it into an array of
					// words
					StringTokenizer st = new StringTokenizer(LABELS[position]);
					String[] tabWords = new String[100];
					int nbWords = 0;
					while (st.hasMoreElements()) {
						tabWords[nbWords] = (String) st.nextElement();
						nbWords++;
					}
					// create a button for each words and append it to the
					// bubble composition
					for (int i = 0; i < nbWords; ++i) {
						final Button btn = new Button(v.getContext());
						btn.setText(tabWords[i]);

						// btn.setTextSize( (int)
						// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
						// 10, getResources().getDisplayMetrics()));
						btn.setLayoutParams(new LayoutParams(
								LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT));
						// before being clicked the button is grey
						// btn.setBackgroundResource(R.drawable.buttonbeforeclicked);
						btn.setBackgroundResource(R.drawable.button);

						final String toSay = tabWords[i];

						// play each button
						btn.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
								// String sentence = tabWords[i];
								// btn.setBackgroundColor(Color.RED);

								// plays the audio.
								TextToSpeechManager.getInstance().setLocale(
										language);
								TextToSpeechManager.getInstance().say(toSay);

								Date currentDate = new Date(System
										.currentTimeMillis());
								String date = (String) android.text.format.DateFormat
										.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
												currentDate);
								writeSettings(
										getApplicationContext(),
										"<action><date>"
												+ date
												+ "</date>"
												+ "<details>"
												+ "Play one word of the bubble: "
												+ toSay + "</details></action>");

							}
						});
						final Vibrator vibrationdelete = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						// on long click, delete the button
						btn.setOnLongClickListener(new OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								flowlayout.removeView(btn);
								vibrationdelete.vibrate(200);
								Date currentDate = new Date(System
										.currentTimeMillis());
								String date = (String) android.text.format.DateFormat
										.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
												currentDate);
								writeSettings(
										getApplicationContext(),
										"<action><date>"
												+ date
												+ "</date>"
												+ "<details>"
												+ "Remove word from the bubble: "
												+ btn.getText()
												+ "</details></action>");

								return true;
							}
						});

						if (flowlayout.getChildCount() >= 16) {
							String sentence = "Message trop long";
							Toast.makeText(getBaseContext(), sentence,
									Toast.LENGTH_SHORT).show();

							// plays the audio.
							TextToSpeechManager.getInstance().setLocale(
									Locale.FRENCH);
							TextToSpeechManager.getInstance().say(sentence);

							Date currentDate2 = new Date(System
									.currentTimeMillis());
							String date2 = (String) android.text.format.DateFormat
									.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
											currentDate2);
							writeSettings(
									getApplicationContext(),
									"<action><date>"
											+ date2
											+ "</date>"
											+ "<details>"
											+ "Try to enter a message too long."
											+ "</details></action>");

						} else {
							flowlayout.addView(btn, new LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT));
						}

					}

					vibrationadd.vibrate(200);

				}

				return true;

			}// end of onitemclick
		});

		msgbubblelayout.addView(flowlayout, new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		// --------------------speech to text-------------------
		mHandler = new Handler();
		// Get display items for later interaction
		// ImageView speakButton = (ImageView) findViewById(R.id.btn_speak);

		mList = (ListView) findViewById(R.id.list);

		// Check to see if a recognition activity is present
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() != 0) {
			speakButton.setOnClickListener(this);
		} else {
			speakButton.setEnabled(false);
			// speakButton.setText("Recognizer not present");
		}

		// Most of the applications do not have to handle the voice settings. If
		// the application does not require a recognition in a specific language
		// (i.e., different from the system locale), the application does not
		// need to read the voice settings.
		// refreshVoiceSettings();

		// menu
		micromenu = (ImageView) findViewById(R.id.micro);
		sendmenu = (ImageView) findViewById(R.id.sendsms);
		deletemenu = (ImageView) findViewById(R.id.deletesms);

		micromenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Date currentDate = new Date(System.currentTimeMillis());
				String date = (String) android.text.format.DateFormat.format(
						"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
				writeSettings(
						getApplicationContext(),
						"<action><date>"
								+ date
								+ "</date>"
								+ "<details>"
								+ "Click on microphone icon on the menu to start speech recognition"
								+ "</details></action>");

				if (isInternetOn()) {
					// Do something long
					Runnable runnable = new Runnable() {
						@Override
						public void run() {

							// plays the audio.
							TextToSpeechManager.getInstance().setLocale(
									Locale.FRENCH);
							TextToSpeechManager.getInstance().say(
									"Parlez maintenant.");

							// helpVoiceRecog();
							try {
								Thread.sleep(800);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							handler.post(new Runnable() {
								@Override
								public void run() {

									startVoiceRecognitionActivity();

								}
							});

						}
					};
					new Thread(runnable).start();
				} else {

				}
				// }

			}// end onclick

		});
		// adds the context menu
		micromenu.setOnCreateContextMenuListener(this);

		sendmenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Date currentDate = new Date(System.currentTimeMillis());
				String date = (String) android.text.format.DateFormat.format(
						"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
				writeSettings(getApplicationContext(), "<action><date>" + date
						+ "</date>" + "<details>"
						+ "Click on send message icon in the menu. "
						+ "</details></action>");

				if (phoneNo.length() > 0 && flowlayout.getChildCount() > 1) {
					// retrieve SMS body
					String message = "";
					for (int i = 1; i < flowlayout.getChildCount(); ++i) {
						// message += flowlayout.getChildAt(i).getText();
						Button btn = (Button) flowlayout.getChildAt(i);
						message += btn.getText();
						message += " ";
					}
					// send SMS
					sendSMS(phoneNo, message);
					// insert SMS sent into DB
					final Calendar c = Calendar.getInstance();
					String dateToday = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd kk:mm", c.getTime());
					String threadid = retrieveThreadIdFromNumberContact(phoneNo);
					// right after the msg is sent, navigate to the message
					// details page
					Intent i = new Intent(MessageDetails.this,
							MessageDetails.class);
					String name = getContactNameFromNumber(phoneNo);
					Bundle bundle = new Bundle();
					// Add the parameters to bundle
					bundle.putString("Name", name);
					bundle.putString("Tel", phoneNo);
					bundle.putBoolean("NewMsg", false);
					// Add this bundle to the intent
					i.putExtras(bundle);
					startActivity(i);
					// insert SMS in the data base content provider
					ContentValues values = new ContentValues();
					values.put("address", phoneNo);
					// values.put("date",dateToday);
					values.put("read", 1);
					values.put("thread_id", threadid);
					values.put("body", message);
					getContentResolver().insert(
							Uri.parse("content://sms/sent"), values);

				}

				else if (flowlayout.getChildCount() <= 1) {
					Toast.makeText(getBaseContext(),
							"Entrez un message s'il vous plait.",
							Toast.LENGTH_SHORT).show();

					// plays the audio.
					TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);
					TextToSpeechManager.getInstance().say(
							"Entrez un message s'il vous plait.");

					Date currentDate1 = new Date(System.currentTimeMillis());
					String date1 = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate1);
					writeSettings(getApplicationContext(), "<action><date>"
							+ date1 + "</date>" + "<details>"
							+ "Try to send a sms without entering the content"
							+ "</details></action>");

				} else if (phoneNo.length() > 0) {
					Toast.makeText(getBaseContext(),
							"Entrez un numéro s'il vous plait.",
							Toast.LENGTH_SHORT).show();

					// plays the audio.
					TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);
					TextToSpeechManager.getInstance().say(
							"Entrez un message s'il vous plait.");

					Date currentDate2 = new Date(System.currentTimeMillis());
					String date2 = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate2);
					writeSettings(
							getApplicationContext(),
							"<action><date>"
									+ date2
									+ "</date>"
									+ "<details>"
									+ "Try to send a sms without entering the phone number."
									+ "</details></action>");

				}
			}
		});

		deletemenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Do something long
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						for (int i = 1; i < flowlayout.getChildCount(); ++i) {
							final Button btn = (Button) flowlayout
									.getChildAt(i);

							try {
								Thread.sleep(800);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							handler.post(new Runnable() {
								@Override
								public void run() {
									flowlayout.removeView(btn);
								}
							});
						}
					}
				};
				new Thread(runnable).start();

				Date currentDate = new Date(System.currentTimeMillis());
				String date = (String) android.text.format.DateFormat.format(
						"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
				writeSettings(getApplicationContext(), "<action><date>" + date
						+ "</date>" + "<details>"
						+ "Delete sms. (delete button in menu)"
						+ "</details></action>");

			}
		});

		// for the karaoke
		handler = new Handler();

		// for the language
		flaglanguage = (ImageView) findViewById(R.id.flaglanguage);
		flaglanguage.setBackgroundResource(R.drawable.frenchflag);
		flaglanguage.setOnCreateContextMenuListener(this);

	}// end of "onCreate"

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu((ContextMenu) menu, view, menuInfo);
		CreateMenu(menu);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	public void onUtteranceCompleted(String uttId) {
		if (uttId == "end of wakeup message ID") {
			// playAnnoyingMusic();

			// Toast.makeText(getApplicationContext(), "onutterance completed",
			// Toast.LENGTH_LONG);
		}
	}

	private void playAudio() {

		try {

			mMediaPlayer = MediaPlayer.create(this, R.raw.beep);
			mMediaPlayer.setLooping(false);
			Log.e("beep", "started0");
			mMediaPlayer.start();
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer arg0) {
					finish();
				}
			});
		} catch (Exception e) {
			Log.e("beep", "error: " + e.getMessage(), e);
		}
	}

	public void playKaraoke(final FlowLayout fl) {
		timesKaraoke++;

		if (timesKaraoke <= 1) {
			// KARAOKE

			// sets the locale.
			TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);

			// Do something long
			Runnable runnable = new Runnable() {
				@Override
				public void run() {

					for (int i = 1; i < fl.getChildCount(); ++i) {
						final Button btn = (Button) fl.getChildAt(i);
						// wholesentenceplayed += btn.getText();
						btn.setFocusableInTouchMode(true);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								// progress.setProgress(value);
								btn.requestFocus();
								// drop all pending entries in the playback
								// queue.

								// plays the audio.
								TextToSpeechManager.getInstance().say(
										(String) btn.getText());
							}
						});

					}
					timesKaraoke = 0;

				}
			};
			new Thread(runnable).start();

			String wholesentenceplayed = "";
			for (int i = 1; i < fl.getChildCount(); ++i) {
				final Button btn = (Button) fl.getChildAt(i);
				wholesentenceplayed += btn.getText();
			}

			Date currentDate = new Date(System.currentTimeMillis());
			String date = (String) android.text.format.DateFormat.format(
					"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
			writeSettings(getApplicationContext(), "<action><date>" + date
					+ "</date>" + "<details>" + "tap on play button karaoke"
					+ wholesentenceplayed + "</details></action>");

		}

	}

	private List<Conversation> populateList(List<SMS> allSMS) {

		// list with all the conversations
		List<Conversation> allconversations = new ArrayList<Conversation>();
		for (SMS smsnew : allSMS) {
			boolean add = false;
			for (Conversation conv : allconversations) {
				if (conv.threadid.equals(smsnew.threadid)) {
					conv.listsms.add(smsnew);
					add = true;
				}

			}
			if (add == false) { // we create a new conversation
				Conversation newconv = new Conversation();
				List<SMS> newlist = new ArrayList<SMS>();
				newlist.add(smsnew);
				newconv.listsms = newlist;
				newconv.threadid = smsnew.threadid;
				allconversations.add(newconv);
			}
		}
		return allconversations;
	}

	private void refreshVoiceSettings() {
		Log.i(TAG, "Sending broadcast");
		sendOrderedBroadcast(RecognizerIntent.getVoiceDetailsIntent(this),
				null, new SupportedLanguageBroadcastReceiver(), null,
				Activity.RESULT_OK, null, null);

	}

	public Conversation retrieveConvFromThreadId(List<Conversation> allConv,
			String threadid) {
		for (Conversation conv : allConv) {
			String convthreadid = conv.threadid;
			if (convthreadid.equals(threadid)) {
				return conv;
			}
		}

		return null;
	}

	public String retrieveThreadIdFromNumberContact(String phoneNumContact) {
		for (SMS sms : smsAllRetrieve()) {
			String smscontact = sms.contact;
			if (smscontact.equals(phoneNumContact))
				return sms.threadid;
		}
		return "error";
	}

	// ------------------sends a SMS message to another device---
	private void sendSMS(String phoneNumber, String message) {

		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
				SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
				new Intent(DELIVERED), 0);

		// ---when the SMS has been sent---
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK: // message sent

					Date currentDate = new Date(System.currentTimeMillis());
					String date = (String) android.text.format.DateFormat.format(
							"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate);
					writeSettings(getApplicationContext(), "<action><date>"
							+ date + "</date>" + "<details>" + "Message sent."
							+ "</details></action>");

					// vocal feedback when message sent
					String sentence = "Message envoyé";

					// plays the audio.
					TextToSpeechManager.getInstance().setLocale(language);
					TextToSpeechManager.getInstance().say(sentence);

					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(),
							"Erreur d'envoi du message", Toast.LENGTH_SHORT)
							.show();

					Date currentDate1 = new Date(System.currentTimeMillis());
					String date1 = (String) android.text.format.DateFormat.format(
							"yyyy-MM-dd'T'kk:mm:ss'Z'", currentDate1);
					writeSettings(getApplicationContext(), "<action><date>"
							+ date1 + "</date>" + "<details>"
							+ "Error in the sending of the message."
							+ "</details></action>");

					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "Erreur, pas de  service",
							Toast.LENGTH_SHORT).show();

					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off",
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		// ---when the SMS has been delivered---
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					/*
					 * Toast.makeText(getBaseContext(), "SMS delivered",
					 * Toast.LENGTH_SHORT).show();
					 */
					break;
				case Activity.RESULT_CANCELED:
					/*
					 * Toast.makeText(getBaseContext(), "SMS not delivered",
					 * Toast.LENGTH_SHORT).show();
					 */
					break;
				}
			}
		}, new IntentFilter(DELIVERED));

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
	}

	// for the page navigation
	public void setOb(EasySmsApp obA) {
		this.ob = obA;
	}

	// return a list with all the SMS and for each sms a status sent: yes or no
	public List<SMS> smsAllRetrieve() {

		// we put all the SMS sent and received in a list
		List<SMS> allSMSlocal = new ArrayList<SMS>();
		Uri uriSMSURIinbox = Uri.parse("content://sms/");
		Cursor curinbox = getContentResolver().query(uriSMSURIinbox, null,
				null, null, null);
		if (curinbox.moveToFirst()) {
			long datesms = 0;
			String phoneNumber = null;
			String body = null;
			String threadid;
			String datestring = "Date inconnue";
			String timestring = "Heure inconnue";
			Date dateFromSms = null;
			int type = -1;
			int read = -1;
			String dateTimeString = "erreur date";
			// return -1 if the column does not exist.
			int dateColumn = curinbox.getColumnIndex("date");
			int numberColumn = curinbox.getColumnIndex("address");
			int bodyColumn = curinbox.getColumnIndex("body");
			int threadColumn = curinbox.getColumnIndex("thread_id");
			int typeColumn = curinbox.getColumnIndex("type");
			int typeRead = curinbox.getColumnIndex("read");
			do {
				if (dateColumn != -1) {
					datesms = curinbox.getLong(dateColumn);
					dateTimeString = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd'T'kk:mm:ss'Z'", datesms);
					datestring = (String) android.text.format.DateFormat
							.format("yyyy-MM-dd", datesms);
					timestring = (String) android.text.format.DateFormat
							.format("kk:mm", datesms);
					// Toast.makeText(getApplicationContext(),datestring,
					// Toast.LENGTH_SHORT).show();
					dateFromSms = new Date(datesms);
					final Calendar c = Calendar.getInstance();
					int mYear = c.get(Calendar.YEAR);
					int mMonth = c.get(Calendar.MONTH) + 1;
					int mDay = c.get(Calendar.DAY_OF_MONTH);
					// String dateToday = mYear + "-" + mMonth + "-" + mDay;

				}
				if (typeRead != -1)
					read = curinbox.getInt(typeRead);
				if (typeColumn != -1)
					type = curinbox.getInt(typeColumn);
				if (bodyColumn != -1)
					body = curinbox.getString(bodyColumn);
				if (numberColumn != -1)
					phoneNumber = curinbox.getString(numberColumn);
				if (threadColumn != -1)
					threadid = curinbox.getString(threadColumn);
				else
					threadid = "nada";

				// we create a new SMS
				SMS smsnew = new SMS("unknown", threadid, datestring,
						timestring, phoneNumber, body, read);
				// to know if it is a message sent or received
				if (type == 2) { // SENT
					smsnew.sent = "yes";
				} else if (type == 1) { // INBOX
					smsnew.sent = "no";
				}
				if (read == 0) { // message is not read
					smsnew.read = 0;

				} else if (read == 1) { // message is read
					smsnew.read = 1;
				}
				// we add this SMS to the list of all the SMS
				allSMSlocal.add(smsnew);

			} while (curinbox.moveToNext());

		}

		return allSMSlocal;

	}

	/**
	 * Fire an intent to start the speech recognition activity.
	 */
	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

		// Specify the calling package to identify your application
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
				.getPackage().getName());

		// Display an hint to the user about what he should say.
		// intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
		// "Speech recognition demo");

		// Given an hint to the recognizer about what the user is going to say
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

		// Specify how many results you want to receive. The results will be
		// sorted
		// where the first result is the one with higher confidence.
		// intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

		// Specify the recognition language. This parameter has to be specified
		// only if the
		// recognition has to be done in a specific language and not the default
		// one (i.e., the
		// system locale). Most of the applications do not have to set this
		// parameter.
		/*
		 * if
		 * (!mSupportedLanguageView.getSelectedItem().toString().equals("Default"
		 * )) { intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
		 * mSupportedLanguageView.getSelectedItem().toString()); }
		 */

		// intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,language);
		// intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.FRENCH);
		if (language == Locale.FRENCH)
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
		else if (language == Locale.ITALIAN)
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
		else if (language == Locale.ENGLISH)
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-EN");

		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	public void writeSettings(Context context, String content) {
		String filename = "logdata.txt";
		String packageName = this.getPackageName();
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/Android/data/" + packageName + "/files/";
		try {
			boolean exists = (new File(path)).exists();
			if (!exists) {
				new File(path).mkdirs();
			}
			// Open output stream
			FileOutputStream fOut = new FileOutputStream(path + filename, true);
			// write integers as separated ascii's
			fOut.write((String.valueOf(content).toString() + " ").getBytes());
			// Toast.makeText(context,
			// "Settings saved",Toast.LENGTH_SHORT).show();
			// fOut.write((String.valueOf(content).toString() +
			// " ").getBytes());
			// Close output stream
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
			// Toast.makeText(context,
			// "Settings not saved",Toast.LENGTH_SHORT).show();
		}
	}
}
