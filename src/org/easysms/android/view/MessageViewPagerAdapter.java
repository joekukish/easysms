package org.easysms.android.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.easysms.android.MessageActivity;
import org.easysms.android.R;
import org.easysms.android.data.Conversation;
import org.easysms.android.data.Sms;
import org.easysms.android.ui.KaraokeLayout;
import org.easysms.android.util.ApplicationTracker;
import org.easysms.android.util.ApplicationTracker.EventType;
import org.easysms.android.util.TextToSpeechManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.analytics.tracking.android.EasyTracker;

public class MessageViewPagerAdapter extends PagerAdapter {

	/**
	 * List of drawables used in the grid.
	 */
	private static final Integer[] THUMBNAIL_IDS = { R.drawable.ok,
			R.drawable.no, R.drawable.late, R.drawable.callme,
			R.drawable.nobattery, R.drawable.busy, R.drawable.hurryup,
			R.drawable.howareu, R.drawable.loveu, R.drawable.driving,
			R.drawable.whatdate, R.drawable.whattime };

	/**
	 * Parent reference used to indicate interaction with the pages of the
	 * ViewPager.
	 */
	private MessageActivity mParent;
	/** Text options as a result of the Voice Recognition. */
	private List<String> mVoiceOptions;

	/**
	 * Creates a new MessageViewPagerAdapter instance.
	 * 
	 * @param parent
	 *            parent activity where any change will be notified.
	 */
	public MessageViewPagerAdapter(MessageActivity parent) {
		mParent = parent;
	}

	@Override
	public void destroyItem(View arg0, int arg1, Object arg2) {
		((ViewPager) arg0).removeView((View) arg2);
	}

	public void displayVoiceOptions(List<String> options) {

		mVoiceOptions = options;
		// indicates that size must be modified.
		this.notifyDataSetChanged();
	}

	public int getCount() {
		return mVoiceOptions == null ? 3 : 4;
	}

