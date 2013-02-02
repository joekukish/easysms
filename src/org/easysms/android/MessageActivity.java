package org.easysms.android;

import java.util.List;

import org.easysms.android.data.Contact;
import org.easysms.android.data.Sms;
import org.easysms.android.provider.MyPagerAdapter;
import org.easysms.android.provider.SmsContentProvider;
import org.easysms.android.ui.KaraokeLayout;
import org.easysms.android.util.TextToSpeechManager;

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
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

@TargetApi(8)
public class MessageActivity extends SherlockActivity {

	/** Identifier of the extra used to pass the name. */
	public static final String NAME_EXTRA = "Name";
	/** Identifier of the extra that indicates a new message should be shown. */
	public static final String NEW_MESSAGE_EXTRA = "NewMsg";
	/** Identifier of the extra used to pass the phone number. */
	public static final String PHONENUMBER_EXTRA = "Tel";
	/** Code used to detect the Voice Recognition Intent. */
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	/** Handler used to execute actions in another thread. */
	private Handler handler;
	private String mContactName;
	private String mContactPhoneNumber;

	/** Provider used to manage the underlying SMS data. */
	private SmsContentProvider mContentProvider;
	/** KaraokeLayout where the message to send is composed. */
	private KaraokeLayout mSendLayout;

	/**
	 * Adds the given text into the send bubble.
	 * 
	 * @param text
	 *            text to add.
	 */
	public void addTextToMessage(String text) {
		// adds the text in karaoke mode.
		mSendLayout.addText(text);
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
	 * Detects
	 * 
	 * @return
	 */
	public boolean isInternetOn() {
		ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// ARE WE CONNECTED TO THE NET
		if ((connec.getNetworkInfo(0) != null && connec.getNetworkInfo(0)
				.getState() == NetworkInfo.State.CONNECTED)
				|| (connec.getNetworkInfo(1) != null && connec
						.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED)) {
			// MESSAGE TO SCREEN FOR TESTING (IF REQ)
			// Toast.makeText(this, connec.getNetworkInfo(0) + "connected",
			// Toast.LENGTH_LONG).show();
			return true;
		} else if ((connec.getNetworkInfo(0) != null && connec
				.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED)
				|| (connec.getNetworkInfo(1) != null && connec
						.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED)) {
			// System.out.println(“Not Connected”);
			return false;
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// sets the theme used throughout the application.
		setTheme(EasySmsApp.THEME);

		// objects the provider from the application.
		mContentProvider = ((EasySmsApp) getApplication()).getContentProvider();

		Bundle bundle = getIntent().getExtras();
		Boolean newMsg = bundle.getBoolean(NEW_MESSAGE_EXTRA);

		if (!newMsg) {
			// shows and existing thread.
			setContentView(R.layout.act_view_message);

			// gets the area where the message is composed.
			mSendLayout = (KaraokeLayout) findViewById(R.id.view_message_compose_bubble);

			// obtains the user info from the extras.
			mContactName = (String) bundle.get(MessageActivity.NAME_EXTRA);
			mContactPhoneNumber = (String) bundle
					.get(MessageActivity.PHONENUMBER_EXTRA);

			// sets the adapter of the ViewPager.
			MyPagerAdapter adapter = new MyPagerAdapter(this);
			ViewPager myPager = (ViewPager) findViewById(R.id.view_message_view_pager);
			myPager.setAdapter(adapter);
			myPager.setCurrentItem(0);

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
		}

		// initializes the handler for voice recognition.
		handler = new Handler();
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

			// goes back to the home upon the back button click.
			intent = new Intent();
			intent.setClass(this, InboxActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);

			return true;

		case R.id.menu_call:

			// creates the uri.
			String uri = "tel:" + mContactPhoneNumber;

			// creates a new intent with the call action.
			intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse(uri));
			startActivity(intent);

			return true;

		case R.id.menu_delete:

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					for (int i = 1; i < mSendLayout.getChildCount(); ++i) {
						final Button btn = (Button) mSendLayout.getChildAt(i);

						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								mSendLayout.removeView(btn);
							}
						});
					}
				}
			};
			new Thread(runnable).start();

			return true;
		case R.id.menu_voice:

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
			MenuItem item = menu.findItem(R.id.menu_voice);
			item.setEnabled(false);
			item.setVisible(false);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	protected void onSendButtonClick() {
		if (mContactPhoneNumber.length() > 0 && mSendLayout.getChildCount() > 1) {
			// retrieve SMS body
			String message = "";
			for (int i = 1; i < mSendLayout.getChildCount(); ++i) {
				// message += flowlayout.getChildAt(i).getText();
				Button btn = (Button) mSendLayout.getChildAt(i);
				message += btn.getText();
				message += " ";
			}
			// send SMS
			mContentProvider.sendSMS(mContactPhoneNumber, message);

			// insert SMS sent into DB
			String threadid = retrieveThreadIdFromNumberContact(mContactPhoneNumber);
			// right after the msg is sent, navigate to the message
			// details page
			Intent i = new Intent(MessageActivity.this, MessageActivity.class);

			// gets the contact data based on the phone number.
			Contact contact = mContentProvider.getContact(mContactPhoneNumber);

			Bundle bundle = new Bundle();
			// Add the parameters to bundle
			bundle.putString("Name", contact.displayName);
			bundle.putString("Tel", mContactPhoneNumber);
			bundle.putBoolean("NewMsg", false);
			// Add this bundle to the intent
			i.putExtras(bundle);
			startActivity(i);
			// insert SMS in the data base content provider
			ContentValues values = new ContentValues();
			values.put("address", mContactPhoneNumber);
			// values.put("date",dateToday);
			values.put("read", 1);
			values.put("thread_id", threadid);
			values.put("body", message);
			getContentResolver()
					.insert(Uri.parse("content://sms/sent"), values);
		}

		else if (mSendLayout.getChildCount() <= 1) {
			Toast.makeText(getBaseContext(),
					"Entrez un message s'il vous plait.", Toast.LENGTH_SHORT)
					.show();

			// plays the audio.
			TextToSpeechManager.getInstance().say(
					getResources().getString(R.string.promt_enter_a_message));

		} else if (mContactPhoneNumber.length() > 0) {
			Toast.makeText(getBaseContext(),
					"Entrez un numéro s'il vous plait.", Toast.LENGTH_SHORT)
					.show();

			// plays the audio.
			TextToSpeechManager.getInstance().say(
					getResources().getString(R.string.promt_enter_a_message));
		}
	}

	public String retrieveThreadIdFromNumberContact(String phoneNumContact) {
		for (Sms sms : mContentProvider.getMessages()) {
			String smscontact = sms.contact;
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

		// Display an hint to the user about what he should say.
		// intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
		// "Speech recognition demo");

		// Given an hint to the recognizer about what the user is going to say
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

		// number of results the recognizer should return.
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

		// starts the activity.
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

}
