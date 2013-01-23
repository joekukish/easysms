package org.easysms.android.data;

import java.util.Calendar;
import java.util.Date;

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

		final Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		String dateToday = mDay + "-" + mMonth + "-" + mYear;

		String datestring = (String) android.text.format.DateFormat.format(
				"dd-MM-yyyy", date);
		String timestring = (String) android.text.format.DateFormat.format(
				"kk:mm", date);
		
		// dateTimeString = (String) android.text.format.DateFormat
		// .format("yyyy-MM-dd'T'kk:mm:ss'Z'", datesms);
		// datestring = (String) android.text.format.DateFormat
		// .format("yyyy-MM-dd", datesms);
		// timestring = (String) android.text.format.DateFormat
		// .format("kk:mm", datesms);

		return "";
	}
}
