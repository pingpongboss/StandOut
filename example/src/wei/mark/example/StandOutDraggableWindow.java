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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class StandOutDraggableWindow extends StandOutWindow {

	@Override
	protected View createView(final int id) {
		switch (id) {
			default:
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.main, null);

				TextView idText = (TextView) view.findViewById(R.id.id);
				idText.setText(String.valueOf(id));

				Button closeButton = (Button) view
						.findViewById(R.id.close_button);
				closeButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Log.d("StandOutHelloWorld", "Close button clicked");
						close(id);
					}
				});

				Button minimizeButton = (Button) view
						.findViewById(R.id.minimize_button);
				minimizeButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Log.d("StandOutHelloWorld", "Minimize button clicked");
						hide(id);
					}
				});

				view.setTag(new CustomTag(id));

				return view;
		}
	}

	private int nextId = DEFAULT_ID;

	@Override
	protected LayoutParams getParams(int id, View view) {
		CustomTag tag = (CustomTag) ((WrappedTag) view.getTag()).tag;

		return new LayoutParams(tag.x + tag.deltaX, tag.y + tag.deltaY,
				Gravity.TOP | Gravity.LEFT);
	}

	@Override
	protected boolean onTouch(int id, View view, MotionEvent event) {
		switch (id) {
			default:
				CustomTag tag = (CustomTag) ((WrappedTag) view.getTag()).tag;

				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN) {
					tag.downX = (int) event.getRawX();
					tag.downY = (int) event.getRawY();
					tag.deltaX = tag.deltaY = 0;
				} else if (action == MotionEvent.ACTION_MOVE) {
					tag.deltaX = (int) event.getRawX() - tag.downX;
					tag.deltaY = (int) event.getRawY() - tag.downY;
				} else if (action == MotionEvent.ACTION_UP) {
					tag.x = tag.x + tag.deltaX;
					tag.y = tag.y + tag.deltaY;

					// tap
					if (tag.deltaX == 0 && tag.deltaY == 0) {
						bringToFront(id);
					}

					tag.deltaX = tag.deltaY = 0;
					tag.downX = tag.x;
					tag.downY = tag.y;
				}
				Log.d("StandOutHelloWorld", "Handle touch: " + event);

				updateViewLayout(id);

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

	private class CustomTag {
		private int x, y;
		private int downX, downY;
		private int deltaX, deltaY;

		public CustomTag(int id) {
			x = 50 + (50 * id) % 300;
			y = 50 + (50 * id) % 300;
		}
	}
}
