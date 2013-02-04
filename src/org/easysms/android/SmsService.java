package org.easysms.android;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

public class SmsService extends Service {

	class MyContentObserver extends ContentObserver {

		public MyContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			Toast.makeText(getBaseContext(),
					"Didn't call onChange()( " + selfChange + ")",
					Toast.LENGTH_SHORT).show();

			super.onChange(selfChange);
		}
	}

	/** URI of the SMS content provider. */
	public static final String ALL_MESSAGES = "content://sms/";

	/** Handles when a new message arrives. */
	private static Handler mMessageHandler;

	public static Handler getMessageHandler() {
		if (mMessageHandler == null) {
			mMessageHandler = new Handler() {
				public void handleMessage(Message mgs) {

					System.out.println("**** New Message *****");
					System.out.println(mgs);

					// Toast.makeText(getBaseContext(),
					// "Didn't call onChange()" + mgs, Toast.LENGTH_SHORT)
					// .show();
				}
			};
		}
		return mMessageHandler;
	}

	/** Observer in charge of handling any change in the SMS list. */
	private ContentObserver mContentObserver;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// observer that handles any change.
		mContentObserver = new MyContentObserver(getMessageHandler());

		// registers the observer in charge of notifying any change in the SMS.
		this.getApplicationContext()
				.getContentResolver()
				.registerContentObserver(Uri.parse(ALL_MESSAGES), true,
						mContentObserver);
	}

	@Override
	public void onDestroy() {

		// removes the observer.
		if (mContentObserver != null) {
			this.getApplicationContext().getContentResolver()
					.unregisterContentObserver(mContentObserver);
		}

		super.onDestroy();
	}
}
