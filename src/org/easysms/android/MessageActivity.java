package org.easysms.android;

import java.util.Date;
import java.util.List;

import org.easysms.android.data.Contact;
import org.easysms.android.data.Sms;
import org.easysms.android.provider.SmsContentProvider;
import org.easysms.android.ui.KaraokeLayout;
import org.easysms.android.util.ApplicationTracker;
import org.easysms.android.util.ApplicationTracker.EventType;
import org.easysms.android.util.TextToSpeechManager;
import org.easysms.android.view.MessageViewPagerAdapter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

@TargetApi(8)
public class MessageActivity extends SherlockActivity {

	/** Identifier of the extra used to pass the name. */
	public static final String EXTRA_NAME = "Name";
	/** Identifier of the extra that indicates a new message should be shown. */
	public static final String EXTRA_NEW_MESSAGE = "NewMsg";
	/** Identifier of the extra used to pass the phone number. */
	public static final String EXTRA_PHONE_NUMBER = "Tel";

	/** Code used to detect to the Pick Contact Intent. */
	private static final int PICK_CONTACT = 4321;
	/** Code used to detect the Voice Recognition Intent. */
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	/** KaraokeLayout where the message to send is composed. */
	private KaraokeLayout mComposeLayout;
	/** Name of the contact. */
	private String mContactName;
	/** Phone number of the contact. */
	private String mContactPhoneNumber;
	/** Provider used to manage the underlying SMS data. */
	private SmsContentProvider mContentProvider;
	/** Handler used to execute actions in another thread. */
	private Handler mHandler;
	/** Indicates whether we are displaying a new message or an exiting thread. */
	private boolean mIsNewMessage;
	/** Adapter used to handle the content inside the ViewPager. */
	private MessageViewPagerAdapter mPagerAdapter;
	/** Tracker used for Google Analytics. */
	protected Tracker mTracker;

	/** Pager that allows swiping between the views. */
	private ViewPager mViewPager;

	/**
	 * Adds the given text into the send bubble.
	 * 
	 * @param text
	 *            text to add.
	 */
	public void addTextToMessage(String text) {
		// adds the text in karaoke mode.
		mComposeLayout.setText(mComposeLayout.getText() + " " + text);
	}

	public String getContactName() {
		return mContactName;
	}

	public String getContactPhonenumber() {
		return mContactPhoneNumber;
	}

	public SmsContentProvider getContentProvider() {
		return mContentProvider;
	}