	public Object instantiateItem(View collection, int position) {

		LayoutInflater inflater = (LayoutInflater) collection.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = null;

		switch (position) {
		case 0:

			view = inflater.inflate(R.layout.layout_conversation_list, null);
			ListView conversationList = (ListView) view
					.findViewById(R.id.conversation_list_list);

			// gets all the SMS of the conversation, that match the same
			// phone number.
			List<Conversation> listallconv = populateList(mParent
					.getContentProvider().getMessages());
			String threadidconv = retrieveThreadIdFromNumberContact(mParent
					.getContactPhonenumber());
			Conversation conv = retrieveConvFromThreadId(listallconv,
					threadidconv);

			prepareConversation(conv);

			// sets the conversation thread.
			conversationList.setAdapter(new ConversationAdapter(mParent, conv));

			break;
		case 1:

			// creates the page that contains the icons.
			view = inflater.inflate(R.layout.layout_icon_grid, null);

			// Each row in the list stores country name, currency and flag
			final List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();

			String[] labels = collection.getResources().getStringArray(
					R.array.icons_array);

			String[] text = collection.getResources().getStringArray(
					R.array.sentenses_array);

			// prevents any index out of bounds exception.
			int itemCount = Math.min(labels.length, THUMBNAIL_IDS.length);

			for (int i = 0; i < itemCount; i++) {
				HashMap<String, String> hm = new HashMap<String, String>();
				hm.put("txt", labels[i]);
				hm.put("flag", Integer.toString(THUMBNAIL_IDS[i]));
				hm.put("sentense", text[i]);
				aList.add(hm);
			}

			// Keys used in HashMap
			final String[] from = { "flag", "txt" };

			// IDs of views in listview_layout
			final int[] to = { R.id.flag, R.id.txt };

			// adapter that can populate the contents of the grid.
			SimpleAdapter adapter = new SimpleAdapter(view.getContext(), aList,
					R.layout.tpl_grid_view_item, from, to);

			// Getting a reference to GridView of MainActivity
			GridView gridView = (GridView) view
					.findViewById(R.id.layout_icon_grid_view);

			// Setting an adapter containing images to the GridView
			gridView.setAdapter(adapter);

			// adds the listeners
			gridView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v,
						int position, long id) {

					// gets the clicked item.
					HashMap<String, String> clickedItem = aList.get(position);

					// tracks the user activity.
					ApplicationTracker.getInstance().logEvent(EventType.CLICK,
							this, "quick_item", clickedItem.get("txt"),
							position);
					// tracks using google analytics.
					EasyTracker.getTracker().sendEvent("ui_action",
							"quick_item_press", clickedItem.get("txt"),
							(long) position);

					// plays the text associated to the audio.
					TextToSpeechManager.getInstance().say(
							clickedItem.get("sentense"));
				}
			});

			gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
				public boolean onItemLongClick(AdapterView<?> parent, View v,
						int position, long id) {

					// gets the clicked item.
					HashMap<String, String> clickedItem = aList.get(position);

					// tracks the user activity.
					ApplicationTracker.getInstance().logEvent(
							EventType.LONG_CLICK, this, "quick_item",
							clickedItem.get("txt"), position);
					// tracks using google analytics.
					EasyTracker.getTracker().sendEvent("ui_action",
							"quick_item_long_press", clickedItem.get("txt"),
							(long) position);

					// adds the text to the message activity.
					mParent.addTextToMessage(clickedItem.get("sentense"));

					return true;

				}
			});

			break;
		case 2:

			// inflates the layout
			view = inflater.inflate(
					R.layout.layout_speech_recognition_instructions, null);

			// gets the bubble
			KaraokeLayout instructionsBubble = (KaraokeLayout) view
					.findViewById(R.id.speech_recognition_karaoke_instructions);

			// adds the help message.
			instructionsBubble.setText(view.getResources().getString(
					R.string.voice_help));
			break;

		case 3:

			// inflates the layout
			view = inflater.inflate(R.layout.layout_speech_recognition_options,
					null);
			LinearLayout optionsLayout = (LinearLayout) view
					.findViewById(R.id.speech_recognition_options_list);

			// iterates the options.
			for (int i = 0; i < mVoiceOptions.size(); i++) {

				// create a new flow layout for each choice
				final KaraokeLayout fl = new KaraokeLayout(mParent);

				// add the view number
				ImageView number = new ImageView(mParent);
				if (i == 0) {
					number.setBackgroundResource(R.drawable.one);
					fl.addView(number);

				} else if (i == 1) {
					number.setBackgroundResource(R.drawable.two);
					fl.addView(number);

				} else if (i == 2) {
					number.setBackgroundResource(R.drawable.three);
					fl.addView(number);
				}

				number.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						mParent.addTextToMessage(fl.getText());
						return true;
					}
				});

				// adds the sentence to the layout.
				fl.setText(mVoiceOptions.get(i));

				fl.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						mParent.addTextToMessage(fl.getText());
						return true;
					}
				});

				// adds the option to the view.
				optionsLayout.addView(fl);
			}

			break;
		}

		((ViewPager) collection).addView(view, 0);
		return view;
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0 == ((View) arg1);
	}

	private List<Conversation> populateList(List<Sms> allSMS) {

		// list with all the conversations
		List<Conversation> allconversations = new ArrayList<Conversation>();
		for (Sms smsnew : allSMS) {
			boolean add = false;
			for (Conversation conv : allconversations) {
				if (conv.threadid.equals(smsnew.threadid)) {
					conv.listsms.add(smsnew);
					add = true;
				}
			}
			if (add == false) { // we create a new conversation
				Conversation newconv = new Conversation();
				List<Sms> newlist = new ArrayList<Sms>();
				newlist.add(smsnew);
				newconv.listsms = newlist;
				newconv.threadid = smsnew.threadid;
				allconversations.add(newconv);
			}
		}
		return allconversations;
	}

	/**
	 * Fills the bitmap objects of the whole conversation.
	 * 
	 * @param conv
	 *            the conversation to process.
	 */
	private void prepareConversation(Conversation conv) {

		Bitmap photo;
		for (int x = 0; x < conv.listsms.size(); x++) {
			// loads the photo bitmap.
			photo = mParent.getContentProvider().getContactPhoto(
					conv.listsms.get(x).address);
			if (photo != null) {
				conv.listsms.get(x).image = photo;
			}
		}
	}

	public Conversation retrieveConvFromThreadId(List<Conversation> allConv,
			String threadid) {
		for (Conversation conv : allConv) {
			String convthreadid = conv.threadid;
			if (convthreadid.equals(threadid)) {
				return conv;
			}
		}

		return null;
	}

	public String retrieveThreadIdFromNumberContact(String phoneNumContact) {
		for (Sms sms : mParent.getContentProvider().getMessages()) {
			String smscontact = sms.address;
			// TODO: could it really be null?
			if (smscontact != null && smscontact.equals(phoneNumContact))
				return sms.threadid;
		}
		return "error";
	}
}
