package org.easysms.android.view;

import java.util.HashMap;

import org.easysms.android.R;
import org.easysms.android.data.Conversation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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

	/** Icon that represents the main crop inside that plot. */
	private ImageView mContactImage;
	/** Description line of the plot. */
	private TextView mDisplayName;
	/** Number counter of the plot. */
	private TextView mCounter;
	/** The View object that represents a single row inside the ListView. */
	private View mRow;
	/** Title line of the plot. */
	private TextView mPhoneNumber;
	private TextView mMessage;

	private TextView mDate;

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

	public TextView getDisplayNameTextView() {
		if (mDisplayName == null) {
			mDisplayName = (TextView) mRow
					.findViewById(R.id.inbox_item_text_name);
		}
		return (mDisplayName);
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
		getDateTextView().setText((String) conversation.get("date"));
		// getContactImageView().setImageResource(converstaion.);
		getCounterTextView().setText((String) conversation.get("counter"));
		getMessageTextView().setText((String) conversation.get("message"));

	}
}
