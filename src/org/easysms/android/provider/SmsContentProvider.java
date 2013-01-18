package org.easysms.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;

public class SmsContentProvider extends ContentProvider {

	public String getContactNameFromNumber(String number) {
		/*
		 * We have a phone number and we want to grab the name of the contact
		 * with that number, if such a contact exists
		 */
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		/* phoneNumber here being a variable with the phone number stored. */
		Cursor c = getContext().getContentResolver().query(lookupUri,
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
		Cursor c = getContext().getContentResolver().query(lookupUri,
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
		return photoId;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
}
