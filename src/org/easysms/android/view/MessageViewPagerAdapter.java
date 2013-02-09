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
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
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

		if (mVoiceOptions != options) {
			mVoiceOptions = options;

			// indicates that size must be modified.
			this.notifyDataSetChanged();
		}
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

			// thread id from the active conversation.
			final long convThreadId = mParent.getThreadId();

			// only continues if valid conversation is loaded.
			if (convThreadId == -1) {
				break;
			}

			final Conversation conv = new Conversation();
			conv.listsms = new ArrayList<Sms>();
			conv.threadid = convThreadId;

			// creates the new conversation adapter and prepares all the
			// corresponding listeners.
			final ConversationAdapter convAdapter = new ConversationAdapter(
					mParent, conv);

			convAdapter
					.setOnKaraokeClickListener(new KaraokeLayout.OnKaraokeClickListener() {

						@Override
						public void onClick(Button button) {

							// tracks the user activity.
							ApplicationTracker.getInstance().logEvent(
									EventType.CLICK, mParent,
									"conversation_bubble_word",
									button.getText());
							// tracks using google analytics.
							EasyTracker.getTracker().sendEvent("ui_action",
									"button_press", "conversation_bubble_word",
									null);
						}
					});

			convAdapter
					.setOnKaraokeLongClickListener(new KaraokeLayout.OnKaraokeLongClickListener() {

						@Override
						public boolean onLongClick(Button button) {
							// tracks the user activity.
							ApplicationTracker.getInstance().logEvent(
									EventType.LONG_CLICK, mParent,
									"conversation_bubble_word",
									button.getText());
							// tracks using google analytics.
							EasyTracker.getTracker().sendEvent("ui_action",
									"button_long_press",
									"conversation_bubble_word", null);

							// adds the text inside the button to the compose
							// bubble.
							mParent.addTextToMessage(button.getText()
									.toString());

							return true;
						}
					});

			convAdapter
					.setOnKaraokePlayButtonListener(new KaraokeLayout.OnKaraokePlayButtonClickListener() {

						@Override
						public boolean onPlayClick() {
							// tracks the user activity.
							ApplicationTracker.getInstance().logEvent(
									EventType.CLICK, mParent,
									"conversation_bubble_play");
							// tracks using google analytics.
							EasyTracker.getTracker().sendEvent("ui_action",
									"button_press", "conversation_bubble_play",
									null);
							return true;
						}
					});

			// sets the conversation thread.
			conversationList.setAdapter(convAdapter);

			// handles the content loading in a separate thread
			new Handler().post(new Runnable() {
				public void run() {

					// gets all the messages of the same thread.
					conv.listsms.addAll(mParent.getContentProvider()
							.getConversationByThreadId(convThreadId).listsms);

					prepareConversation(conv);

					// notifies that the UI needs to be updated.
					convAdapter.notifyDataSetChanged();
				}
			});

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

			instructionsBubble
					.setOnKaraokeClickListener(new KaraokeLayout.OnKaraokeClickListener() {

						@Override
						public void onClick(Button button) {

							// tracks the user activity.
							ApplicationTracker.getInstance().logEvent(
									EventType.CLICK, mParent,
									"voice_instructions_bubble_word",
									button.getText());
							// tracks using google analytics.
							EasyTracker.getTracker().sendEvent("ui_action",
									"button_press",
									"voice_instructions_bubble_word", null);
						}
					});

			instructionsBubble
					.setOnKaraokeLongClickListener(new KaraokeLayout.OnKaraokeLongClickListener() {

						@Override
						public boolean onLongClick(Button button) {
							// tracks the user activity.
							ApplicationTracker.getInstance().logEvent(
									EventType.LONG_CLICK, mParent,
									"voice_instructions_bubble_word",
									button.getText());
							// tracks using google analytics.
							EasyTracker.getTracker().sendEvent("ui_action",
									"button_long_press",
									"voice_instructions_bubble_word", null);

							// adds the text inside the button to the compose
							// bubble.
							mParent.addTextToMessage(button.getText()
									.toString());

							return true;
						}
					});

			instructionsBubble
					.setOnKaraokePlayButtonClickListener(new KaraokeLayout.OnKaraokePlayButtonClickListener() {

						@Override
						public boolean onPlayClick() {
							// tracks the user activity.
							ApplicationTracker.getInstance().logEvent(
									EventType.CLICK, mParent,
									"voice_instructions_bubble_play");
							// tracks using google analytics.
							EasyTracker.getTracker().sendEvent("ui_action",
									"button_press",
									"voice_instructions_bubble_play", null);
							return true;
						}
					});

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
				final KaraokeLayout kl = new KaraokeLayout(mParent);

				// add the view number
				ImageView number = new ImageView(mParent);
				if (i == 0) {
					number.setBackgroundResource(R.drawable.one);
					number.setContentDescription("one");
				} else if (i == 1) {
					number.setBackgroundResource(R.drawable.two);
					number.setContentDescription("two");
				} else if (i == 2) {
					number.setBackgroundResource(R.drawable.three);
					number.setContentDescription("three");
				}

				// adds the view to the layout.
				kl.addView(number);

				number.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.CLICK, mParent,
								"voice_option_number",
								v.getContentDescription(), kl.getText());
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_press", "voice_option_number", null);

					}
				});
				number.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.LONG_CLICK, mParent,
								"voice_option_number",
								v.getContentDescription(), kl.getText());
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_long_press", "voice_option_number",
								null);

						// adds the text to the compose layout.
						mParent.addTextToMessage(kl.getText());
						return true;
					}
				});

				// adds the sentence to the layout.
				kl.setText(mVoiceOptions.get(i));

				kl.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.LONG_CLICK, mParent,
								"voice_option_bubble", kl.getText());
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_long_press", "voice_option_bubble",
								null);

						// adds the text to the compose layout.
						mParent.addTextToMessage(kl.getText());
						return true;
					}
				});

				kl.setOnKaraokeClickListener(new KaraokeLayout.OnKaraokeClickListener() {

					@Override
					public void onClick(Button button) {
						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.CLICK, mParent,
								"voice_option_bubble_word", button.getText());
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_press", "voice_option_bubble_word",
								null);
					}
				});

				kl.setOnKaraokeLongClickListener(new KaraokeLayout.OnKaraokeLongClickListener() {

					@Override
					public boolean onLongClick(Button button) {

						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.LONG_CLICK, mParent,
								"voice_option_bubble_word", button.getText());
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_long_press",
								"voice_option_bubble_word", null);

						// adds the text to the compose layout.
						mParent.addTextToMessage(button.getText().toString());

						return true;
					}
				});

				kl.setOnKaraokePlayButtonClickListener(new KaraokeLayout.OnKaraokePlayButtonClickListener() {

					@Override
					public boolean onPlayClick() {
						// tracks the user activity.
						ApplicationTracker.getInstance().logEvent(
								EventType.CLICK, mParent,
								"voice_option_bubble_play");
						// tracks using google analytics.
						EasyTracker.getTracker().sendEvent("ui_action",
								"button_press", "voice_option_bubble_play",
								null);
						return true;
					}
				});

				// adds the option to the view.
				optionsLayout.addView(kl);
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
}
