package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SimpleWindow extends StandOutWindow {

	@Override
	protected String getAppName() {
		return "SimpleWindow";
	}

	@Override
	protected int getAppIcon() {
		return android.R.drawable.ic_menu_close_clear_cancel;
	}

	@Override
	protected View createAndAttachView(final int id, ViewGroup root) {
		// create a new layout from body.xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.simple, root, true);

		return view;
	}

	// the window will be centered
	@Override
	protected LayoutParams getParams(int id, View view) {
		return new LayoutParams(250, 300, 0, 0, Gravity.CENTER);
	}

	// move the window by dragging the view
	@Override
	protected int getFlags(int id) {
		return super.getFlags(id) | FLAG_BODY_MOVE_ENABLE;
	}

	@Override
	protected String getPersistentNotificationMessage(int id) {
		return "Click to close the SimpleWindow";
	}

	@Override
	protected Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getCloseIntent(this, SimpleWindow.class, id);
	}
}
