package org.easysms.android.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.easysms.android.R;
import org.easysms.android.data.Sms;
import org.easysms.android.util.TextToSpeechManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsContentProvider extends ContentObserver {

	/** Context used to query the SMS. */
	private Context mContext;
	/** List of cached messages. */
	private ArrayList<Sms> mMessages;

	public SmsContentProvider(Context context) {
		super(new Handler());
		mContext = context;
	}

	public void sendSMS(String phoneNumber, String message) {

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

					// vocal feedback when message sent
					String sentence = mContext.getResources().getString(
							R.string.message_sent);

					// plays the audio.
					TextToSpeechManager.getInstance().say(sentence);

					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(mContext, "Erreur d'envoi du message",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(mContext, "Erreur, pas de  service",
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

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
	}

	public String getContactNameFromNumber(String number) {
		/*
		 * We have a phone number and we want to grab the name of the contact
		 * with that number, if such a contact exists
		 */
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));

		/* phoneNumber here being a variable with the phone number stored. */
		Cursor c = mContext.getContentResolver().query(lookupUri,
				new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		/*
		 * If we want to get something other than the displayed name for the
		 * contact, then just use something else instead of DISPLAY_NAME
		 */
		String name = "";
		while (c.moveToNext()) {
			// if we find a match we put it in a String.
			name = c.getString(c
					.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
		}
		c.close();
		return name;

	}

	public String getContactPhotoFromNumber(String number) {
		/*
		 * We have a phone number and we want to grab the name of the contact
		 * with that number, if such a contact exists
		 */
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		/* phoneNumber here being a variable with the phone number stored. */
		Cursor c = mContext.getContentResolver().query(lookupUri,
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

	public Bitmap getContactPhotoWithId(String photoId) {
		Cursor photo2 = mContext.getContentResolver().query(Data.CONTENT_URI,
				new String[] { Photo.PHOTO }, // column for the blob
				Data._ID + "=?", // select row by id
				new String[] { photoId }, // filter by photoId
				null);
		Bitmap photoBitmap = null;
		if (photo2.moveToFirst()) {
			byte[] photoBlob = photo2.getBlob(photo2
					.getColumnIndex(Photo.PHOTO));
			photoBitmap = BitmapFactory.decodeByteArray(photoBlob, 0,
					photoBlob.length);
		}
		photo2.close();

		return photoBitmap;
	}

	public synchronized List<Sms> getMessages() {

		if (mMessages == null) {

			// we put all the SMS sent and received in a list
			mMessages = new ArrayList<Sms>();

			Cursor curinbox = mContext.getContentResolver().query(
					Uri.parse("content://sms/"), null, null, null, null);

			// gets all the available messages.
			if (curinbox.moveToFirst()) {
				long datesms = 0;
				String phoneNumber = null;
				String body = null;
				String threadid = null;
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

					Sms sms;

					if (phoneNumber != null) {
						sms = new Sms(threadid, new Date(datesms), phoneNumber,
								body);

						// to know if it is a message sent or received
						if (type == 2) { // SENT
							sms.isSent = true;
						} else if (type == 1) { // INBOX
							sms.isSent = false;
						}

						// indicates whether it was read or not.
						sms.isRead = read == 1;

						// we add this SMS to the list of all the SMS
						mMessages.add(sms);
					}

				} while (curinbox.moveToNext());
			}

			curinbox.close();
		}

		return mMessages;
	}

	@Override
	public void onChange(boolean selfChange) {

		super.onChange(selfChange);

		// resets the message list.
		mMessages = null;
		querySMS();
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
					"Sending to " + add + ".Time:" + time + " - " + body,
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext,
					"Receive from " + add + ".Time:" + time + " - " + body,
					Toast.LENGTH_SHORT).show();
		}

		cur.close();

		/* logging action HERE... */
	}
}
