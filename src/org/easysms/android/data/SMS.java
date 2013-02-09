package org.easysms.android.data;

import java.util.Date;

import org.easysms.android.util.DateHelper;

import android.content.Context;
import android.graphics.Bitmap;

public class Sms {
	public String address;
	public String body;
	public Date date;
	public Bitmap image;
	public Boolean isRead;
	public String person;
	public String protocol;
	public long threadid;
	public int type;

	public Sms(long m_threadid, Date m_date, String m_address, String m_body) {
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
