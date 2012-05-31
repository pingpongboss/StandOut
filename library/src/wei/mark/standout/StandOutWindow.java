package wei.mark.standout;

import java.util.WeakHashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.FrameLayout;

public abstract class StandOutWindow extends Service implements OnTouchListener {
	public static final int DEFAULT_ID = 0;
	public static final int RESERVED_ID = -1;

	public static final String ACTION_SHOW = "SHOW";
	public static final String ACTION_RESTORE = "RESTORE";
	public static final String ACTION_CLOSE = "CLOSE";
	public static final String ACTION_HIDE = "HIDE";

	private static WeakHashMap<Integer, View> views = new WeakHashMap<Integer, View>();

	public static void show(Context context, Class<?> cls, int id) {
		context.startService(getShowIntent(context, cls, id));
	}

	public static void hide(Context context, Class<?> cls, int id) {
		context.startService(getShowIntent(context, cls, id));
	}

	public static void close(Context context, Class<?> cls, int id) {
		context.startService(getCloseIntent(context, cls, id));
	}

	public static Intent getShowIntent(Context context, Class<?> cls, int id) {
		String action = views.containsKey(id) ? ACTION_RESTORE : ACTION_SHOW;
		Uri uri = views.containsKey(id) ? Uri.parse("standout://" + id) : null;

		return new Intent(context, cls).putExtra("id", id).setAction(action)
				.setData(uri);
	}

	public static Intent getHideIntent(Context context, Class<?> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_HIDE);
	}

	public static Intent getCloseIntent(Context context, Class<?> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_CLOSE);
	}

	private WindowManager mWindowManager;
	private NotificationManager mNotificationManager;

	private boolean started;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		started = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (intent != null) {
			String action = intent.getAction();
			int id = intent.getIntExtra("id", DEFAULT_ID);
			Log.d("StandOutWindow", "Intent id: " + id);

			if (id == RESERVED_ID) {
				throw new RuntimeException(
						"ID cannot equals StandOutWindow.RESERVED_ID");
			}

			if (ACTION_SHOW.equals(action) || ACTION_RESTORE.equals(action)) {
				show(id);
			} else if (ACTION_CLOSE.equals(action)) {
				close(id);
			} else if (ACTION_HIDE.equals(action)) {
				hide(id);
			}
		}

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		for (int id : views.keySet()) {
			hide(id);
		}
	}

	protected abstract View createView(int id);

	protected abstract StandOutWindow.LayoutParams getParams(int id, View view);

	protected abstract Notification getPersistentNotification(int id);

	protected abstract Notification getMinimizedNotification(int id);

	protected abstract boolean onTouch(int id, View view, MotionEvent event);

	public View getWrappedView(int id) {
		View view = views.get(id);
		if (view != null) {
			return view;
		}

		view = createView(id);

		FrameLayout frame = new FrameLayout(this);
		frame.setTag(new WrappedTag(id, view.getTag()));

		frame.addView(view, new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT));
		frame.setOnTouchListener(this);

		return frame;
	}

	protected final void show(int id) {
		View view = getWrappedView(id);

		views.put(id, view);

		StandOutWindow.LayoutParams params = getParams(id, view);

		try {
			mWindowManager.addView(view, params);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Notification notification = getPersistentNotification(id);
		notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;

		if (!started) {
			startForeground(RESERVED_ID, notification);
			started = true;
		}

		onShow(id, view);
	}

	protected final void close(int id) {
		View view = getWrappedView(id);
		try {
			mWindowManager.removeView(view);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		views.remove(id);
		if (views.isEmpty()) {
			started = false;
			stopForeground(true);
		}

		onClose(id, view);
	}

	protected final void hide(int id) {
		View view = getWrappedView(id);
		try {
			mWindowManager.removeView(view);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Notification notification = getMinimizedNotification(id);
		notification.flags = notification.flags | Notification.FLAG_NO_CLEAR
				| Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(id, notification);

		onHide(id, view);
	}

	protected void onShow(int id, View view) {
		Log.d("StandOutWindow", "Loaded views: " + views.keySet());
	}

	protected void onClose(int id, View view) {
		Log.d("StandOutWindow", "Loaded views: " + views.keySet());
	}

	protected void onHide(int id, View view) {
		Log.d("StandOutWindow", "Loaded views: " + views.keySet());
	}

	protected final void updateViewLayout(int id) {
		View view = getWrappedView(id);
		StandOutWindow.LayoutParams params = getParams(id, view);

		updateViewLayout(view, params);
	}

	protected final void updateViewLayout(View view,
			StandOutWindow.LayoutParams params) {
		mWindowManager.updateViewLayout(view, params);
	}

	protected final void bringToFront(int id) {
		View view = getWrappedView(id);
		StandOutWindow.LayoutParams params = getParams(id, view);

		bringToFront(view, params);
	}

	protected final void bringToFront(View view,
			StandOutWindow.LayoutParams params) {
		mWindowManager.removeView(view);
		mWindowManager.addView(view, params);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int id = ((WrappedTag) v.getTag()).id;
		return onTouch(id, v, event);
	}

	protected class LayoutParams extends
			android.view.WindowManager.LayoutParams {
		public LayoutParams() {
			super(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					TYPE_SYSTEM_ALERT, FLAG_NOT_FOCUSABLE,
					PixelFormat.TRANSLUCENT);
		}

		public LayoutParams(int w, int h) {
			this();
			width = w;
			height = h;
		}

		public LayoutParams(int w, int h, int xpos, int ypos, int gravityFlag) {
			this(w, h);
			x = xpos;
			y = ypos;
			gravity = gravityFlag;
		}

		public LayoutParams(int xpos, int ypos, int gravityFlag) {
			this();
			x = xpos;
			y = ypos;
			gravity = gravityFlag;
		}
	}

	public class WrappedTag {
		public int id;
		public Object tag;

		public WrappedTag(int id, Object tag) {
			super();
			this.id = id;
			this.tag = tag;
		}
	}
}
