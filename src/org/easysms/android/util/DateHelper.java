package org.easysms.android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.easysms.android.R;

import android.content.Context;

/**
 * Helper functions to handle the date.
 * 
 * @author Oscar Bola–os <@oscarbolanos>
 * 
 */
public class DateHelper {

	/**
	 * Calculates the difference in days between two dates. To do so, it
	 * substracts the milliseconds between each date and then divides this value
	 * by the length of a day in milliseconds.
	 * 
	 * @param dateEarly
	 *            initial date to calculate
	 * @param dateLater
	 *            later date to calculate
	 * 
	 * @return differente in dayts between the given dates
	 */
	public static long calculateDays(Date dateEarly, Date dateLater) {
		return (dateLater.getTime() - dateEarly.getTime())
				/ (24 * 60 * 60 * 1000);
	}

	/**
	 * Formats the date. The format corresponds only to reference dates: today,
	 * yesterday, X days ago and X weeks ago.
	 * 
	 * @param date
	 *            the date to format.
	 * @param context
	 *            application context used for localization.
	 * 
	 * @return a string with the formatted date.
	 */
	public static String formatDate(Date date, Context context) {

		// calculates the difference
		long dayDif = calculateDays(date, new Date());

		if (dayDif == 0)
			return context.getString(R.string.date_today);
		else if (dayDif == 1)
			return context.getString(R.string.date_yesterday);
		else if (dayDif < 15)
			return String.format(context.getString(R.string.date_last_week),
					dayDif);
		else {
			return "";
			// return String.format(
			// context.getString(R.string.dateMoreThanAWeek),
			// (int) Math.floor(dayDif / 7));
		}
	}

	/**
	 * Returns a string that represents a short date of the given one. If the
	 * date is from today, the time is given, otherwise the day.
	 * 
	 * @param date
	 *            the date to convert to string.
	 * 
	 * @return a string with the date in short.
	 */
	public static String formatDateShort(Context context, Date date) {

		long dayDif = calculateDays(date, new Date());

		// returns time of message
		if (dayDif == 0) {
			return new SimpleDateFormat(context.getResources().getString(
					R.string.date_format_time)).format(date);
			// returns short date
		} else {
			return new SimpleDateFormat(context.getResources().getString(
					R.string.date_format_short)).format(date);
		}
	}

	public static String formatWithDay(Context context, String date) {

		try {

			// parses the giving date using the unified
			// date format.
			Date newDate = new SimpleDateFormat(context.getResources()
					.getString(R.string.date_format)).parse(date);

			// reformats the date only to extract the day of the week.
			return new SimpleDateFormat("EEEE").format(newDate);

		} catch (ParseException e) {
			return date;
		}
	}

	public static long getBeginningYear() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, 01);
		calendar.set(Calendar.MONTH, 00);
		return calendar.getTimeInMillis();
	}

	public static String getDateNow(Context context) {
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat(context
				.getResources().getString(R.string.date_format));
		formatter.setLenient(true);
		return formatter.format(currentDate.getTime()) + " 00:00:00";
	}

	public static String getDatePast(Context context, int offsetDays) {
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat(context
				.getResources().getString(R.string.date_format));
		formatter.setLenient(true);
		currentDate.add(Calendar.DATE, offsetDays);
		return formatter.format(currentDate.getTime()) + " 00:00:00";
	}

	public static boolean validDate(int day, int month, int year) {
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		try {
			c.set(year, month, day);
			// getTime() will produce an exception if the date is invalid.

			c.getTime();
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
}
