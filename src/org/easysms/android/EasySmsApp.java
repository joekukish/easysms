package org.easysms.android;

import org.easysms.android.provider.SmsContentProvider;

import android.app.Application;
import android.provider.Settings.Secure;

public class EasySmsApp extends Application {
	/**
	 * Defines the default device id which is used in case the data is not
	 * available.
	 */
	public static final String DEFAULT_DEVICE_ID = "#default_id#";
	/** Defines the theme that will be used. */

	/** Provider used to handle read and send SMS. */
	private SmsContentProvider mContentProvider;
	/** DeviceId of the current phone where the application is running. */
	private String mDeviceId;

	/**
	 * Creates a new EasySmsApp instance.
	 * 
	 * The content provider is initialized and register as an SMS observer.
	 */
	public EasySmsApp() {
		super();

		// initializes the content provider.
		mContentProvider = new SmsContentProvider(this);
	}

	public SmsContentProvider getContentProvider() {
		return mContentProvider;
	}

	/**
	 * Gets the DeviceId from the Telephony Manager. If the value is not
	 * available a default value is set.
	 * 
	 * @return the current DeviceId
	 */
	public String getDeviceId() {

		// if the value is null it is table from the database.
		if (mDeviceId == null || mDeviceId.equals("")) {

			mDeviceId = Secure.getString(getContentResolver(),
					Secure.ANDROID_ID);

			// sets the default value if invalid.
			if (mDeviceId == null || mDeviceId.equals("")) {
				mDeviceId = DEFAULT_DEVICE_ID;
			}
		}

		return mDeviceId;
	}
}
