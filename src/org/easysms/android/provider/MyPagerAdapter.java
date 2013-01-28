package org.easysms.android.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.easysms.android.MessageActivity;
import org.easysms.android.R;
import org.easysms.android.ui.KaraokeLayout;
import org.easysms.android.util.TextToSpeechManager;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;

public class MyPagerAdapter extends PagerAdapter {

	/**
	 * Parent reference used to indicate interaction with the pages of the
	 * ViewPager.
	 */
	private MessageActivity mParent;

	public int getCount() {
		return 3;
	}

	public MyPagerAdapter(MessageActivity parent) {
		mParent = parent;
	}

	// references to our images
	private Integer[] mThumbIds = { R.drawable.ok, R.drawable.no,
			R.drawable.late, R.drawable.callme, R.drawable.nobattery,
			R.drawable.busy, R.drawable.hurryup, R.drawable.howareu,
			R.drawable.loveu, R.drawable.driving, R.drawable.whatdate,
			R.drawable.whattime };

	public Object instantiateItem(View collection, int position) {

		LayoutInflater inflater = (LayoutInflater) collection.getContext()

		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = null;

		switch (position) {
		case 0:
			view = inflater.inflate(R.layout.layout_conversation_list, null);

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
			int itemCount = Math.min(labels.length, mThumbIds.length);

			for (int i = 0; i < itemCount; i++) {
				HashMap<String, String> hm = new HashMap<String, String>();
				hm.put("txt", labels[i]);
				hm.put("flag", Integer.toString(mThumbIds[i]));
				hm.put("sentense", text[i]);
				aList.add(hm);
			}

			// Keys used in HashMap
			String[] from = { "flag", "txt" };

			// IDs of views in listview_layout
			int[] to = { R.id.flag, R.id.txt };

			// Instantiating an adapter to store each items
			// R.layout.listview_layout defines the layout of each item
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

					// adds the text to the message activity.
					mParent.addTextToMessage(clickedItem.get("sentense"));

					return true;

				}
			});

			break;
		case 2:
			view = inflater.inflate(R.layout.layout_speech_recognition, null);

			// gets the bubble
			KaraokeLayout instructionsBubble = (KaraokeLayout) view
					.findViewById(R.id.layout_speech_recognition_instructions_bubble);

			// adds the help message.
			instructionsBubble.addText(view.getResources().getString(
					R.string.voice_help));

			break;
		}

		((ViewPager) collection).addView(view, 0);
		return view;
	}

	@Override
	public void destroyItem(View arg0, int arg1, Object arg2) {
		((ViewPager) arg0).removeView((View) arg2);
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0 == ((View) arg1);
	}

	@Override
	public Parcelable saveState() {
		return null;
	}
}
