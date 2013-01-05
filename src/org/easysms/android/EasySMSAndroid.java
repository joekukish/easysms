package org.easysms.android;

import java.io.OutputStreamWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

@TargetApi(8)
public class EasySMSAndroid extends ListActivity implements
		TextToSpeech.OnInitListener {

	// list of hash map for the message threads
	private static final ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
	// handler for karaoke
	private Handler handler;
	protected Boolean isComplete = false;
	private TextToSpeech mTts;
	private OutputStreamWriter osw = null;
	private ImageView profile;
	protected Handler taskHandler = new Handler();
	private LinearLayout threadpage;

	public void displayListSMS() {

		smsAllRetrieve();
		final ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent i = new Intent(EasySMSAndroid.this, MessageDetails.class);

				Object selectedFromList = (lv.getItemAtPosition(position));
				HashMap<String, Object> o = (HashMap<String, Object>) list
						.get(position);
				String telnum = "unknown";
				String name = "unknown";
				Object telnumobj = o.get("telnumber");
				if (telnumobj != null)
					telnum = telnumobj.toString();
				Object nameobj = o.get("name");
				if (nameobj != null)
					name = nameobj.toString();
				// Toast.makeText(getApplicationContext(),"name"+name,
				// Toast.LENGTH_SHORT).show();
				// Date currentDate = new Date(System.currentTimeMillis());
				// String date = (String)
				// android.text.format.DateFormat.format("yyyy-MM-dd'T'kk:mm:ss'Z'",
				// currentDate);

				// Next create the bundle and initialize it
				Bundle bundle = new Bundle();
				// Add the parameters to bundle
				bundle.putString("Name", name);
				bundle.putString("Tel", telnum);
				bundle.putBoolean("NewMsg", false);
				// Add this bundle to the intent
				i.putExtras(bundle);
				startActivity(i);

			}
		});
		final SimpleAdapter adapter = new SimpleAdapter(this, list,
				R.layout.custom_row_view, new String[] { "avatar", "telnumber",
						"date", "name", "message", "sent" }, new int[] {
						R.id.contact_image, R.id.text1, R.id.text2, R.id.text3,
						R.id.text4, R.id.isent });

		setListAdapter(adapter);
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
			// if we find a match we put it in a String.
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

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.custom_list_view);
		// text to speech
		mTts = new TextToSpeech(this, this);
		// for the karaoke
		handler = new Handler();
		profile = (ImageView) findViewById(R.id.contact_image);

		// -------------------New Message button------------------------
		Button b = (Button) findViewById(R.id.btnClick);
		b.setOnClickListener(new View.OnClickListener() {
			// on click on "create message" button, we navigate to the other
			// page!
			public void onClick(View arg0) {
				// here i call new screen;
				Intent i = new Intent(EasySMSAndroid.this, MessageDetails.class);
				// Next create the bundle and initialize it
				Bundle bundle = new Bundle();
				// Add the parameters to bundle as
				bundle.putBoolean("NewMsg", true);
				// Add this bundle to the intent
				i.putExtras(bundle);
				startActivity(i);
			}
		});
		b.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				String sentence = "ツrire un nouveau message";
				mTts.setLanguage(Locale.FRENCH);
				// Drop all pending entries in the playback queue.
				mTts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
				return true;
			}
		});

		threadpage = (LinearLayout) findViewById(R.id.threadpage);

		displayListSMS();

		// le timer fait ramer toute l'application!!! trouver un autre moyen
		// ==> retrieve a signal when a new msg is received.
		// -------------------timer------------------------
		final long elapse = 10000;
		Runnable t = new Runnable() {
			public void run() {
				// Toast.makeText(getApplicationContext(), "dans timer",
				// Toast.LENGTH_SHORT).show();
				list.clear();
				displayListSMS();
				if (!isComplete) {
					taskHandler.postDelayed(this, elapse);
				}
			}
		};
		taskHandler.postDelayed(t, elapse);

	}

	public void playKaraoke(final FlowLayout fl) {
		// KARAOKE
		mTts.setLanguage(Locale.FRENCH);
		// do something long
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				for (int i = 1; i < fl.getChildCount(); ++i) {
					final Button btn = (Button) fl.getChildAt(i);
					btn.setFocusableInTouchMode(true);
					// mTts.speak((String) btn.getText(),
					// TextToSpeech.QUEUE_ADD, myHashAlarm);
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					handler.post(new Runnable() {
						@Override
						public void run() {
							// progress.setProgress(value);
							btn.requestFocus();
							// drop all pending entries in the playback queue.
							mTts.speak((String) btn.getText(),
									TextToSpeech.QUEUE_FLUSH, null);
							// mTts.speak((String) btn.getText(),
							// TextToSpeech.QUEUE_ADD, myHashAlarm);
						}
					});
				}
			}
		};
		new Thread(runnable).start();

	}

	private List<Conversation> populateList(List<SMS> allSMS) {

		// list with all the conversations
		List<Conversation> allconversations = new ArrayList<Conversation>();
		for (SMS smsnew : allSMS) {
			boolean add = false;
			// case where the SMS is a draft, otherwise the pp bugs.
			if (smsnew.contact != null) {
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
		}

		for (Conversation conv : allconversations) {
			HashMap<String, Object> temp2 = new HashMap<String, Object>();
			// on regarde le 1er sms de chaque liste

			SMS firstsms = conv.listsms.get(0);
			// get name associated to phone number
			String name = getContactNameFromNumber(firstsms.contact);
			String photoid = getContactPhotoFromNumber(firstsms.contact);
			if (photoid == null) {
				temp2.put("avatar", R.drawable.nophotostored);

			} else {

				Cursor photo2 = managedQuery(Data.CONTENT_URI,
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

					if (photoBitmap != null) {
						temp2.put("avatar", R.drawable.nophotostored);
					} else {
						temp2.put("avatar", R.drawable.nophotostored);
					}

				}
				photo2.close();

			}
			temp2.put("telnumber", firstsms.contact);

			final Calendar c = Calendar.getInstance();
			// int date = Calendar.DATE;
			int mYear = c.get(Calendar.YEAR);
			int mMonth = c.get(Calendar.MONTH) + 1;
			int mDay = c.get(Calendar.DAY_OF_MONTH);
			// String dateToday = mYear + "-" + mMonth + "-" + mDay;
			String dateToday = mDay + "-" + mMonth + "-" + mYear;
			String datesms = firstsms.datesms;
			if (dateToday.equals(datesms)) {
				temp2.put("date", firstsms.timesms);
			} else {
				temp2.put("date", firstsms.datesms);
			}
			temp2.put("name", name);
			temp2.put("message", firstsms.body);
			if (firstsms.sent == "yes") {
				temp2.put("sent", R.drawable.sent);
			} else if (firstsms.sent == "no") {
				temp2.put("sent", R.drawable.received);
			}

			list.add(temp2);
		}

		return allconversations;
	}

	// return a list with all the SMS and for each sms a status sent: yes or no
	public void smsAllRetrieve() {
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
					// datestring = (String)
					// android.text.format.DateFormat.format("yyyy-MM-dd",datesms);
					datestring = (String) android.text.format.DateFormat
							.format("dd-MM-yyyy", datesms);
					timestring = (String) android.text.format.DateFormat
							.format("kk:mm", datesms);
					// Toast.makeText(getApplicationContext(), datestring,
					// Toast.LENGTH_SHORT).show();
					dateFromSms = new Date(datesms);

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
				SMS smsnew;

				if (phoneNumber != null) {
					smsnew = new SMS("unknown", threadid, datestring,
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
				}

			} while (curinbox.moveToNext());

		}

		List<Conversation> listconv = populateList(allSMSlocal);
		if (listconv.isEmpty()) {

			LinearLayout wholeLayout = new LinearLayout(this);
			wholeLayout.setLayoutParams(new LayoutParams((int) TypedValue
					.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260,
							getResources().getDisplayMetrics()),
					LayoutParams.WRAP_CONTENT));
			// for the speech to text
			// bubble conversation.
			final FlowLayout flowlayoutspeechrecog1 = new FlowLayout(this);
			flowlayoutspeechrecog1.setLayoutParams(new LayoutParams(
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 310, getResources()
									.getDisplayMetrics()),
					LayoutParams.WRAP_CONTENT));
			flowlayoutspeechrecog1.setBackgroundResource(R.drawable.bubblelast);
			// microphone button
			ImageView speakButton = new ImageView(this);
			speakButton.setBackgroundResource(R.drawable.emptyinbox);
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.setMargins((int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_SP, 2, getResources()
							.getDisplayMetrics()), 0, 0, 0);
			flowlayoutspeechrecog1.addView(speakButton, layoutParams);
			// play button
			ImageView helpplay = new ImageView(this);
			helpplay.setBackgroundResource(R.drawable.playsms);
			helpplay.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					playKaraoke(flowlayoutspeechrecog1);
				}

			});

			String[] messageHelp = new String[] { "Bo杯e", "de", "r残eption",
					"vide.", "Pour", "残rire", "un", "nouveau", "message",
					"cliquez", "sur", "le", "bouton", "en", "bas", "de",
					"l'残ran." };
			for (int j = 0; j < messageHelp.length; ++j) {
				final Button but = new Button(this);
				but.setText(messageHelp[j]);
				but.setBackgroundResource(R.drawable.button);
				but.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mTts.setLanguage(Locale.FRENCH);
						// drop all pending entries in the playback queue.
						mTts.speak(but.getText().toString(),
								TextToSpeech.QUEUE_FLUSH, null);
					}
				});
				flowlayoutspeechrecog1.addView(but);
			}

			LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutParams2.setMargins((int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_SP, 10, getResources()
							.getDisplayMetrics()), (int) TypedValue
					.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
							getResources().getDisplayMetrics()), 0,
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
									.getDisplayMetrics()));

			wholeLayout.addView(flowlayoutspeechrecog1);
			wholeLayout.addView(helpplay);
			threadpage.addView(wholeLayout, layoutParams2);
		}
	}

	@Override
	public void onInit(int status) {

	}
}