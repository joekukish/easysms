package org.easysms.android.provider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.easysms.android.R;
import org.easysms.android.data.Contact;
import org.easysms.android.data.Conversation;
import org.easysms.android.data.Sms;
import org.easysms.android.util.TextToSpeechManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsContentProvider extends ContentObserver {

	/** Context used to query the SMS. */
	private Context mContext;
	/** List of cached messages. */
	private ArrayList<Sms> mMessages;

	/**
	 * Creates a new SmsContectProvider instance.
	 * 
	 * @param context
	 *            application context.
	 */
	public SmsContentProvider(Context context) {
		super(new Handler());
		mContext = context;
	}

	/**
	 * Obtains the contact information given a phone number.
	 * 
	 * @param number
	 *            phone number to check.
	 * 
	 * @return a Contact object with all registered data.
	 */
	public Contact getContact(String number) {

		// we want to take a contact that has the same phone number.
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));

		Cursor c = mContext.getContentResolver().query(lookupUri,
				new String[] { PhoneLookup._ID, /* PhoneLookup.PHOTO_URI, */
				PhoneLookup.PHOTO_ID, PhoneLookup.DISPLAY_NAME }, null, null,
				null);

		Contact tmpContact = new Contact();
		tmpContact.phoneNumber = number;

		if (c != null) {
			while (c.moveToNext()) {
				// if we find a match we put it in a String.
				tmpContact.id = c.getInt(c
						.getColumnIndexOrThrow(PhoneLookup._ID));
				// tmpContact.photoUri =
				// c.getString(c.getColumnIndexOrThrow(PhoneLookup.PHOTO_URI));
				tmpContact.photoId = c.getLong(c
						.getColumnIndexOrThrow(PhoneLookup.PHOTO_ID));
				tmpContact.displayName = c.getString(c
						.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
			}

			// closes the cursor.
			c.close();
		}
		return tmpContact;
	}

	public Bitmap getContactPhoto(String phoneNumber) {

		// path where the contact can be found with the given phone number.
		Uri phoneUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(phoneNumber));

		// path where the image can be found.
		Uri photoUri = null;

		Cursor contact = mContext.getContentResolver().query(phoneUri,
				new String[] { ContactsContract.Contacts._ID }, null, null,
				null);

		if (contact.moveToFirst()) {
			long userId = contact.getLong(contact
					.getColumnIndex(ContactsContract.Contacts._ID));
			photoUri = ContentUris.withAppendedId(
					ContactsContract.Contacts.CONTENT_URI, userId);

		}

		// closes the current cursor.
		contact.close();

		if (photoUri != null) {
			InputStream input = ContactsContract.Contacts
					.openContactPhotoInputStream(mContext.getContentResolver(),
							photoUri);
			if (input != null) {
				return BitmapFactory.decodeStream(input);
			}
		} else {
			return null;
		}
		return null;
	}

	public Bitmap getContactPhotoWithPhotoId(long photoId) {
		Cursor c = mContext.getContentResolver().query(
				ContactsContract.Data.CONTENT_URI,
				new String[] { ContactsContract.CommonDataKinds.Photo.PHOTO },
				ContactsContract.Data._ID + "=?",
				new String[] { Long.toString(photoId) }, null);
		byte[] imageBytes = null;
		if (c != null) {
			if (c.moveToFirst()) {
				imageBytes = c.getBlob(0);
			}
			c.close();
		}

		if (imageBytes != null) {
			return BitmapFactory.decodeByteArray(imageBytes, 0,
					imageBytes.length);
		} else {
			return null;
		}
	}

	protected synchronized List<Sms> getConversations() {

		// we put all the SMS sent and received in a list
		mMessages = new ArrayList<Sms>();

		// final String[] projection = new String[] { "*" };

		// thread_id, msg_count, snippet
		Cursor c = mContext.getContentResolver().query(
				Uri.parse("content://sms/conversations?simple=true"), null,
				null, null, "date DESC");

		// gets all the available messages.
		if (c.moveToFirst()) {

			// prints the contents.
			DatabaseUtils.dumpCurrentRow(c);

			long datesms = 0;
			String address = null;
			String body = null;
			long threadid = -1;
			String protocol = null;
			String person = null;
			int type = -1;
			int read = -1;

			// return -1 if the column does not exist.
			int dateColumn = c.getColumnIndex("date");
			int addressColumn = c.getColumnIndex("address");
			int bodyColumn = c.getColumnIndex("body");
			int threadColumn = c.getColumnIndex("thread_id");
			int typeColumn = c.getColumnIndex("type");
			int typeRead = c.getColumnIndex("read");
			int protocolColumn = c.getColumnIndex("protocol");
			int personColumn = c.getColumnIndex("person");

			do {
				if (dateColumn != -1)
					datesms = c.getLong(dateColumn);
				if (typeRead != -1)
					read = c.getInt(typeRead);
				if (typeColumn != -1)
					type = c.getInt(typeColumn);
				if (bodyColumn != -1)
					body = c.getString(bodyColumn);
				if (addressColumn != -1)
					address = c.getString(addressColumn);
				if (threadColumn != -1)
					threadid = c.getLong(threadColumn);
				if (protocolColumn != -1)
					protocol = c.getString(protocolColumn);
				if (personColumn != -1)
					person = c.getString(personColumn);

				Sms sms;

				if (address != null) {
					sms = new Sms(threadid, new Date(datesms), address, body);

					// sets the person that initiated the message.
					sms.person = person;
					// message type.
					sms.type = type;
					// indicates whether it was read or not.
					sms.isRead = read == 1;
					// send or received message.
					sms.protocol = protocol;

					// we add this SMS to the list of all the SMS
					mMessages.add(sms);
				}

			} while (c.moveToNext());
		}

		c.close();

		return mMessages;
	}

	public synchronized Conversation getConversationByThreadId(long threadId) {

		// TODO: cache conversations and return the correct.

		// creates temporary object that will be filled with data.
		Conversation tmpConversation = new Conversation();
		tmpConversation.listsms = new ArrayList<Sms>();
		tmpConversation.threadid = threadId;

		// queries the ContentResolver for the data.
		Cursor c = mContext.getContentResolver().query(
				Uri.parse("content://sms/"), null, "thread_id=" + threadId,
				null, "date ASC");

		// gets all the available messages.
		if (c != null && c.moveToFirst()) {
			long datesms = 0;
			String address = null;
			String body = null;
			long threadid = -1;
			String protocol = null;
			String person = null;
			int type = -1;
			int read = -1;

			// return -1 if the column does not exist.
			int dateColumn = c.getColumnIndex("date");
			int addressColumn = c.getColumnIndex("address");
			int bodyColumn = c.getColumnIndex("body");
			int threadColumn = c.getColumnIndex("thread_id");
			int typeColumn = c.getColumnIndex("type");
			int typeRead = c.getColumnIndex("read");
			int protocolColumn = c.getColumnIndex("protocol");
			int personColumn = c.getColumnIndex("person");

			do {
				if (dateColumn != -1)
					datesms = c.getLong(dateColumn);
				if (typeRead != -1)
					read = c.getInt(typeRead);
				if (typeColumn != -1)
					type = c.getInt(typeColumn);
				if (bodyColumn != -1)
					body = c.getString(bodyColumn);
				if (addressColumn != -1)
					address = c.getString(addressColumn);
				if (threadColumn != -1)
					threadid = c.getLong(threadColumn);
				if (protocolColumn != -1)
					protocol = c.getString(protocolColumn);
				if (personColumn != -1)
					person = c.getString(personColumn);

				Sms sms;

				if (address != null) {
					sms = new Sms(threadid, new Date(datesms), address, body);

					// sets the person that initiated the message.
					sms.person = person;
					// message type.
					sms.type = type;
					// indicates whether it was read or not.
					sms.isRead = read == 1;
					// send or received message.
					sms.protocol = protocol;

					// we add this SMS to the list of all the SMS
					tmpConversation.listsms.add(sms);
				}

			} while (c.moveToNext());

			// closes the cursor.
			c.close();
		}

		return tmpConversation;
	}

	public synchronized List<Sms> getMessages() {

		// if (mMessages == null) {

		// we put all the SMS sent and received in a list
		mMessages = new ArrayList<Sms>();

		Cursor c = mContext.getContentResolver().query(
				Uri.parse("content://sms/"), null, null, null, null);

		// gets all the available messages.
		if (c.moveToFirst()) {
			long datesms = 0;
			String address = null;
			String body = null;
			long threadid = -1;
			String protocol = null;
			String person = null;
			int type = -1;
			int read = -1;

			// return -1 if the column does not exist.
			int dateColumn = c.getColumnIndex("date");
			int addressColumn = c.getColumnIndex("address");
			int bodyColumn = c.getColumnIndex("body");
			int threadColumn = c.getColumnIndex("thread_id");
			int typeColumn = c.getColumnIndex("type");
			int typeRead = c.getColumnIndex("read");
			int protocolColumn = c.getColumnIndex("protocol");
			int personColumn = c.getColumnIndex("person");

			do {
				if (dateColumn != -1)
					datesms = c.getLong(dateColumn);
				if (typeRead != -1)
					read = c.getInt(typeRead);
				if (typeColumn != -1)
					type = c.getInt(typeColumn);
				if (bodyColumn != -1)
					body = c.getString(bodyColumn);
				if (addressColumn != -1)
					address = c.getString(addressColumn);
				if (threadColumn != -1)
					threadid = c.getLong(threadColumn);
				if (protocolColumn != -1)
					protocol = c.getString(protocolColumn);
				if (personColumn != -1)
					person = c.getString(personColumn);

				Sms sms;

				if (address != null) {
					sms = new Sms(threadid, new Date(datesms), address, body);

					// sets the person that initiated the message.
					sms.person = person;
					// message type.
					sms.type = type;
					// indicates whether it was read or not.
					sms.isRead = read == 1;
					// send or received message.
					sms.protocol = protocol;

					// we add this SMS to the list of all the SMS
					mMessages.add(sms);
				}

			} while (c.moveToNext());
		}

		c.close();
		// }

		return mMessages;
	}

	@Override
	public void onChange(boolean selfChange) {

		super.onChange(selfChange);

		// resets the message list.
		mMessages = null;
		// querySMS();
	}

	protected void querySMS() {
		Uri uriSMS = Uri.parse("content://sms/");
		Cursor cur = mContext.getContentResolver().query(uriSMS, null, null,
				null, null);
		// this will make it point to the first record, which
		// is the last SMS sent
		cur.moveToNext();

		// content of the SMS
		String body = cur.getString(cur.getColumnIndex("body"));
		// phone number
		String add = cur.getString(cur.getColumnIndex("address"));
		// date
		String time = cur.getString(cur.getColumnIndex("date"));
		// protocol, used to determine if sent or received.
		String protocol = cur.getString(cur.getColumnIndex("protocol"));

		// determines if sent or received.
		if (protocol == null) {
			Toast.makeText(mContext,
					"Sending to " + add + ". Time:" + time + " - " + body,
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext,
					"Receive from " + add + ". Time:" + time + " - " + body,
					Toast.LENGTH_SHORT).show();
		}

		cur.close();

		/* logging action HERE... */
	}

	public void sendSms(String phoneNumber, String message) {

		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0,
				new Intent(SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(mContext, 0,
				new Intent(DELIVERED), 0);

		// ---when the SMS has been sent---
		mContext.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {

				switch (getResultCode()) {
				case Activity.RESULT_OK: // message sent

					// plays the audio.
					TextToSpeechManager.getInstance().say(
							mContext.getResources().getString(
									R.string.message_sent));

					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:

					Toast.makeText(
							mContext,
							mContext.getResources().getString(
									R.string.message_sent_error),
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(
							mContext,
							mContext.getResources().getString(
									R.string.error_no_service),
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(mContext, "Null PDU", Toast.LENGTH_SHORT)
							.show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(mContext, "Radio off", Toast.LENGTH_SHORT)
							.show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		// ---when the SMS has been delivered---
		mContext.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					break;
				case Activity.RESULT_CANCELED:
					break;
				}
			}
		}, new IntentFilter(DELIVERED));

		// sends the message.
		SmsManager.getDefault().sendTextMessage(phoneNumber, null, message,
				sentPI, deliveredPI);
	}
}
