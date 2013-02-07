package org.easysms.android.data;

import java.util.Date;

import org.easysms.android.util.DateHelper;

import android.content.Context;
import android.graphics.Bitmap;

public class Sms {
	public String body;
	public String address;
	public Date date;
	public String person;
	public Boolean isRead;
	public int type;
	public String threadid;
	public Bitmap image;
	public String protocol;

	public Sms(String m_threadid, Date m_date, String m_address, String m_body) {
		threadid = m_threadid;
		date = m_date;
		address = m_address;
		body = m_body;

		// initial values.
		isRead = false;
	}

	public String getDate(Context context) {
		return DateHelper.formatDateShort(context, date);
	}
}
