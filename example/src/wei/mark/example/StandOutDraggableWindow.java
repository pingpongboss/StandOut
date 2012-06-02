package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class StandOutDraggableWindow extends StandOutWindow {

	@Override
	protected View createAndAttachView(int id, ViewGroup root) {
		switch (id) {
			default:
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.body, root, true);

				TextView idText = (TextView) view.findViewById(R.id.id);
				idText.setText(String.valueOf(id));

				return view;
		}
	}

	private int nextId = DEFAULT_ID;

	@Override
	protected LayoutParams getParams(int id, View view) {
		WindowTouchInfo touchInfo = ((WrappedTag) view.getTag()).touchInfo;

		return new LayoutParams(200, 200, touchInfo.x, touchInfo.y, Gravity.TOP
				| Gravity.LEFT);
	}

	@Override
	protected int getFlags(int id) {
		return super.getFlags(id) | FLAG_DECORATION_SYSTEM;
	}

	@Override
	protected boolean onTouch(int id, View window, View view, MotionEvent event) {
		switch (id) {
			default:
				Log.d("StandOutHelloWorld", "Handle touch: " + event);

				switch (event.getAction()) {
					case MotionEvent.ACTION_UP:
						WindowTouchInfo touchInfo = ((WrappedTag) window
								.getTag()).touchInfo;

						// tap
						if (touchInfo.deltaX == 0 && touchInfo.deltaY == 0) {
							bringToFront(id);
						}
				}
				return true;
		}
	}

	@Override
	protected Notification getPersistentNotification(int id) {
		int icon = android.R.drawable.ic_menu_add;
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = "StandOutWindow Example";
		String contentText = "Click to add a new StandOutWindow.";
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		Intent notificationIntent = StandOutWindow.getShowIntent(this,
				StandOutDraggableWindow.class, id + 1);

		PendingIntent contentIntent = PendingIntent.getService(this, 0,
				notificationIntent,
				// flag updates existing notification
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(c, contentTitle, contentText,
				contentIntent);
		return notification;
	}

	@Override
	protected Notification getHiddenNotification(int id) {
		int icon = android.R.drawable.ic_menu_info_details;
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = "Hidden StandOutWindow Example";
		String contentText = "Click to restore #" + id + ".";
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		Intent notificationIntent = StandOutWindow.getShowIntent(this,
				StandOutDraggableWindow.class, id);

		PendingIntent contentIntent = PendingIntent.getService(this, 0,
				notificationIntent,
				// flag updates existing notification
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(c, contentTitle, contentText,
				contentIntent);
		return notification;
	}

	@Override
	protected boolean onShow(int id, View view) {
		super.onShow(id, view);

		this.nextId = Math.max(this.nextId, id) + 1;

		return false;
	}
}
