package org.easysms.android;

import android.app.Application;
import android.provider.Settings.Secure;

public class EasySmsApp extends Application {
	/**
	 * Defines the default device id which is used in case the data is not
	 * available.
	 */
	public static final String DEFAULT_DEVICE_ID = "#default_id#";
	/** Defines the theme that will be used. */
	public static final int THEME = R.style.Theme_Sherlock_Light_DarkActionBar;

	/** DeviceId of the current phone where the App is running. */
	private String mDeviceId;

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
