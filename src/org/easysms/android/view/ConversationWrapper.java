package org.easysms.android.view;

import org.easysms.android.R;
import org.easysms.android.data.Sms;
import org.easysms.android.ui.KaraokeLayout;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
	/** TextView that holds the date */
	private TextView mDate;
	/** TextView that holds the message. */
	private KaraokeLayout mMessage;
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

	public void populateFrom(int position, Sms message, Context context) {

		// sets the image if available
		if (message.image != null) {
			getContactImageView().setImageBitmap(message.image);
		} else {
			getContactImageView().setImageResource(R.drawable.nophotostored);
		}

		RelativeLayout.LayoutParams contactParams = (RelativeLayout.LayoutParams) getContactImageView()
				.getLayoutParams();
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mRow
				.findViewById(R.id.conversation_item_layout).getLayoutParams();

		// if message is sent.
		if (message.type == 2) {

			contactParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
			contactParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
			layoutParams.addRule(RelativeLayout.LEFT_OF,
					R.id.conversation_item_image_contact);
			layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
			getDateTextView().setGravity(Gravity.RIGHT);

		} else {

			contactParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
			contactParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
			layoutParams.addRule(RelativeLayout.LEFT_OF, 0);
			layoutParams.addRule(RelativeLayout.RIGHT_OF,
					R.id.conversation_item_image_contact);
			getDateTextView().setGravity(Gravity.LEFT);
		}

		// sets the new layout parameters.
		getContactImageView().setLayoutParams(contactParams);
		mRow.findViewById(R.id.conversation_item_layout).setLayoutParams(
				layoutParams);

		// sets the values of the conversation.
		getMessageTextView().setText(message.body);
		getDateTextView().setText(message.getDate(context));

	}
}
