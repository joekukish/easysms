package org.easysms.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.easysms.android.data.Conversation;
import org.easysms.android.data.Sms;
import org.easysms.android.provider.SmsContentProvider;
import org.easysms.android.util.ApplicationTracker;
import org.easysms.android.util.ApplicationTracker.EventType;
import org.easysms.android.util.TextToSpeechManager;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

@TargetApi(8)
public class InboxActivity extends SherlockListActivity {

	// list of hash map for the message threads
	private static final ArrayList<HashMap<String, Object>> mMessageList = new ArrayList<HashMap<String, Object>>();
	/** Flag used to determine if the refresh is complete. */
	protected Boolean mIsComplete = false;
	/** Handler used to update the list as new messages arrive. */
	protected Handler mTaskHandler;
	/** Class that handles the SMS extraction. */
	protected SmsContentProvider mContentProvider;

	/**
	 * Creates a thread that updates the message list every 10 seconds.
	 */
	private void createUpdateThread() {

		// creates the handler used to process the message update.
		mTaskHandler = new Handler();

		// le timer fait ramer toute l'application!!! trouver un autre moyen
		// ==> retrieve a signal when a new msg is received.
		// -------------------timer------------------------
		final long elapse = 10000;
		Runnable t = new Runnable() {
			public void run() {
				mMessageList.clear();
				displayListSMS();
				if (!mIsComplete) {
					mTaskHandler.postDelayed(this, elapse);
				}
			}
		};
		mTaskHandler.postDelayed(t, elapse);
	}

	private void displayListSMS() {

		// gets the messages from the provider and groups them into
		// conversations.
		populateList(mContentProvider.getMessages());

		final ListView lv = getListView();

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// Object selectedFromList = (lv.getItemAtPosition(position));
				HashMap<String, Object> o = (HashMap<String, Object>) mMessageList
						.get(position);

				// TODO: should we pass the threadid? this approach will not
				// work if multiple destinatary messages are allowed.

				// gets the parameters from the selected message
				String telnum = (String) o.get("telnumber");
				String name = (String) o.get("name");

				// activity use to show the message.
				Intent i = new Intent(InboxActivity.this, MessageActivity.class);
				// creates and initializes the bundle.
				Bundle bundle = new Bundle();
				// adds the parameters to bundle
				bundle.putString(MessageActivity.NAME_EXTRA, name);
				bundle.putString(MessageActivity.PHONENUMBER_EXTRA, telnum);
				bundle.putBoolean(MessageActivity.NEW_MESSAGE_EXTRA, false);
				// adds this bundle to the intent
				i.putExtras(bundle);
				startActivity(i);

			}
		});
		// creates the adapter that handles the message list.
		final SimpleAdapter adapter = new SimpleAdapter(this, mMessageList,
				R.layout.tpl_inbox_item, new String[] { "avatar", "telnumber",
						"date", "name", "message" }, new int[] {
						R.id.inbox_item_image_contact,
						R.id.inbox_item_text_phonenumber,
						R.id.inbox_item_text_date, R.id.inbox_item_text_name,
						R.id.inbox_item_text_message });

		setListAdapter(adapter);
	}

	/**
	 * Gets the tag used to log the actions performed by the user. The tag is
	 * obtained from the name of the class.
	 * 
	 * @return the tag that represents the class.
	 */
	protected String getLogTag() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);

		// sets the theme used throughout the application.
		setTheme(EasySmsApp.THEME);

		// sets the desired layout for the activity.
		setContentView(R.layout.act_inbox);

		// objects the provider from the application.
		mContentProvider = ((EasySmsApp) getApplication()).getContentProvider();

		getContentResolver().registerContentObserver(
				Uri.parse("content://sms/"), true, mContentProvider);

		// initializes the TextToSpeech
		TextToSpeechManager.init(getApplicationContext());
		// sets the default language locale.
		TextToSpeechManager.getInstance().setLocale(Locale.ENGLISH);

		// sets the device id which will be used to track the activity of all
		// phones.
		ApplicationTracker.getInstance().setDeviceId(
				((EasySmsApp) getApplication()).getDeviceId());

		// tracks that the activity was opened.
		ApplicationTracker.getInstance().logEvent(EventType.ACTIVITY_VIEW,
				getLogTag());

		// loads the available messages.
		displayListSMS();

		// starts thread that updates the layout.
		createUpdateThread();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_inbox, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_new_message:

			// creates an intent for the next screen.
			Intent i = new Intent(InboxActivity.this, MessageActivity.class);
			// creates and initializes a new bundle.
			Bundle bundle = new Bundle();
			// indicates that a new message is being created.
			bundle.putBoolean(MessageActivity.NEW_MESSAGE_EXTRA, true);
			// adds the bundle to the intent.
			i.putExtras(bundle);
			startActivity(i);

			// tracks the user activity.
			ApplicationTracker.getInstance().logEvent(EventType.CLICK,
					getLogTag(), R.id.menu_new_message);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private List<Conversation> populateList(List<Sms> allSMS) {

		// list with all the conversations
		List<Conversation> allconversations = new ArrayList<Conversation>();
		for (Sms smsnew : allSMS) {
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
					List<Sms> newlist = new ArrayList<Sms>();
					newlist.add(smsnew);
					newconv.listsms = newlist;
					newconv.threadid = smsnew.threadid;
					allconversations.add(newconv);
				}

			}
		}

		for (Conversation conv : allconversations) {
			HashMap<String, Object> temp2 = new HashMap<String, Object>();

			// we use the first message of the list.
			Sms firstsms = conv.listsms.get(0);
			// get name associated to phone number
			String name = mContentProvider
					.getContactNameFromNumber(firstsms.contact);
			String photoId = mContentProvider
					.getContactPhotoFromNumber(firstsms.contact);

			temp2.put("avatar", R.drawable.nophotostored);

			// loads the photo bitmap.
			if (photoId != null) {

				Bitmap photo = mContentProvider.getContactPhotoWithId(photoId);
				if (photo != null) {
					temp2.put("avatar", photo);
				}
			}

			// adds the objects to display of the message.
			temp2.put("telnumber", firstsms.contact);
			temp2.put("date", firstsms.getDate(this));
			temp2.put("name", name);
			temp2.put("message", firstsms.body);
			temp2.put("sent", firstsms.isSent ? R.drawable.ic_action_send
					: R.drawable.received);

			mMessageList.add(temp2);
		}

		return allconversations;
	}
}