package org.easysms.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.easysms.android.data.Contact;
import org.easysms.android.data.Conversation;
import org.easysms.android.data.Sms;
import org.easysms.android.provider.SmsContentProvider;
import org.easysms.android.util.ApplicationTracker;
import org.easysms.android.util.ApplicationTracker.EventType;
import org.easysms.android.util.TextToSpeechManager;
import org.easysms.android.view.InboxItemAdapter;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;

@TargetApi(8)
public class InboxActivity extends SherlockListActivity implements
		OnItemClickListener {

	/** Adapter used to manage the message list. */
	private InboxItemAdapter mAdapter;
	/** Class that handles the SMS extraction. */
	private SmsContentProvider mContentProvider;
	/** List of conversations that are displayed in the inbox. */
	private ArrayList<HashMap<String, Object>> mMessageList;

	/**
	 * Loads the complete message list.
	 */
	private void displayMessageList() {

		// gets the list and sets the item listener.
		final ListView lv = getListView();
		lv.setOnItemClickListener(this);

		// creates and sets the adapter.
		mAdapter = new InboxItemAdapter(this, mMessageList);
		lv.setAdapter(mAdapter);

		// handles the content loading in a separate thread
		new Handler().post(new Runnable() {
			public void run() {

				// gets the messages from the provider and groups them into
				// conversations.
				populateList(mContentProvider.getMessages());

				// notifies that the UI needs to be updated.
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);

		// sets the desired layout for the activity.
		setContentView(R.layout.act_inbox);

		// objects the provider from the application.
		mContentProvider = ((EasySmsApp) getApplication()).getContentProvider();

		// starts the service in charge of monitoring any SMS change.
		startService(new Intent(this, SmsService.class));

		// initializes the TextToSpeech
		TextToSpeechManager.init(getApplicationContext());
		// uses the same locale from the system.
		TextToSpeechManager.getInstance().setLocale(Locale.getDefault());

		// sets the device id which will be used to track the activity of all
		// phones.
		ApplicationTracker.getInstance().setDeviceId(
				((EasySmsApp) getApplication()).getDeviceId());

		// gets the current tracker.
		EasyTracker.getInstance().setContext(this);

		// initializes the list where the messages will be stored.
		mMessageList = new ArrayList<HashMap<String, Object>>();

		// loads the available messages.
		displayMessageList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_inbox, menu);

		return true;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		// TODO: shouldn't we pass the thread id? this approach will not
		// work if multiple recipients messages are allowed.

		// Object selectedFromList = (lv.getItemAtPosition(position));
		HashMap<String, Object> o = (HashMap<String, Object>) mMessageList
				.get(position);

		// tracks the user activity.
		ApplicationTracker.getInstance().logEvent(EventType.CLICK, this,
				"inbox_item", o.get("telnumber"));
		// tracks using google analytics.
		EasyTracker.getTracker().sendEvent("ui_action", "inbox_item_press",
				(String) o.get("telnumber"), null);

		// gets the parameters from the selected message
		long threadid = (Long) o.get("thread_id");
		String telnum = (String) o.get("telnumber");
		String name = (String) o.get("name");

		// activity use to show the message.
		Intent i = new Intent(InboxActivity.this, MessageActivity.class);

		// creates and initializes the bundle.
		Bundle bundle = new Bundle();
		// adds the parameters to bundle
		bundle.putLong(MessageActivity.EXTRA_THREAD_ID, threadid);
		bundle.putString(MessageActivity.EXTRA_NAME, name);
		bundle.putString(MessageActivity.EXTRA_PHONE_NUMBER, telnum);
		bundle.putBoolean(MessageActivity.EXTRA_NEW_MESSAGE, false);
		// adds this bundle to the intent
		i.putExtras(bundle);
		// starts the new activity.
		startActivity(i);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_new_message:

			// tracks the user activity.
			ApplicationTracker.getInstance().logEvent(EventType.CLICK, this,
					"new_message_button", item.getItemId());
			// tracks using google analytics.
			EasyTracker.getTracker().sendEvent("ui_action", "button_press",
					"new_message_button", (long) item.getItemId());

			// creates an intent for the next screen.
			Intent i = new Intent(InboxActivity.this, MessageActivity.class);
			// creates and initializes a new bundle.
			Bundle bundle = new Bundle();
			// indicates that a new message is being created.
			bundle.putBoolean(MessageActivity.EXTRA_NEW_MESSAGE, true);
			// adds the bundle to the intent.
			i.putExtras(bundle);
			startActivity(i);

			return true;
		default:
			return super.onOptionsItemSelected(item);
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

	private List<Conversation> populateList(List<Sms> allSMS) {

		// list with all the conversations
		List<Conversation> allconversations = new ArrayList<Conversation>();
		for (Sms smsnew : allSMS) {
			boolean add = false;
			// case where the SMS is a draft, otherwise the pp bugs.
			if (smsnew.address != null) {
				for (Conversation conv : allconversations) {
					if (conv.threadid == smsnew.threadid) {
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

			// object used to store the properties of the conversation.
			HashMap<String, Object> temp2 = new HashMap<String, Object>();

			// we use the first message of the list.
			Sms firstsms = conv.listsms.get(0);
			// gets the contact using the number in the SMS.
			Contact contact = mContentProvider.getContact(firstsms.address);
			// loads the photo bitmap.
			Bitmap photo = mContentProvider
					.getContactPhoto(contact.phoneNumber);
			if (photo != null) {
				temp2.put("avatar", photo);
			}

			// adds the objects to display of the message.
			temp2.put("count", conv.listsms.size());
			temp2.put("telnumber", firstsms.address);
			temp2.put("date", firstsms.getDate(this));
			temp2.put("name", contact.displayName);
			temp2.put("message", firstsms.body);
			temp2.put("thread_id", conv.threadid);
			temp2.put("read", firstsms.isRead);

			mMessageList.add(temp2);
		}

		return allconversations;
	}
}