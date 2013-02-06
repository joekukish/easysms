package org.easysms.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

@TargetApi(8)
public class MessagingActivity extends SherlockActivity {

	private String phoneNo = "";
	private TextView recipient;
	private TextView recipientnum;

	/**
	 * Handle the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		

		super.onActivityResult(requestCode, resultCode, data);
	}
}
