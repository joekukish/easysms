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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

		// checks if internet is available.
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

		Bundle bundle = getIntent().getExtras();
		Boolean newMsg = bundle.getBoolean(EXTRA_NEW_MESSAGE);

		// configures and loads the google analytics tracker.
		EasyTracker.getInstance().setContext(this);
		mTracker = EasyTracker.getTracker();

		if (!newMsg) {
			// shows and existing thread.
			setContentView(R.layout.act_view_message);

			// gets the area where the message is composed.
			mComposeLayout = (KaraokeLayout) findViewById(R.id.view_message_karaoke_compose);

			// obtains the user info from the extras.
			mContactName = (String) bundle.get(MessageActivity.EXTRA_NAME);
			mContactPhoneNumber = (String) bundle
					.get(MessageActivity.EXTRA_PHONE_NUMBER);

			// sets the adapter of the ViewPager.
			mPagerAdapter = new MessageViewPagerAdapter(this);
			mViewPager = (ViewPager) findViewById(R.id.view_message_view_pager);
			mViewPager.setAdapter(mPagerAdapter);
			mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
			mViewPager.setCurrentItem(0);

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

			// enables the icon to serve as back.
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

			// gets the send button and wires the event.
			ImageButton sendButton = (ImageButton) findViewById(R.id.view_message_button_send);
			sendButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onSendButtonClick();
				}
			});
			// sendButton.setActivated(false);
		}

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
		inflater.inflate(R.menu.menu_view_message, menu);

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
		if (activities.size() == 0) {

			// tracks the error.
			ApplicationTracker.getInstance().logEvent(EventType.ERROR, this,
					"speech_recognizer", "speech_recognizer_not_available");
			mTracker.sendEvent("app_error", "speech_recognizer",
					"speech_recognizer_not_available", new Date().getTime());

			MenuItem item = menu.findItem(R.id.menu_voice);
			item.setEnabled(false);
			item.setVisible(false);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	protected void onSendButtonClick() {

		// message said using the TTS and a Toast.
		String promptText;

		if (mContactPhoneNumber.length() > 0
				&& !mComposeLayout.getText().equals("")) {

			// sends the current message as an SMS.
			mContentProvider.sendSMS(mContactPhoneNumber,
					mComposeLayout.getText());

			// inserts the SMS sent into DB
			String threadid = retrieveThreadIdFromNumberContact(mContactPhoneNumber);
			// right after the msg is sent, navigate to the message
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
