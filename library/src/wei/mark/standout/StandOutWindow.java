package wei.mark.standout;

import java.util.LinkedList;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
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
	 * Intent action: Close all existing windows.
	 */
	public static final String ACTION_CLOSE_ALL = "CLOSE_ALL";

	/**
	 * Intent action: Hide an existing window with an existing id. To enable the
	 * ability to restore this window, make sure you implement
	 * {@link #getHiddenNotification(int)}.
	 */
	public static final String ACTION_HIDE = "HIDE";

	/**
	 * This default flag indicates that the window requires no window
	 * decorations (titlebar, hide/close buttons, resize handle, etc).
	 */
	public static final int FLAG_DECORATION_NONE = 0x00000000;

	/**
	 * Setting this flag indicates that the window wants the system provided
	 * window decorations (titlebar, hide/close buttons, resize handle, etc).
	 */
	public static final int FLAG_DECORATION_SYSTEM = 0x00000001;

	/**
	 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag indicates
	 * that the window decorator should NOT provide a hide button.
	 */
	public static final int FLAG_DECORATION_HIDE_DISABLE = 0x00000002;

	/**
	 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag indicates
	 * that the window decorator should NOT provide a close button.
	 */
	public static final int FLAG_DECORATION_CLOSE_DISABLE = 0x00000004;

	/**
	 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag indicates
	 * that the window decorator should NOT provide a resize handle.
	 */
	public static final int FLAG_DECORATION_RESIZE_DISABLE = 0x00000008;

	/**
	 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag indicates
	 * that the window decorator should NOT provide a resize handle.
	 */
	public static final int FLAG_DECORATION_MOVE_DISABLE = 0x00000010;

	/**
	 * Setting this flag indicates that the window can be moved by dragging the
	 * body.
	 * 
	 * <p>
	 * Note that if {@link #FLAG_DECORATION_SYSTEM} is set, the window can
	 * always be moved by dragging the titlebar.
	 */
	public static final int FLAG_BODY_MOVE_ENABLE = 0x00000020;

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
	 *            The id representing this window. If the id exists, and the
	 *            corresponding window was previously hidden, then that window
	 *            will be restored.
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
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @param id
	 *            The id representing this window. The window must previously be
	 *            shown.
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
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @param id
	 *            The id representing this window. The window must previously be
	 *            shown.
	 */
	public static void close(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		context.startService(getCloseIntent(context, cls, id));
	}

	/**
	 * Close all existing windows.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 */
	public static void closeAll(Context context,
			Class<? extends StandOutWindow> cls) {
		context.startService(getCloseAllIntent(context, cls));
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
	 *            The id representing this window. If the id exists, and the
	 *            corresponding window was previously hidden, then that window
	 *            will be restored.
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
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @param id
	 *            The id representing this window. If the id exists, and the
	 *            corresponding window was previously hidden, then that window
	 *            will be restored.
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
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @param id
	 *            The id representing this window. If the id exists, and the
	 *            corresponding window was previously hidden, then that window
	 *            will be restored.
	 * @return An {@link Intent} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getCloseIntent(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_CLOSE);
	}

	/**
	 * See {@link #closeAll(Context, Class, int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @return An {@link Intent} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getCloseAllIntent(Context context,
			Class<? extends StandOutWindow> cls) {
		return new Intent(context, cls).setAction(ACTION_CLOSE_ALL);
	}

	// internal system services
	private WindowManager mWindowManager;
	private NotificationManager mNotificationManager;
	private LayoutInflater mLayoutInflater;

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
		mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
			} else if (ACTION_HIDE.equals(action)) {
				hide(id);
			} else if (ACTION_CLOSE.equals(action)) {
				close(id);
			} else if (ACTION_CLOSE_ALL.equals(action)) {
				closeAll();
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
		closeAll();
	}

	/**
	 * Create a new {@link View} corresponding to the id, and add it as a child
	 * to the root. The view will become the contents of this StandOut window.
	 * The view MUST be newly created, and you MUST attach it to root.
	 * 
	 * <p>
	 * If you are inflating your view from XML, make sure you use
	 * {@link LayoutInflater#inflate(int, ViewGroup, boolean)} to attach your
	 * view to root. Set the ViewGroup to be root, and the boolean to true.
	 * 
	 * <p>
	 * If you are creating your view programatically, make sure you use
	 * {@link ViewGroup#addView(View)} to add your view to root.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param root
	 *            The {@link ViewGroup} to attach your view as a child to.
	 * @return A new {@link View} corresponding to the id. The view will be the
	 *         content of this StandOut window. The view MUST be newly created.
	 */
	protected abstract View createAndAttachView(int id, ViewGroup root);

	/**
	 * Return the {@link StandOutWindow#LayoutParams} for the corresponding id.
	 * The system will set the layout params on the view for this StandOut
	 * window. The layout params may be reused.
	 * 
	 * 
	 * @param id
	 *            The id of the window.
	 * @param view
	 *            The view corresponding to the id. Given as courtesy, so you
	 *            may get the existing layout params.
	 * @return The {@link StandOutWindow#LayoutParams} corresponding to the id.
	 *         The layout params will be set on the view. The layout params may
	 *         be reused.
	 */
	protected abstract StandOutWindow.LayoutParams getParams(int id, View view);

	/**
	 * Implement this method to change modify the behavior and appearance of the
	 * window corresponding to the id.
	 * 
	 * <p>
	 * You may use any of the flags defined in {@link StandOutWindow} such as
	 * {@link #FLAG_DECORATION_NONE}.
	 * 
	 * <p>
	 * Use bitwise OR (|) to set flags, and bitwise XOR (^) to unset flags. To
	 * test if a flag is set, use (getFlags(id) & flag) != 0.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return Bitwise OR'd flags
	 */
	protected int getFlags(int id) {
		return FLAG_DECORATION_NONE;
	}

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
	 *            The id of the window.
	 * @return The {@link Notification} corresponding to the id, or null if
	 *         you've previously returned a notification.
	 */
	protected abstract Notification getPersistentNotification(int id);

	/**
	 * Return a hidden {@link Notification} for the corresponding id. The system
	 * will request a notification for every id that is hidden.
	 * 
	 * <p>
	 * If null is returned, StandOut will assume you do not wish to support
	 * hiding this window, and will {@link #close(int)} it for you.
	 * 
	 * <p>
	 * See the StandOutExample project for an implementation of
	 * {@link #getHiddenNotification(int)} that for every hidden window keeps a
	 * notification which restores that window upon user's click.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The {@link Notification} corresponding to the id or null.
	 */
	protected Notification getHiddenNotification(int id) {
		return null;
	}

	/**
	 * Implement this method to be alerted to touch events on the window
	 * corresponding to the id. The events are passed directly from
	 * {@link View.OnTouchListener#onTouch(View, MotionEvent)}
	 * 
	 * @see {@link View.OnTouchListener#onTouch(View, MotionEvent)}
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param window
	 *            The window corresponding to the id.
	 * @param view
	 *            The view where the event originated from.
	 * @param event
	 *            See linked method.
	 */
	protected boolean onTouch(int id, View window, View view, MotionEvent event) {
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
	protected boolean onShow(int id, View window) {
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
	protected boolean onClose(int id, View window) {
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
	protected boolean onHide(int id, View window) {
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
		View window = getWrappedView(id);

		if (window == null) {
			Log.w("StandOutWindow", "Tried to show(" + id + ") a null view");
			return;
		}

		// alert callbacks
		onShow(id, window);

		((WrappedTag) window.getTag()).shown = true;

		// add view to internal map
		views.put(id, window);

		// get the params corresponding to the id
		StandOutWindow.LayoutParams params = (LayoutParams) window
				.getLayoutParams();
		if (params == null) {
			params = getParams(id, window);
		}

		try {
			// add the view to the window manager
			mWindowManager.addView(window, params);
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
		View window = getWrappedView(id);

		if (window == null) {
			Log.w("StandOutWindow", "Tried to close(" + id + ") a null view");
			return;
		}

		// alert callbacks
		onClose(id, window);

		WrappedTag tag = (WrappedTag) window.getTag();

		if (tag.shown) {
			try {
				// remove the view from the window manager
				mWindowManager.removeView(window);
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
	 * Close all existing windows.
	 */
	protected final void closeAll() {
		// add ids to temporary set to avoid concurrent modification
		LinkedList<Integer> ids = new LinkedList<Integer>();
		for (int id : views.keySet()) {
			ids.add(id);
		}
		// close each window
		for (int id : ids) {
			close(id);
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
		// get the hidden notification for this view
		Notification notification = getHiddenNotification(id);

		if (notification == null) {
			close(id);
			return;
		}

		// get the view corresponding to the id
		View window = getWrappedView(id);

		if (window == null) {
			Log.w("StandOutWindow", "Tried to hide(" + id + ") a null view");
			return;
		}

		// alert callbacks
		onHide(id, window);

		((WrappedTag) window.getTag()).shown = false;

		try {
			// remove the view from the window manager
			mWindowManager.removeView(window);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// display the notification
		notification.flags = notification.flags | Notification.FLAG_NO_CLEAR
				| Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(id, notification);
	}

	/**
	 * Update the window corresponding to this view with the given params.
	 * 
	 * @param window
	 *            The window to update.
	 * @param params
	 *            The updated layout params to apply.
	 */
	protected final void updateViewLayout(View window,
			StandOutWindow.LayoutParams params) {
		if (window == null) {
			Log.w("StandOutWindow", "Tried to updateViewLayout() a null window");
			return;
		}

		mWindowManager.updateViewLayout(window, params);
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
		StandOutWindow.LayoutParams params = (LayoutParams) view
				.getLayoutParams();
		if (params == null) {
			params = getParams(id, view);
		}

		bringToFront(view, params);
	}

	/**
	 * Bring the window corresponding to this view in front of all other
	 * windows.
	 * 
	 * @param window
	 *            The view of the window to bring to the front.
	 * @param params
	 *            The layout params to apply to the view when re-adding to the
	 *            window manager, or null to apply the existing layout params
	 *            from the view
	 */
	protected final void bringToFront(View window,
			StandOutWindow.LayoutParams params) {
		if (window == null) {
			Log.w("StandOutWindow", "Tried to bringToFront() a null view");
			return;
		}

		mWindowManager.removeView(window);

		if (params == null) {
			params = (StandOutWindow.LayoutParams) window.getLayoutParams();
		}

		mWindowManager.addView(window, params);
	}

	// wraps the view from getView() into a frame that is easier to manage.
	// the frame allows us to pass touch input to implementations
	// and set a WrappedTag to keep track of the id and visibility
	private View getWrappedView(int id) {
		// try get the wrapped view from the internal map
		View cachedView = views.get(id);

		// if the wrapped view exists, then return it rather than creating one
		if (cachedView != null) {
			return cachedView;
		}

		// create the wrapping frame and body
		final View window;
		FrameLayout body;

		int flags = getFlags(id);

		if ((flags & FLAG_DECORATION_SYSTEM) != 0) {
			// requested system window decorations
			window = getSystemWindow(id);
			body = (FrameLayout) window.findViewById(R.id.body);
		} else {
			// did not request decorations. will provide own implementation
			window = new FrameLayout(this);
			body = (FrameLayout) window;
		}

		// attach the view corresponding to the id from the implementation
		View view = createAndAttachView(id, body);

		// make sure the implemention created a view
		if (view == null) {
			throw new RuntimeException(
					"Your view must not be null in createAndAttachView()");
		}
		// make sure the implementation attached the view
		if (view.getParent() == null) {
			throw new RuntimeException(
					"You must attach your view to the given root ViewGroup in createAndAttachView()");
		}

		// wrap the existing tag and attach it to the frame
		window.setTag(new WrappedTag(id, false, view.getTag()));

		return window;
	}

	/**
	 * Returns the system window decorations if the implementation sets
	 * {@link #FLAG_DECORATION_SYSTEM}.
	 * 
	 * <p>
	 * The system window decorations support hiding, closing, moving, and
	 * resizing.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The frame view containing the system window decorations.
	 */
	private View getSystemWindow(final int id) {
		final View window = mLayoutInflater.inflate(R.layout.window, null);

		// hide
		Button hide = (Button) window.findViewById(R.id.hide);
		hide.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("StandOutHelloWorld", "Minimize button clicked");
				hide(id);
			}
		});

		// close
		Button close = (Button) window.findViewById(R.id.close);
		close.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("StandOutHelloWorld", "Close button clicked");
				close(id);
			}
		});

		// move
		final OnTouchListener moveTouchListener = new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (id) {
					default:
						WindowTouchInfo touchInfo = ((WrappedTag) window
								.getTag()).touchInfo;
						StandOutWindow.LayoutParams params = (LayoutParams) window
								.getLayoutParams();

						switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
								touchInfo.x = params.x;
								touchInfo.y = params.y;
								touchInfo.downX = (int) event.getRawX();
								touchInfo.downY = (int) event.getRawY();
								touchInfo.deltaX = touchInfo.deltaY = 0;
								break;
							case MotionEvent.ACTION_MOVE:
								touchInfo.deltaX = (int) event.getRawX()
										- touchInfo.downX;
								touchInfo.deltaY = (int) event.getRawY()
										- touchInfo.downY;
								break;
							case MotionEvent.ACTION_UP:
								touchInfo.x = touchInfo.x + touchInfo.deltaX;
								touchInfo.y = touchInfo.y + touchInfo.deltaY;

								// tap
								if (touchInfo.deltaX == 0
										&& touchInfo.deltaY == 0) {
									bringToFront(id);
								}

								touchInfo.deltaX = touchInfo.deltaY = 0;
								touchInfo.downX = touchInfo.downY = 0;
								break;
						}

						Log.d("StandOutWindow", "Titlebar handle touch: "
								+ event);

						// update the position of the window
						params.x = touchInfo.x + touchInfo.deltaX;
						params.y = touchInfo.y + touchInfo.deltaY;
						updateViewLayout(window, params);

						return true;
				}
			}
		};

		final View titlebar = window.findViewById(R.id.titlebar);
		titlebar.setOnTouchListener(moveTouchListener);

		// resize
		View corner = window.findViewById(R.id.corner);
		corner.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (id) {
					default:
						WindowTouchInfo touchInfo = ((WrappedTag) window
								.getTag()).touchInfo;
						StandOutWindow.LayoutParams params = (LayoutParams) window
								.getLayoutParams();

						switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
								touchInfo.width = params.width;
								touchInfo.height = params.height;
								touchInfo.downX = (int) event.getRawX();
								touchInfo.downY = (int) event.getRawY();
								touchInfo.deltaX = touchInfo.deltaY = 0;
								break;
							case MotionEvent.ACTION_MOVE:
								touchInfo.deltaX = (int) event.getRawX()
										- touchInfo.downX;
								touchInfo.deltaY = (int) event.getRawY()
										- touchInfo.downY;
								break;
							case MotionEvent.ACTION_UP:
								touchInfo.width = touchInfo.width
										+ touchInfo.deltaX;
								touchInfo.height = touchInfo.height
										+ touchInfo.deltaY;

								touchInfo.deltaX = touchInfo.deltaY = 0;
								touchInfo.downX = touchInfo.downY = 0;
								break;
						}

						Log.d("StandOutWindow", "Corner handle touch: " + event);

						// update the position of the window
						params.width = Math.max(0, touchInfo.width
								+ touchInfo.deltaX);
						params.height = Math.max(0, touchInfo.height
								+ touchInfo.deltaY);
						updateViewLayout(window, params);

						return true;
				}
			}
		});

		// set window appearance and behavior based on flags
		int flags = getFlags(id);

		if ((flags & FLAG_DECORATION_HIDE_DISABLE) != 0) {
			hide.setVisibility(View.GONE);
		}
		if ((flags & FLAG_DECORATION_CLOSE_DISABLE) != 0) {
			close.setVisibility(View.GONE);
		}
		if ((flags & FLAG_DECORATION_MOVE_DISABLE) != 0) {
			titlebar.setOnTouchListener(null);
		}
		if ((flags & FLAG_DECORATION_RESIZE_DISABLE) != 0) {
			corner.setVisibility(View.GONE);
		}

		// body should always send touch events
		final boolean bodyMoveEnabled = (flags & FLAG_BODY_MOVE_ENABLE) != 0;
		View body = window.findViewById(R.id.body);
		body.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// pass all touch events to the implementation
				int id = ((WrappedTag) window.getTag()).id;
				boolean consumed = StandOutWindow.this.onTouch(id, window, v,
						event);

				// if set FLAG_BODY_MOVE_ENABLE, move the window
				if (bodyMoveEnabled) {
					consumed = consumed || moveTouchListener.onTouch(v, event);
				}

				return consumed;
			}
		});

		return window;
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
		 * Id of the window
		 */
		public int id;
		/**
		 * Whether the window is shown or hidden/closed
		 */
		public boolean shown;
		/**
		 * Touch information of the window
		 */
		public WindowTouchInfo touchInfo;
		/**
		 * Original tag of the wrapped view
		 */
		public Object tag;

		public WrappedTag(int id, boolean shown, Object tag) {
			super();
			this.id = id;
			this.shown = shown;
			this.touchInfo = new WindowTouchInfo();
			this.tag = tag;
		}
	}

	public class WindowTouchInfo {
		public int x, y, width, height;
		public int downX, downY;
		public int deltaX, deltaY;
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

			width = height = 200;
			x = y = 50 + (50 * views.size()) % 300;
			gravity = Gravity.TOP | Gravity.LEFT;
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
}
