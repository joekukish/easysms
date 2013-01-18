package org.easysms.android.data;

public class Sms {
	public String body;
	public String contact;
	public String datesms;
	public int read;
	public String sent;
	public String threadid;
	public String timesms;

	public Sms(String m_sent, String m_threadid, String m_datesms,
			String m_timesms, String m_contact, String m_body, int m_read) {
		sent = m_sent;
		threadid = m_threadid;
		datesms = m_datesms;
		timesms = m_timesms;
		contact = m_contact;
		body = m_body;
		read = m_read;
	}
}
