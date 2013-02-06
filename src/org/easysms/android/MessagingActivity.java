package org.easysms.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

@TargetApi(8)
public class MessagingActivity extends SherlockActivity {

	private static final int PICK_CONTACT = 4321;

	private String phoneNo = "";
	private TextView recipient;
	private TextView recipientnum;

	/**
	 * Handle the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK) {

			phoneNo = "";
			Uri contactData = data.getData();
			Cursor cur = getContentResolver().query(contactData, null, null,
					null, null);
			String nameContact = "Nom inconnu";
			String photoId = null;
			if (cur.moveToFirst()) {
				String id = cur.getString(cur.getColumnIndexOrThrow(Phone._ID));
				// contact name
				nameContact = cur
						.getString(cur
								.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
				// photoID
				long photo = cur
						.getLong(cur
								.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));
				String no = "NumŽro inconnu";
				// if the contact has a phone number
				if (Integer
						.parseInt(cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor pCur = getContentResolver().query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID
									+ " = ?", new String[] { id }, null);
					// this second loop will retrieve all the contact
					// numbers for a particular contact id
					String mobilePhone = "Inconnu";
					String homePhone = "Inconnu";
					String workPhone = "Inconnu";
					String otherPhone = "Inconnu";

					if (pCur != null) {
						while (pCur.moveToNext()) {
							// takes only the MOBILE number
							if (pCur.getInt(pCur.getColumnIndex(Phone.TYPE)) == Phone.TYPE_MOBILE) {
								/*
								 * int phNumber = pCur.getColumnIndexOrThrow(
								 * ContactsContract
								 * .CommonDataKinds.Phone.NUMBER); no =
								 * pCur.getString(phNumber);
								 */
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
						if (mobilePhone != "Inconnu") {
							no = mobilePhone;
						}
					}

					pCur.close();
				}

				// reinitialise le textview
				recipient.setText("");
				recipientnum.setText("");
				// append new name selected
				recipient.append(nameContact);
				recipientnum.append(no);
				phoneNo += no;

				id = null;
				nameContact = null;
				no = null;

				cur.close();
				cur = null;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// checks the bundle to handle correctly the two cases.
		Bundle bundle = getIntent().getExtras();
		Boolean newMsg = bundle.getBoolean(MessageActivity.EXTRA_NEW_MESSAGE);

		if (newMsg) { // if new message don't display the message details page
			setContentView(R.layout.act_new_message);

			// Intent intent = new Intent(Intent.ACTION_PICK,
			// ContactsContract.Contacts.CONTENT_URI);
			// startActivityForResult(intent, PICK_CONTACT);

			recipient = (TextView) findViewById(R.id.contactname);
			recipientnum = (TextView) findViewById(R.id.contactnumber);

		}
	}
}