	/**
	 * Detects if there is an active Internet connection.
	 * 
	 * @return true if there is an active Internet connection, otherwise false.
	 */
	public boolean isInternetOn() {
		ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		// checks if Internet is available.
		if ((connec.getNetworkInfo(0) != null && connec.getNetworkInfo(0)
				.getState() == NetworkInfo.State.CONNECTED)
				|| (connec.getNetworkInfo(1) != null && connec
						.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED)) {

			return true;
		} else if ((connec.getNetworkInfo(0) != null && connec
				.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED)
				|| (connec.getNetworkInfo(1) != null && connec
						.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED)) {
			return false;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {

			// obtains the results from the voice recognizer.
			List<String> matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			// gives the content to the adapter.
			mPagerAdapter.displayVoiceOptions(matches);
			// changes the current item.
			mViewPager.setCurrentItem(3);
		} else if (requestCode == PICK_CONTACT
				&& resultCode == Activity.RESULT_OK) {

			Uri contactData = data.getData();
			Cursor cur = getContentResolver().query(contactData, null, null,
					null, null);

			if (cur.moveToFirst()) {

				String id = cur.getString(cur.getColumnIndexOrThrow(Phone._ID));
				String contactName = cur
						.getString(cur
								.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
				long photoId = cur
						.getLong(cur
								.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));

				String contactNumber = null;

				// checks if it has registered phone numbers.
				if (Integer
						.parseInt(cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

					// queries the number.
					Cursor pCur = getContentResolver().query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID
									+ " = ?", new String[] { id }, null);

					// this second loop will retrieve all the contact
					// numbers for a particular contact id
					String mobilePhone = null;
					String homePhone = null;
					String workPhone = null;
					String otherPhone = null;

					if (pCur != null) {

						// extracts all the available numbers.
						while (pCur.moveToNext()) {
							if (pCur.getInt(pCur.getColumnIndex(Phone.TYPE)) == Phone.TYPE_MOBILE) {

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
						// uses the first available number.
						if (mobilePhone != null) {
							contactNumber = mobilePhone;
						} else if (homePhone != null) {
							contactNumber = homePhone;
						} else if (workPhone != null) {
							contactNumber = workPhone;
						} else if (otherPhone != null) {
							otherPhone = contactNumber;
						}
					}

					pCur.close();
				}

				// gets the image using the photo id.
				Bitmap profileImage = mContentProvider
						.getContactPhotoWithPhotoId(photoId);

				// sets the image of the contact.
				ImageView profile = (ImageView) findViewById(R.id.new_message_image_contact);
				if (profileImage != null) {
					profile.setImageBitmap(profileImage);
				} else {
					profile.setImageResource(R.drawable.nophotostored);
				}

				// updates the name and number.
				TextView recipientName = (TextView) findViewById(R.id.new_message_text_name);
				TextView recipientNumber = (TextView) findViewById(R.id.new_message_text_phone_number);

				// append new name selected
				recipientName.setText(contactName);
				recipientNumber.setText(contactNumber);

				// sets the recipients number.
				mContactPhoneNumber = contactNumber;

				cur.close();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// sets the theme used throughout the application.
		setTheme(EasySmsApp.THEME);

		// objects the provider from the application.
		mContentProvider = ((EasySmsApp) getApplication()).getContentProvider();

		// checks if it is a new message from the bundle.
		Bundle bundle = getIntent().getExtras();
		mIsNewMessage = bundle.getBoolean(EXTRA_NEW_MESSAGE, false);

		// configures and loads the google analytics tracker.
		EasyTracker.getInstance().setContext(this);
		mTracker = EasyTracker.getTracker();

		if (mIsNewMessage) {
			// shows and existing thread.
			setContentView(R.layout.act_new_message);

			// adds the contact listener used to select the target contact.
			ImageView contactImage = (ImageView) findViewById(R.id.new_message_image_contact);
			contactImage.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_PICK,
							ContactsContract.Contacts.CONTENT_URI);
					startActivityForResult(intent, PICK_CONTACT);
				}
			});

		} else {

			// shows and existing thread.
			setContentView(R.layout.act_view_message);

			// obtains the user info from the extras.
			mContactName = (String) bundle.get(MessageActivity.EXTRA_NAME);
			mContactPhoneNumber = (String) bundle
					.get(MessageActivity.EXTRA_PHONE_NUMBER);

			// allows the top bar to be different.
			ActionBar actionBar = getSupportActionBar();

			// modifies layout depending if name is available or not.
			if (mContactName == null || mContactName.trim().equals("")) {
				// sets the phone number instead of the name since it is not
				// available.
				// the action bar handles the layout change.
				actionBar.setTitle(mContactPhoneNumber);

			} else {
				actionBar.setTitle(mContactName);
				actionBar.setSubtitle(mContactPhoneNumber);
			}
		}

		// sets the adapter of the ViewPager.
		mPagerAdapter = new MessageViewPagerAdapter(this);

		// configures the pager that allows to swap between different views
		// by swiping.
		mViewPager = (ViewPager) findViewById(R.id.view_message_view_pager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
		mViewPager.setCurrentItem(0);

		// gets the area where the message is composed.
		mComposeLayout = (KaraokeLayout) findViewById(R.id.view_message_karaoke_compose);
		mComposeLayout
				.setOnKaraokeClickListener(new KaraokeLayout.OnKaraokeClickListener() {

					@Override
					public void onClick(Button button) {

						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.CLICK, MessageActivity.this,
								"compose_bubble_word", button.getText());
						// tracks using google analytics.
						mTracker.sendEvent("ui_action", "button_press",
								"compose_bubble_word", null);

					}
				});

		mComposeLayout
				.setOnKaraokeLongClickListener(new KaraokeLayout.OnKaraokeLongClickListener() {

					@Override
					public boolean onLongClick(Button button) {

						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.LONG_CLICK, MessageActivity.this,
								"compose_bubble_word", button.getText());
						// tracks using google analytics.
						mTracker.sendEvent("ui_action", "button_long_press",
								"compose_bubble_word", null);

						// removes the test from the bubble.
						mComposeLayout.removeWordButton(button);

						return true;
					}
				});

		mComposeLayout
				.setOnKaraokePlayButtonClickListener(new KaraokeLayout.OnKaraokePlayButtonClickListener() {

					@Override
					public boolean onPlayClick() {
						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.CLICK, MessageActivity.this,
								"compose_bubble_play");
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_press", "compose_bubble_play", null);
						return true;
					}
				});

