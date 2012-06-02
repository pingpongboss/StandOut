package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
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

	// every window is initially 200x200
	@Override
	protected LayoutParams getParams(int id, View view) {
		return new LayoutParams(200, 200);
	}

	// we want the system window decorations, and we want to drag the body
	@Override
	protected int getFlags(int id) {
		return FLAG_DECORATION_SYSTEM | FLAG_BODY_MOVE_ENABLE;
	}

	// our persistent notification creates a new window on every click
	@Override
	protected Notification getPersistentNotification(int id) {
		// basic notification stuff
		// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
		int icon = android.R.drawable.ic_menu_add;
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = "StandOutMultiWindow Example";
		String contentText = "Click to add a new StandOutMultiWindow.";
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		// getPersistentNotification() is called for every new window
		// so we replace the old notification with a new one that has
		// a bigger id
		Intent notificationIntent = StandOutWindow.getShowIntent(this,
				StandOutMultiWindow.class, id + 1);

		PendingIntent contentIntent = PendingIntent.getService(this, 0,
				notificationIntent,
				// flag updates existing persistent notification
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(c, contentTitle, contentText,
				contentIntent);
		return notification;
	}

	// each hidden notification will restore the hidden window
	@Override
	protected Notification getHiddenNotification(int id) {
		// same basics as getPersistentNotification()
		int icon = android.R.drawable.ic_menu_info_details;
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = "Hidden StandOutMultiWindow Example";
		String contentText = "Click to restore #" + id + ".";
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		// the difference here is we are providing the same id
		Intent notificationIntent = StandOutWindow.getShowIntent(this,
				StandOutMultiWindow.class, id);

		PendingIntent contentIntent = PendingIntent.getService(this, 0,
				notificationIntent, 0);

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(c, contentTitle, contentText,
				contentIntent);
		return notification;
	}
}
