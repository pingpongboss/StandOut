package wei.mark.standout;

import java.util.WeakHashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

/**
 * Extend this class to easily create and manage floating StandOut windows.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public abstract class StandOutWindow extends Service {
	/**
	 * StandOut window id: You may use this sample id for your first window.
	 */
	public static final int DEFAULT_ID = 0;
	/**
	 * StandOut window id: You may NOT use this id for any windows.
	 */
	public static final int RESERVED_ID = -1;

	/**
	 * Intent action: Show a new window corresponding to the id.
	 */
	public static final String ACTION_SHOW = "SHOW";
	/**
	 * Intent action: Restore a previously hidden window corresponding to the
	 * id. The window should be previously hidden with {@link #ACTION_HIDE}.
	 */
	public static final String ACTION_RESTORE = "RESTORE";
	/**
	 * Intent action: Close an existing window with an existing id.
	 */
	public static final String ACTION_CLOSE = "CLOSE";
	/**
	 * Intent action: Hide an existing window with an existing id. To enable the
	 * ability to restore this window, make sure you implement
	 * {@link #getHiddenNotification(int)}.
	 */
	public static final String ACTION_HIDE = "HIDE";

	// internal map of ids to shown/hidden views
	private static WeakHashMap<Integer, View> views;

	// static constructor
	static {
		views = new WeakHashMap<Integer, View>();
	}

	/**
	 * Show a new window corresponding to the id, or restore a previously hidden
	 * window.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The unique id representing this window. If the id exists, and
	 *            the corresponding window was previously hidden, then that
	 *            window will be restored.
	 */
	public static void show(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		context.startService(getShowIntent(context, cls, id));
	}

	/**
	 * Hide the existing window corresponding to the id. To enable the ability
	 * to restore this window, make sure you implement
	 * {@link #getHiddenNotification(int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The unique id representing this window. The window must
	 *            previously be shown.
	 */
	public static void hide(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		context.startService(getShowIntent(context, cls, id));
	}

	/**
	 * Close an existing window with an existing id.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The unique id representing this window. The window must
	 *            previously be shown.
	 */
	public static void close(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		context.startService(getCloseIntent(context, cls, id));
	}

	/**
	 * See {@link #show(Context, Class, int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The unique id representing this window. If the id exists, and
	 *            the corresponding window was previously hidden, then that
	 *            window will be restored.
	 * @return An {@link Intent} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getShowIntent(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		// Show or restore window depending on whether the id exists
		String action = views.containsKey(id) ? ACTION_RESTORE : ACTION_SHOW;
		Uri uri = views.containsKey(id) ? Uri.parse("standout://" + id) : null;

		return new Intent(context, cls).putExtra("id", id).setAction(action)
				.setData(uri);
	}

	/**
	 * See {@link #hide(Context, Class, int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The unique id representing this window. If the id exists, and
	 *            the corresponding window was previously hidden, then that
	 *            window will be restored.
	 * @return An {@link Intent} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getHideIntent(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_HIDE);
	}

	/**
	 * See {@link #close(Context, Class, int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The unique id representing this window. If the id exists, and
	 *            the corresponding window was previously hidden, then that
	 *            window will be restored.
	 * @return An {@link Intent} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getCloseIntent(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_CLOSE);
	}

	// internal system services
	private WindowManager mWindowManager;
	private NotificationManager mNotificationManager;

	// internal state variables
	private boolean startedForeground;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		startedForeground = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// intent should be created with
		// getShowIntent(), getHideIntent(), getCloseIntent()
		if (intent != null) {
			String action = intent.getAction();
			int id = intent.getIntExtra("id", DEFAULT_ID);
			Log.d("StandOutWindow", "Intent id: " + id);

			// this will interfere with getPersistentNotification()
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
		} else {
			Log.w("StandOutWindow",
					"Tried to onStartCommand() with a null intent");
		}

		// the service is started in foreground in show()
		// so we don't expect Android to kill this service
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// closes all windows
		for (int id : views.keySet()) {
			close(id);
		}
	}

	/**
	 * Create a new {@link View} corresponding to the id. The system will set
	 * the view as the contents of this StandOut window. The view MUST be newly
	 * created.
	 * 
	 * @param id
	 *            The unique id representing the window.
	 * @return A new {@link View} corresponding to the id. The view will be the
	 *         content of this StandOut window. The view MUST be newly created.
	 */
	protected abstract View createView(int id);

	/**
	 * Return the {@link StandOutWindow#LayoutParams} for the corresponding id.
	 * The system will set the layout params on the view for this StandOut
	 * window. The layout params may be reused.
	 * 
	 * 
	 * @param id
	 *            The unique id of the window.
	 * @param view
	 *            The view corresponding to the id. Given as courtesy, so you
	 *            may get the existing layout params.
	 * @return The {@link StandOutWindow#LayoutParams} corresponding to the id.
	 *         The layout params will be set on the view. The layout params may
	 *         be reused.
	 */
	protected abstract StandOutWindow.LayoutParams getParams(int id, View view);

	/**
	 * Return a persistent {@link Notification} for the corresponding id. You
	 * must return a notification for AT LEAST the first id to be requested.
	 * Once the persistent notification is shown, further calls to
	 * {@link #getPersistentNotification(int)} may return null. This way Android
	 * can start the StandOut window service in the foreground and will not kill
	 * the service on low memory.
	 * 
	 * <p>
	 * As a courtesy, the system will request a notification for every new id
	 * shown. Your implementation is encouraged to include the
	 * {@link PendingIntent#FLAG_UPDATE_CURRENT} flag in the notification so
	 * that there is only one system-wide persistent notification.
	 * 
	 * <p>
	 * See the StandOutExample project for an implementation of
	 * {@link #getPersistentNotification(int)} that keeps one system-wide
	 * persistent notification that creates a new window on every click.
	 * 
	 * @param id
	 *            The unique id of the window.
	 * @return The {@link Notification} corresponding to the id, or null if
	 *         you've previously returned a notification.
	 */
	protected abstract Notification getPersistentNotification(int id);

	/**
	 * Return a hidden {@link Notification} for the corresponding id. The system
	 * will request a notification for every id that is hidden.
	 * 
	 * <p>
	 * See the StandOutExample project for an implementation of
	 * {@link #getHiddenNotification(int)} that for every hidden window keeps a
	 * notification that restores that window on click.
	 * 
	 * @param id
	 *            The unique id of the window.
	 * @return The {@link Notification} corresponding to the id or null.
	 */
	protected abstract Notification getHiddenNotification(int id);

	/**
	 * Implement this method to be alerted to touch events on the window
	 * corresponding to the id. The events are passed directly from
	 * {@link View.OnTouchListener#onTouch(View, MotionEvent)}
	 * 
	 * @see {@link View.OnTouchListener#onTouch(View, MotionEvent)}
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view corresponding to the id.
	 * @param event
	 *            See linked method.
	 */
	protected boolean onTouch(int id, View view, MotionEvent event) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be shown. {@link #onShow(int, View)} will occur before the
	 * view is added to the window manager.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be shown.
	 * @return Return true to cancel the view from being shown, or false to
	 *         continue.
	 */
	protected boolean onShow(int id, View view) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be closed. {@link #onClose(int, View)} will occur before
	 * the view is removed from the window manager.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be closed.
	 * @return Return true to cancel the view from being closed, or false to
	 *         continue.
	 */
	protected boolean onClose(int id, View view) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be hidden. {@link #onClose(int, View)} will occur before
	 * the view is removed from the window manager and
	 * {@link #getHiddenNotification(int)} is called.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be hidden.
	 * @return Return true to cancel the view from being hidden, or false to
	 *         continue.
	 */
	protected boolean onHide(int id, View view) {
		return false;
	}

	/**
	 * Show or restore a window corresponding to the id.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final void show(int id) {
		// get the view corresponding to the id
		View view = getWrappedView(id);

		if (view == null) {
			Log.w("StandOutWindow", "Tried to show(" + id + ") a null view");
			return;
		}

		// alert callbacks
		onShow(id, view);

		((WrappedTag) view.getTag()).shown = true;

		// add view to internal map
		views.put(id, view);

		// get the params corresponding to the id
		StandOutWindow.LayoutParams params = getParams(id, view);

		try {
			// add the view to the window manager
			mWindowManager.addView(view, params);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// get the persistent notification
		Notification notification = getPersistentNotification(id);

		// show the notification
		if (notification != null) {
			notification.flags = notification.flags
					| Notification.FLAG_NO_CLEAR;

			// only show notification if not shown before
			if (!startedForeground) {
				// tell Android system to show notification
				startForeground(RESERVED_ID, notification);
				startedForeground = true;
			}
		} else {
			// notification can only be null if it was provided before
			if (!startedForeground) {
				throw new RuntimeException("Your StandOutWindow service must"
						+ "provide a persistent notification."
						+ "The notification prevents Android"
						+ "from killing your service in low"
						+ "memory situations.");
			}
		}
	}

	/**
	 * Close a window corresponding to the id.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final void close(int id) {
		// get the view corresponding to the id
		View view = getWrappedView(id);

		if (view == null) {
			Log.w("StandOutWindow", "Tried to close(" + id + ") a null view");
			return;
		}

		// alert callbacks
		onClose(id, view);

		WrappedTag tag = (WrappedTag) view.getTag();

		if (tag.shown) {
			try {
				// remove the view from the window manager
				mWindowManager.removeView(view);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			tag.shown = false;
			// cancel hidden notification
			mNotificationManager.cancel(id);
		}

		// remove view from internal map
		views.remove(id);
		// if we just released the last view, quit
		if (views.isEmpty()) {
			// tell Android to remove the persistent notification
			// the Service will be shutdown by the system on low memory
			startedForeground = false;
			stopForeground(true);
		}
	}

	/**
	 * Hide a window corresponding to the id. Show a notification for the hidden
	 * window.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final void hide(int id) {
		// get the view corresponding to the id
		View view = getWrappedView(id);

		if (view == null) {
			Log.w("StandOutWindow", "Tried to hide(" + id + ") a null view");
			return;
		}

		// alert callbacks
		onHide(id, view);

		((WrappedTag) view.getTag()).shown = false;

		try {
			// remove the view from the window manager
			mWindowManager.removeView(view);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// get the hidden notification for this view
		Notification notification = getHiddenNotification(id);

		// display the notification
		if (notification != null) {
			notification.flags = notification.flags
					| Notification.FLAG_NO_CLEAR
					| Notification.FLAG_AUTO_CANCEL;

			mNotificationManager.notify(id, notification);
		}
	}

	/**
	 * Update the window corresponding to this id with an updated
	 * {@link StandOutWindow.LayoutParams} from {@link #getParams(int, View)}.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final void updateViewLayout(int id) {
		View view = getWrappedView(id);
		StandOutWindow.LayoutParams params = getParams(id, view);

		updateViewLayout(view, params);
	}

	/**
	 * Update the window corresponding to this view with the given params.
	 * 
	 * @param view
	 *            The view to update.
	 * @param params
	 *            The updated layout params to apply.
	 */
	protected final void updateViewLayout(View view,
			StandOutWindow.LayoutParams params) {
		if (view == null) {
			Log.w("StandOutWindow", "Tried to updateViewLayout() a null view");
			return;
		}

		mWindowManager.updateViewLayout(view, params);
	}

	/**
	 * Bring the window corresponding to this id in front of all other windows.
	 * The window may flicker as it is removed and restored by the system.
	 * 
	 * @param id
	 *            The id of the window to bring to the front.
	 */
	protected final void bringToFront(int id) {
		View view = getWrappedView(id);
		StandOutWindow.LayoutParams params = getParams(id, view);

		bringToFront(view, params);
	}

	/**
	 * Bring the window corresponding to this view in front of all other
	 * windows.
	 * 
	 * @param view
	 *            The view of the window to bring to the front.
	 * @param params
	 *            The layout params to apply to the view when re-adding to the
	 *            window manager, or null to apply the existing layout params
	 *            from the view
	 */
	protected final void bringToFront(View view,
			StandOutWindow.LayoutParams params) {
		if (view == null) {
			Log.w("StandOutWindow", "Tried to bringToFront() a null view");
			return;
		}

		mWindowManager.removeView(view);

		if (params == null) {
			params = (StandOutWindow.LayoutParams) view.getLayoutParams();
		}

		mWindowManager.addView(view, params);
	}

	// wraps the view from getView() into a frame that is easier to manage.
	// the frame allows us to pass touch input to implementations
	// and set a WrappedTag to keep track of the id and visibility
	private final View getWrappedView(int id) {
		// try get the wrapped view from the internal map
		View view = views.get(id);

		// if the wrapped view exists, then return it rather than creating one
		if (view != null) {
			return view;
		}

		// get the view corresponding to the id from the implementation
		view = createView(id);

		// create the wrapping frame
		FrameLayout frame = new FrameLayout(this);
		// wrap the existing tag and attach it to the frame
		frame.setTag(new WrappedTag(id, false, view.getTag()));

		// attach the view to the wrapping frame
		frame.addView(view, new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT));
		// capture all touch events
		frame.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int id = ((WrappedTag) v.getTag()).id;
				// pass all touch events to the implementation
				return StandOutWindow.this.onTouch(id, v, event);
			}
		});

		return frame;
	}

	/**
	 * LayoutParams specific to floating StandOut windows.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
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

	/**
	 * WrappedTag will be attached to views from
	 * {@link StandOutWindow#getWrappedView(int)}
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	public class WrappedTag {
		/**
		 * Unique id of the window
		 */
		public int id;
		/**
		 * Whether the window is shown or hidden/closed
		 */
		public boolean shown;
		/**
		 * Original tag of the wrapped view
		 */
		public Object tag;

		public WrappedTag(int id, boolean shown, Object tag) {
			super();
			this.id = id;
			this.shown = shown;
			this.tag = tag;
		}
	}
}
