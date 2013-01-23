package org.easysms.android;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.easysms.android.data.Conversation;
import org.easysms.android.data.Sms;
import org.easysms.android.util.ApplicationTracker;
import org.easysms.android.util.ApplicationTracker.EventType;
import org.easysms.android.util.TextToSpeechManager;

import android.annotation.TargetApi;
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

		smsAllRetrieve();

		final ListView lv = getListView();

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// opens the new message.

				// Object selectedFromList = (lv.getItemAtPosition(position));
				HashMap<String, Object> o = (HashMap<String, Object>) mMessageList
						.get(position);
				String telnum = "unknown";
				String name = "unknown";
				Object telnumobj = o.get("telnumber");
				if (telnumobj != null)
					telnum = telnumobj.toString();
				Object nameobj = o.get("name");
				if (nameobj != null) {
					name = nameobj.toString();
				}

				// activity use to show the message.
				Intent i = new Intent(InboxActivity.this,
						MessagingActivity.class);
				// creates and initializes the bundle.
				Bundle bundle = new Bundle();
				// adds the parameters to bundle
				bundle.putString(MessagingActivity.NAME_EXTRA, name);
				bundle.putString(MessagingActivity.PHONENUMBER_EXTRA, telnum);
				bundle.putBoolean(MessagingActivity.NEW_MESSAGE_EXTRA, false);
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

		// initializes the TextToSpeech
		TextToSpeechManager.init(getApplicationContext());
		// sets the default language locale.
		TextToSpeechManager.getInstance().setLocale(Locale.FRENCH);

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

	private String getContactNameFromNumber(String number) {
		/*
		 * We have a phone number and we want to grab the name of the contact
		 * with that number, if such a contact exists
		 */
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));

		/* phoneNumber here being a variable with the phone number stored. */
		Cursor c = getContentResolver().query(lookupUri,
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
		c.close();
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
		Cursor c = getContentResolver().query(lookupUri,
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
		c.close();
		return photoId;
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
			Intent i = new Intent(InboxActivity.this, MessagingActivity.class);
			// creates and initializes a new bundle.
			Bundle bundle = new Bundle();
			// indicates that a new message is being created.
			bundle.putBoolean(MessagingActivity.NEW_MESSAGE_EXTRA, true);
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
			// on regarde le 1er sms de chaque liste

			Sms firstsms = conv.listsms.get(0);
			// get name associated to phone number
			String name = getContactNameFromNumber(firstsms.contact);
			String photoid = getContactPhotoFromNumber(firstsms.contact);
			if (photoid == null) {
				temp2.put("avatar", R.drawable.nophotostored);

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

					if (photoBitmap != null) {
						temp2.put("avatar", R.drawable.nophotostored);
					} else {
						temp2.put("avatar", R.drawable.nophotostored);
					}

				}
				photo2.close();

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

	// return a list with all the SMS and for each sms a status sent: yes or no
	private void smsAllRetrieve() {
		// we put all the SMS sent and received in a list
		List<Sms> allSMSlocal = new ArrayList<Sms>();
		Uri uriSMSURIinbox = Uri.parse("content://sms/");
		Cursor curinbox = getContentResolver().query(uriSMSURIinbox, null,
				null, null, null);
		if (curinbox.moveToFirst()) {
			long datesms = 0;
			String phoneNumber = null;
			String body = null;
			String threadid;

			int type = -1;
			int read = -1;
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
				Sms smsnew;

				if (phoneNumber != null) {
					smsnew = new Sms(threadid, new Date(datesms), phoneNumber,
							body);

					// to know if it is a message sent or received
					if (type == 2) { // SENT
						smsnew.isSent = true;
					} else if (type == 1) { // INBOX
						smsnew.isSent = false;
					}

					// indicates whether it was read or not.
					smsnew.isRead = read == 1;

					// we add this SMS to the list of all the SMS
					allSMSlocal.add(smsnew);
				}

			} while (curinbox.moveToNext());
		}

		populateList(allSMSlocal);
	}
}