package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * This implementation provides multiple windows. You may extend this class or
 * use it as a reference for a basic foundation for your own windows.
 * 
 * <p>
 * Functionality includes system window decorators, moveable, resizeable,
 * hideable, closeable, and bring-to-frontable.
 * 
 * <p>
 * The persistent notification creates new windows. The hidden notifications
 * restores previously hidden windows.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class MultiWindow extends StandOutWindow {

	@Override
	protected String getAppName() {
		return "MultiWindow";
	}

	@Override
	protected int getAppIcon() {
		return android.R.drawable.ic_menu_add;
	}

	@Override
	protected String getTitle(int id) {
		return getAppName() + " " + id;
	}

	@Override
	protected void createAndAttachView(int id, FrameLayout frame) {
		// create a new layout from body.xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.body, frame, true);

		TextView idText = (TextView) view.findViewById(R.id.id);
		idText.setText(String.valueOf(id));
	}

	// every window is initially same size
	@Override
	protected LayoutParams getParams(int id, Window window) {
		return new LayoutParams(id, 400, 300);
	}

	// we want the system window decorations, we want to drag the body, we want
	// the ability to hide windows, and we want to tap the window to bring to
	// front
	@Override
	protected int getFlags(int id) {
		return StandOutFlags.FLAG_DECORATION_SYSTEM
				| StandOutFlags.FLAG_BODY_MOVE_ENABLE
				| StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
				| StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
				| StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
				| StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
	}

	@Override
	protected String getPersistentNotificationTitle(int id) {
		return getAppName() + " Running";
	}

	@Override
	protected String getPersistentNotificationMessage(int id) {
		return "Click to add a new " + getAppName();
	}

	// return an Intent that creates a new MultiWindow
	@Override
	protected Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getShowIntent(this, getClass(), getUniqueId());
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

	// return an Intent that restores the MultiWindow
	@Override
	protected Intent getHiddenNotificationIntent(int id) {
		return StandOutWindow.getShowIntent(this, getClass(), id);
	}

	@Override
	protected Animation getShowAnimation(int id) {
		if (isExistingId(id)) {
			// restore
			return AnimationUtils.loadAnimation(this,
					android.R.anim.slide_in_left);
		} else {
			// show
			return super.getShowAnimation(id);
		}
	}

	@Override
	protected Animation getHideAnimation(int id) {
		return AnimationUtils.loadAnimation(this,
				android.R.anim.slide_out_right);
	}

	@Override
	protected void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends StandOutWindow> fromCls, int fromId) {
		// receive data from WidgetsWindow's button press
		// to show off the data sending framework
		switch (requestCode) {
			case WidgetsWindow.DATA_CHANGED_TEXT:
				String changedText = data.getString("changedText");
				TextView status = (TextView) getWindow(id)
						.findViewById(R.id.id);
				status.setTextSize(20);
				status.setText("Received data from WidgetsWindow: "
						+ changedText);
				break;
			default:
				Log.d("MultiWindow", "Unexpected data received.");
				break;
		}
	}
}