		// enables the icon to serve as back.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// gets the send button and wires the event.
		ImageButton sendButton = (ImageButton) findViewById(R.id.view_message_button_send);
		sendButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// tracks the user activity.
				ApplicationTracker.getInstance().logEvent(EventType.CLICK,
						MessageActivity.this, "send_button");
				// tracks using google analytics.
				EasyTracker.getTracker().sendEvent("ui_action", "button_press",
						"send_button", null);

				// sends the current message in the compose bubble.
				sendMessage();
			}
		});

		// initializes the handler for voice recognition.
		mHandler = new Handler();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu((ContextMenu) menu, view, menuInfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_message, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
		case android.R.id.home:

			// tracks the user activity.
			ApplicationTracker.getInstance().logEvent(EventType.CLICK, this,
					"home_button", item.getItemId());
			// tracks using google analytics.
			mTracker.sendEvent("ui_action", "button_press", "home_button",
					(long) item.getItemId());

			// goes back to the home upon the back button click.
			intent = new Intent();
			intent.setClass(this, InboxActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);

			return true;

		case R.id.menu_call:

			// tracks the user activity.
			ApplicationTracker.getInstance().logEvent(EventType.CLICK, this,
					"call_button", item.getItemId());
			// tracks using google analytics.
			mTracker.sendEvent("ui_action", "button_press", "call_button",
					(long) item.getItemId());

			// creates the uri.
			String uri = "tel:" + mContactPhoneNumber;

			// creates a new intent with the call action.
			intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse(uri));
			startActivity(intent);

			return true;

		case R.id.menu_delete:

			// tracks the user activity.
			ApplicationTracker.getInstance().logEvent(EventType.CLICK, this,
					"delete_button", item.getItemId());
			// tracks using google analytics.
			mTracker.sendEvent("ui_action", "button_press", "delete_button",
					(long) item.getItemId());

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					for (int i = 1; i < mComposeLayout.getChildCount(); ++i) {
						final Button btn = (Button) mComposeLayout
								.getChildAt(i);

						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								mComposeLayout.removeView(btn);
							}
						});
					}
				}
			};
			new Thread(runnable).start();

			return true;
		case R.id.menu_voice:

			// tracks the user activity.
			ApplicationTracker.getInstance().logEvent(EventType.CLICK, this,
					"voice_button", item.getItemId());
			// tracks using google analytics.
			mTracker.sendEvent("ui_action", "button_press", "voice_button",
					(long) item.getItemId());

			if (isInternetOn()) {

				// plays the audio.
				TextToSpeechManager.getInstance().say(
						getResources().getString(R.string.promt_voice));

				// prepares and launches the voice recognition activity.
				startVoiceRecognitionActivity();
			} else {
				// plays the audio.
				TextToSpeechManager.getInstance().say(
						getResources().getString(R.string.promt_no_internet));

				// tracks the error.
				ApplicationTracker.getInstance().logEvent(EventType.ERROR,
						this, "speech_recognizer",
						"internet_connetion_not_available");
				mTracker.sendEvent("app_error", "speech_recognizer",
						"internet_connetion_not_available",
						new Date().getTime());
			}

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// disables recognition button if service is not present.
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

		MenuItem item;
		if (activities.size() == 0) {

			// tracks the error.
			ApplicationTracker.getInstance().logEvent(EventType.ERROR, this,
					"speech_recognizer", "speech_recognizer_not_available");
			mTracker.sendEvent("app_error", "speech_recognizer",
					"speech_recognizer_not_available", new Date().getTime());

			item = menu.findItem(R.id.menu_voice);
			item.setEnabled(false);
			item.setVisible(false);
		}

		// hides call and delete while composing a new message.
		if (mIsNewMessage) {

			item = menu.findItem(R.id.menu_call);
			item.setVisible(false);

			item = menu.findItem(R.id.menu_delete);
			item.setVisible(false);

		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onStart() {
		super.onStart();

		// tracks that the activity was opened.
		ApplicationTracker.getInstance()
				.logEvent(EventType.ACTIVITY_VIEW, this);

		// Google Analytics tracking.
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		// Google Analytics tracking.
		EasyTracker.getInstance().activityStop(this);
	}

	public String retrieveThreadIdFromNumberContact(String phoneNumContact) {
		for (Sms sms : mContentProvider.getMessages()) {
			String smscontact = sms.address;
			// TODO: could it really be null?
			if (smscontact != null && smscontact.equals(phoneNumContact))
				return sms.threadid;
		}
		return "error";
	}

	protected void sendMessage() {

		// message said using the TTS and a Toast.
		String promptText;

		if (mContactPhoneNumber != null && mContactPhoneNumber.length() > 0
				&& !mComposeLayout.getText().equals("")) {

			// sends the current message as an SMS.
			mContentProvider.sendSMS(mContactPhoneNumber,
					mComposeLayout.getText());

			// inserts the SMS sent into DB
			String threadid = retrieveThreadIdFromNumberContact(mContactPhoneNumber);
			// right after the message is sent, navigates to the message
			// details page
			Intent i = new Intent(MessageActivity.this, MessageActivity.class);

			// gets the contact data based on the phone number.
			Contact contact = mContentProvider.getContact(mContactPhoneNumber);

			// creates the bundle
			Bundle bundle = new Bundle();
			// adds the parameters to bundle
			bundle.putString(EXTRA_NAME, contact.displayName);
			bundle.putString(EXTRA_PHONE_NUMBER, mContactPhoneNumber);
			bundle.putBoolean(EXTRA_NEW_MESSAGE, false);
			// adds this bundle to the intent
			i.putExtras(bundle);
			startActivity(i);

			// insert SMS in the data base content provider
			ContentValues values = new ContentValues();
			values.put("address", mContactPhoneNumber);
			values.put("read", 1);
			values.put("thread_id", threadid);
			values.put("body", mComposeLayout.getText());
			getContentResolver()
					.insert(Uri.parse("content://sms/sent"), values);

			// TODO: update UI.
			// clears the text
			mComposeLayout.setText("");

		} else if (mComposeLayout.getText().equals("")) {

			promptText = getResources().getString(
					R.string.promt_enter_a_message);

			Toast.makeText(getBaseContext(), promptText, Toast.LENGTH_SHORT)
					.show();

			// plays the audio.
			TextToSpeechManager.getInstance().say(promptText);

		} else if (mContactPhoneNumber.length() > 0) {

			promptText = getResources().getString(
					R.string.promt_enter_a_phone_number);

			Toast.makeText(getBaseContext(), promptText, Toast.LENGTH_SHORT)
					.show();

			// plays the audio.
			TextToSpeechManager.getInstance().say(promptText);
		}
	}

	/**
	 * Fire an intent to start the speech recognition activity.
	 */
	private void startVoiceRecognitionActivity() {

		// creates an intent that can handle the speech recognition.
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

		// Specify the calling package to identify your application
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
				.getPackage().getName());

		// display an hint to the user about what he should say.
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources()
				.getString(R.string.promt_voice));

		// given an hint to the recognizer about what the user is going to say
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

		// number of results the recognizer should return.
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

		// sets the default language.
		// intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

		// starts the activity.
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

}
