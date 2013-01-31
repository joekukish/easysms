package org.easysms.android;

import java.util.List;
import java.util.Locale;

import org.easysms.android.data.Contact;
import org.easysms.android.data.Conversation;
import org.easysms.android.data.Sms;
import org.easysms.android.provider.MyPagerAdapter;
import org.easysms.android.provider.SmsContentProvider;
import org.easysms.android.ui.KaraokeLayout;
import org.easysms.android.util.TextToSpeechManager;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
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
	private String mContactPhonenumber;
	// private Vibrator mVibrationService;

	/** Provider used to manage the underlying SMS data. */
	private SmsContentProvider mContentProvider;
	/** Default Locale used through the application. */
	private Locale mCurrentLocale = Locale.ENGLISH;
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
		return mContactPhonenumber;
	}

	public SmsContentProvider getContentProvider() {
		return mContentProvider;
	}

	public final boolean isInternetOn() {
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

		// gets the service from the system.
		// mVibrationService = (Vibrator)
		// getSystemService(Context.VIBRATOR_SERVICE);

		// objects the provider from the application.
		mContentProvider = ((EasySmsApp) getApplication()).getContentProvider();

		Bundle bundle = getIntent().getExtras();
		Boolean newMsg = bundle.getBoolean(NEW_MESSAGE_EXTRA);

		// gets the area where the message is composed.
		mSendLayout = (KaraokeLayout) findViewById(R.id.view_message_compose_bubble);

		if (!newMsg) {
			// shows and existing thread.
			setContentView(R.layout.act_view_message);

			// obtains the user info from the extras.
			mContactName = (String) bundle.get(MessageActivity.NAME_EXTRA);
			mContactPhonenumber = (String) bundle
					.get(MessageActivity.PHONENUMBER_EXTRA);

			// sets the adapter of the ViewPager.
			MyPagerAdapter adapter = new MyPagerAdapter(this);
			ViewPager myPager = (ViewPager) findViewById(R.id.view_message_view_pager);
			myPager.setAdapter(adapter);
			myPager.setCurrentItem(0);

			// allows the top bar to be different.
			ActionBar actionBar = getSupportActionBar();
			// adds the menu items to the bottom part while preserving the
			// home button.
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
					| ActionBar.DISPLAY_SHOW_CUSTOM);

			// sets the custom top bar.
			actionBar.setCustomView(R.layout.topbar_view_message);

			TextView nameTextView = (TextView) actionBar.getCustomView()
					.findViewById(R.id.topbar_view_message_name);
			TextView phonenumberTextView = (TextView) actionBar.getCustomView()
					.findViewById(R.id.topbar_view_message_phonenumber);

			// sets the data in the action bar.
			nameTextView.setText(mContactName);
			phonenumberTextView.setText(mContactPhonenumber);

			// enables the icon to serve as back.
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

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

		// RelativeLayout relativeLayout = (RelativeLayout) menu.findItem(
		// R.id.layout_item).getActionView();
		//
		// View inflatedView = getLayoutInflater().inflate(
		// R.layout.media_bottombar, null);
		//
		// relativeLayout.addView(inflatedView);
		//
		// return true;

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:

			// goes back to the home upon the back button click.
			Intent intent = new Intent();
			intent.setClass(this, InboxActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);

			return true;

		case R.id.menu_send:

			if (mContactPhonenumber.length() > 0
					&& mSendLayout.getChildCount() > 1) {
				// retrieve SMS body
				String message = "";
				for (int i = 1; i < mSendLayout.getChildCount(); ++i) {
					// message += flowlayout.getChildAt(i).getText();
					Button btn = (Button) mSendLayout.getChildAt(i);
					message += btn.getText();
					message += " ";
				}
				// send SMS
				mContentProvider.sendSMS(mContactPhonenumber, message);
				// insert SMS sent into DB
				// final Calendar c = Calendar.getInstance();
				String threadid = retrieveThreadIdFromNumberContact(mContactPhonenumber);
				// right after the msg is sent, navigate to the message
				// details page
				Intent i = new Intent(MessageActivity.this,
						MessageActivity.class);

				Contact contact = mContentProvider
						.getContact(mContactPhonenumber);

				Bundle bundle = new Bundle();
				// Add the parameters to bundle
				bundle.putString("Name", contact.displayName);
				bundle.putString("Tel", mContactPhonenumber);
				bundle.putBoolean("NewMsg", false);
				// Add this bundle to the intent
				i.putExtras(bundle);
				startActivity(i);
				// insert SMS in the data base content provider
				ContentValues values = new ContentValues();
				values.put("address", mContactPhonenumber);
				// values.put("date",dateToday);
				values.put("read", 1);
				values.put("thread_id", threadid);
				values.put("body", message);
				getContentResolver().insert(Uri.parse("content://sms/sent"),
						values);

			}

			else if (mSendLayout.getChildCount() <= 1) {
				Toast.makeText(getBaseContext(),
						"Entrez un message s'il vous plait.",
						Toast.LENGTH_SHORT).show();

				// plays the audio.
				TextToSpeechManager.getInstance().say(
						"Entrez un message s'il vous plait.");

			} else if (mContactPhonenumber.length() > 0) {
				Toast.makeText(getBaseContext(),
						"Entrez un numéro s'il vous plait.", Toast.LENGTH_SHORT)
						.show();

				// plays the audio.
				TextToSpeechManager.getInstance().say(
						"Entrez un message s'il vous plait.");

			}
			return true;
		case R.id.menu_call:
			return true;
		case R.id.menu_delete:
			// Do something long
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
				// Do something long
				Runnable runnable2 = new Runnable() {
					@Override
					public void run() {

						// plays the audio.
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
				new Thread(runnable2).start();
			}

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void playKaraoke(KaraokeLayout fl) {

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
		if (mCurrentLocale == Locale.FRENCH)
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
		else if (mCurrentLocale == Locale.ITALIAN)
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
		else if (mCurrentLocale == Locale.ENGLISH)
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-EN");

		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

}
