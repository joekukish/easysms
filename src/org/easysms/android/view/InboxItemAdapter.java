package org.easysms.android.view;

import java.util.HashMap;
import java.util.List;

import org.easysms.android.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class InboxItemAdapter extends ArrayAdapter<HashMap<String, Object>> {

	/** Inflater used to create the layout based on the xml. */
	private LayoutInflater mLayoutInflater;

	/**
	 * Creates a new PlotItemAdapter instance.
	 */
	public InboxItemAdapter(Context context,
			List<HashMap<String, Object>> conversations) {
		super(context, android.R.layout.simple_list_item_1, conversations);

		mLayoutInflater = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		// view holder pattern user for performance.
		InboxItemWrapper wrapper = null;
		if (row == null) {

			row = mLayoutInflater.inflate(R.layout.tpl_inbox_item, parent,
					false);
			wrapper = new InboxItemWrapper(row);
			row.setTag(wrapper);
		} else {
			wrapper = (InboxItemWrapper) row.getTag();
		}

		wrapper.populateFrom(position, getItem(position));
		return (row);
	}
}
