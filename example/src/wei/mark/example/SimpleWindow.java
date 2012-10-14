package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

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
	protected void createAndAttachView(int id, FrameLayout frame) {
		// create a new layout from body.xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.simple, frame, true);
	}

	// the window will be centered
	@Override
	protected LayoutParams getParams(int id, Window window) {
		return new LayoutParams(id, 250, 300, LayoutParams.CENTER,
				LayoutParams.CENTER);
	}

	// move the window by dragging the view
	@Override
	protected int getFlags(int id) {
		return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
				| StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
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
