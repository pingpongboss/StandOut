package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class StandOutMultiWindow extends StandOutWindow {

	@Override
	protected String getAppName() {
		return "MultiWindow";
	}

	@Override
	protected int getAppIcon() {
		return android.R.drawable.ic_menu_add;
	}

	// we need to create a view for the window body
	@Override
	protected View createAndAttachView(int id, ViewGroup root) {
		switch (id) {
			default:
				// create a new layout from body.xml
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.body, root, true);

				TextView idText = (TextView) view.findViewById(R.id.id);
				idText.setText(String.valueOf(id));

				return view;
		}
	}

	// every window is initially same size
	@Override
	protected LayoutParams getParams(int id, View view) {
		return new LayoutParams(400, 300);
	}

	// we want the system window decorations, and we want to drag the body
	@Override
	protected int getFlags(int id) {
		return FLAG_DECORATION_SYSTEM | FLAG_BODY_MOVE_ENABLE
				| FLAG_HIDE_ENABLE | FLAG_WINDOW_BRING_TO_FRONT_ON_TAP;
	}

	@Override
	protected String getPersistentNotificationTitle(int id) {
		return getAppName() + " Running";
	}

	@Override
	protected String getPersistentNotificationMessage(int id) {
		return "Click to add a new " + getAppName();
	}

	@Override
	protected Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getShowIntent(this, StandOutMultiWindow.class,
				getUniqueId());
	}

	@Override
	protected int getHiddenIcon() {
		return android.R.drawable.ic_menu_info_details;
	}

	@Override
	protected String getHiddenNotificationTitle(int id) {
		return getAppName() + " Hidden";
	}

	@Override
	protected String getHiddenNotificationMessage(int id) {
		return "Click to restore #" + id;
	}

	@Override
	protected Intent getHiddenNotificationIntent(int id) {
		return StandOutWindow
				.getShowIntent(this, StandOutMultiWindow.class, id);
	}
}
