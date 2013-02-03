package org.easysms.android.view;

import org.easysms.android.R;
import org.easysms.android.data.Sms;
import org.easysms.android.ui.KaraokeLayout;

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
public class ConversationWrapper {

	/** ImageView that holds the Contact's Image. */
	private ImageView mContactImage;
	/** TextView that holds the message. */
	private KaraokeLayout mMessage;
	/** TextView that holds the date */
	private TextView mDate;
	/** The View object that represents a single row inside the ListView. */
	private View mRow;

	/**
	 * Creates a new ConversationWrapper instance.
	 * 
	 * @param row
	 *            the View where the info will be presented.
	 */
	public ConversationWrapper(View row) {
		mRow = row;
	}

	public ImageView getContactImageView() {
		if (mContactImage == null) {
			mContactImage = (ImageView) mRow
					.findViewById(R.id.conversation_item_image_contact);
		}
		return (mContactImage);
	}

	public TextView getDateTextView() {
		if (mDate == null) {
			mDate = (TextView) mRow
					.findViewById(R.id.conversation_item_text_date);
		}
		return (mDate);
	}

	public KaraokeLayout getMessageTextView() {
		if (mMessage == null) {
			mMessage = (KaraokeLayout) mRow
					.findViewById(R.id.conversation_item_karaoke_message);
		}
		return (mMessage);
	}

	public void populateFrom(int position, Sms message) {

		// getContactImageView().setImageBitmap(bm)
		getMessageTextView().addText(message.body);
		getDateTextView().setText(message.getDate());

	}
}
