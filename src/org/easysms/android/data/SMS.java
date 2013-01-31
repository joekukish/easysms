package org.easysms.android.data;

import java.util.Date;

import org.easysms.android.util.DateHelper;

public class Sms {
	public String body;
	public String contact;
	public Date date;
	public Boolean isRead;
	public Boolean isSent;
	public String threadid;

	public Sms(String m_threadid, Date m_datesms, String m_contact,
			String m_body) {
		threadid = m_threadid;
		date = m_datesms;
		contact = m_contact;
		body = m_body;

		// initial values.
		isSent = false;
		isRead = false;
	}

	public String getDate() {
		return DateHelper.formatDateShort(date);
	}
}
