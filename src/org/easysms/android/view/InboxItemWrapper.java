package org.easysms.android.view;

import java.util.HashMap;

import org.easysms.android.R;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Class that wraps up the contents of a Plot, which is presented on a list
 * adapter.
 * 
 * @author Oscar Bola–os <@oscarbolanos>
 * 
 */
public class InboxItemWrapper {

	/** ImageView that holds the Contact's Image. */
	private ImageView mContactImage;
	/** TextView that holds the amount of messages inside the conversation. */
	private TextView mCounter;
	/** TextView that holds the Date of the last message. */
	private TextView mDate;
	/** TextView that holds the Display name. */
	private TextView mDisplayName;
	/** TextView that holds the last message. */
	private TextView mMessage;
	/** TextView that holds the phone number */
	private TextView mPhoneNumber;
	/** The View object that represents a single row inside the ListView. */
	private View mRow;

	/**
	 * Creates a new PlotItemWrapper instance.
	 * 
	 * @param row
	 *            the View where the info will be presented.
	 */
	public InboxItemWrapper(View row) {
		mRow = row;
	}

	public ImageView getContactImageView() {
		if (mContactImage == null) {
			mContactImage = (ImageView) mRow
					.findViewById(R.id.inbox_item_image_contact);
		}
		return (mContactImage);
	}

	public TextView getCounterTextView() {
		if (mCounter == null) {
			mCounter = (TextView) mRow
					.findViewById(R.id.inbox_item_text_counter);
		}
		return (mCounter);
	}

	public TextView getDateTextView() {
		if (mDate == null) {
			mDate = (TextView) mRow.findViewById(R.id.inbox_item_text_date);
		}
		return (mDate);
	}

	public TextView getDisplayNameTextView() {
		if (mDisplayName == null) {
			mDisplayName = (TextView) mRow
					.findViewById(R.id.inbox_item_text_name);
		}
		return (mDisplayName);
	}

	public TextView getMessageTextView() {
		if (mMessage == null) {
			mMessage = (TextView) mRow
					.findViewById(R.id.inbox_item_text_message);
		}
		return (mMessage);
	}

	public TextView getPhoneNumber() {
		if (mPhoneNumber == null) {
			mPhoneNumber = (TextView) mRow
					.findViewById(R.id.inbox_item_text_phonenumber);
		}
		return (mPhoneNumber);
	}

	public void populateFrom(int position, HashMap<String, Object> conversation) {

		// count is only set if the value is bigger than one.
		if ((Integer) conversation.get("count") > 1) {
			getCounterTextView().setText("" + conversation.get("count"));
		} else {
			getCounterTextView().setText("");
		}

		if (conversation.get("avatar") != null) {
			getContactImageView().setImageBitmap(
					(Bitmap) conversation.get("avatar"));
		} else {
			getContactImageView().setImageResource(R.drawable.nophotostored);
		}

		if (conversation.get("name") == null) {
			getDisplayNameTextView().setText(
					(String) conversation.get("telnumber"));
			getPhoneNumber().setVisibility(View.GONE);
			getMessageTextView().setSingleLine(false);
		} else {
			// sets the template values.
			getPhoneNumber().setText((String) conversation.get("telnumber"));
			getDisplayNameTextView().setText((String) conversation.get("name"));
			getMessageTextView().setSingleLine(true);
		}

		// sets the rest of the values.
		getDateTextView().setText((String) conversation.get("date"));
		getMessageTextView().setText((String) conversation.get("message"));

		// changes the background color if it is a new message.
		if (!(Boolean) conversation.get("read")) {
			mRow.setBackgroundColor(mRow.getResources().getColor(
					android.R.color.white));
		}
	}
}
