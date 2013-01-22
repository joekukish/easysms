package org.easysms.android;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
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
	/** Data provider used to manage the Messages. */
	protected SmsContentProvider mContentProvider;
	/** Flag used to determine if the refresh is complete. */
	protected Boolean mIsComplete = false;
	/** Timer used to refresh the Message list. */
	protected Handler mTaskHandler = new Handler();

	private void createUpdateThread() {
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
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// opens the new message.

				Object selectedFromList = (lv.getItemAtPosition(position));
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
	public String getLogTag() {
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

		// initializes the provider.
		mContentProvider = new SmsContentProvider();

		// loads the available messages.
		displayListSMS();

		// starts thread that updates the layout.
		createUpdateThread();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_activity, menu);

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
			String name = mContentProvider
					.getContactNameFromNumber(firstsms.contact);
			String photoid = mContentProvider
					.getContactPhotoFromNumber(firstsms.contact);
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
				temp2.put("sent", R.drawable.ic_action_send);
			} else if (firstsms.sent == "no") {
				temp2.put("sent", R.drawable.received);
			}

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
							.format("dd-MM-yyyy", datesms);
					timestring = (String) android.text.format.DateFormat
							.format("kk:mm", datesms);
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
				Sms smsnew;

				if (phoneNumber != null) {
					smsnew = new Sms("unknown", threadid, datestring,
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

		populateList(allSMSlocal);
	}
}