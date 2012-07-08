package wei.mark.standout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import wei.mark.standout.StandOutWindow.Window.Editor;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Extend this class to easily create and manage floating StandOut windows.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public abstract class StandOutWindow extends Service {
	private static final String TAG = "StandOutWindow";

	/**
	 * StandOut window id: You may use this sample id for your first window.
	 */
	public static final int DEFAULT_ID = 0;

	/**
	 * Special StandOut window id: You may NOT use this id for any windows.
	 */
	public static final int ONGOING_NOTIFICATION_ID = -1;

	/**
	 * StandOut window id: You may use this id when you want it to be
	 * disregarded. The system makes no distinction for this id; it is only used
	 * to improve code readability.
	 */
	public static final int DISREGARD_ID = -2;

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
	 * Intent action: Send data to a new or existing window.
	 */
	public static final String ACTION_SEND_DATA = "SEND_DATA";

	/**
	 * Intent action: Hide an existing window with an existing id. To enable the
	 * ability to restore this window, make sure you implement
	 * {@link #getHiddenNotification(int)}.
	 */
	public static final String ACTION_HIDE = "HIDE";

	/**
	 * Flags to be returned from {@link StandOutWindow#getFlags(int)}. This
	 * class was created to avoid polluting the flags namespace.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	public static class StandOutFlags {
		// This counter keeps track of which primary bit to set for each flag
		private static int flag_bit = 0;

		/**
		 * Setting this flag indicates that the window wants the system provided
		 * window decorations (titlebar, hide/close buttons, resize handle,
		 * etc).
		 */
		public static final int FLAG_DECORATION_SYSTEM = 1 << flag_bit++;

		/**
		 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag
		 * indicates that the window decorator should NOT provide a close
		 * button.
		 * 
		 * <p>
		 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
		 */
		public static final int FLAG_DECORATION_CLOSE_DISABLE = FLAG_DECORATION_SYSTEM
				| 1 << flag_bit++;

		/**
		 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag
		 * indicates that the window decorator should NOT provide a resize
		 * handle.
		 * 
		 * <p>
		 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
		 */
		public static final int FLAG_DECORATION_RESIZE_DISABLE = FLAG_DECORATION_SYSTEM
				| 1 << flag_bit++;

		/**
		 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag
		 * indicates that the window decorator should NOT provide a resize
		 * handle.
		 * 
		 * <p>
		 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
		 */
		public static final int FLAG_DECORATION_MOVE_DISABLE = FLAG_DECORATION_SYSTEM
				| 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the window can be moved by dragging
		 * the body.
		 * 
		 * <p>
		 * Note that if {@link #FLAG_DECORATION_SYSTEM} is set, the window can
		 * always be moved by dragging the titlebar.
		 */
		public static final int FLAG_BODY_MOVE_ENABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that windows are able to be hidden, that
		 * {@link StandOutWindow#getHiddenIcon(int)},
		 * {@link StandOutWindow#getHiddenTitle(int)}, and
		 * {@link StandOutWindow#getHiddenMessage(int)} are implemented, and
		 * that the system window decorator should provide a hide button if
		 * {@link #FLAG_DECORATION_SYSTEM} is set.
		 */
		public static final int FLAG_WINDOW_HIDE_ENABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the window should be brought to the
		 * front upon user interaction.
		 * 
		 * <p>
		 * Note that if you set this flag, there is a noticeable flashing of the
		 * window during {@link MotionEvent#ACTION_UP}. This the hack that
		 * allows the system to bring the window to the front.
		 */
		public static final int FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the window should be brought to the
		 * front upon user tap.
		 * 
		 * <p>
		 * Note that if you set this flag, there is a noticeable flashing of the
		 * window during {@link MotionEvent#ACTION_UP}. This the hack that
		 * allows the system to bring the window to the front.
		 */
		public static final int FLAG_WINDOW_BRING_TO_FRONT_ON_TAP = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should keep the window's
		 * position within the edges of the screen. If this flag is not set, the
		 * window will be able to be dragged off of the screen.
		 * 
		 * <p>
		 * If this flag is set, the window's {@link Gravity} is recommended to
		 * be {@link Gravity#TOP} | {@link Gravity#LEFT}. If the gravity is
		 * anything other than TOP|LEFT, then even though the window will be
		 * displayed within the edges, it will behave as if the user can drag it
		 * off the screen.
		 * 
		 */
		public static final int FLAG_WINDOW_EDGE_LIMITS_ENABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should tile the window
		 * when it hits the edge of the screen, mimicking the behavior of
		 * Windows 7.
		 * 
		 * <p>
		 * This flag also sets {@link #FLAG_WINDOW_EDGE_LIMITS_ENABLE}.
		 * 
		 */
		public static final int FLAG_WINDOW_EDGE_TILE_ENABLE = FLAG_WINDOW_EDGE_LIMITS_ENABLE
				| 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should keep the window's
		 * aspect ratio constant when resizing.
		 * 
		 * <p>
		 * The aspect ratio will only be enforced in
		 * {@link StandOutWindow#onTouchHandleResize(int, Window, View, MotionEvent)}
		 * . The aspect ratio will not be enforced if you set the width or
		 * height of the window's LayoutParams manually.
		 * 
		 * @see StandOutWindow#onTouchHandleResize(int, Window, View,
		 *      MotionEvent)
		 */
		public static final int FLAG_WINDOW_ASPECT_RATIO_ENABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should resize the window
		 * when it detects a pinch-to-zoom gesture.
		 * 
		 * @see StandOutWindow.Window#onInterceptTouchEvent(MotionEvent)
		 */
		public static final int FLAG_WINDOW_PINCH_RESIZE_ENABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the window does not need focus. If
		 * this flag is set, the system will not take care of setting and
		 * unsetting the focus of windows based on user touch and key events.
		 * 
		 * <p>
		 * You will most likely need focus if your window contains any of the
		 * following: Button, ListView, EditText.
		 * 
		 * <p>
		 * The benefit of disabling focus is that your window will not consume
		 * any key events. Normally, focused windows will consume the Back and
		 * Menu keys.
		 * 
		 * @see {@link StandOutWindow#focus(int)}
		 * @see {@link StandOutWindow#unfocus(int)}
		 * 
		 */
		public static final int FLAG_WINDOW_FOCUSABLE_DISABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should not change the
		 * window's visual state when focus is changed. If this flag is set, the
		 * implementation can choose to change the visual state in
		 * {@link StandOutWindow#onFocusChange(int, Window, boolean)}.
		 * 
		 * @see {@link StandOutWindow.Window#onFocus(boolean)}
		 * 
		 */
		public static final int FLAG_WINDOW_FOCUS_INDICATOR_DISABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should disable all
		 * compatibility workarounds. The default behavior is to run
		 * {@link StandOutWindow.Window#fixCompatibility(View, int)} on the view
		 * returned by the implementation.
		 * 
		 * @see {@link StandOutWindow.Window#fixCompatibility(View, int)}
		 */
		public static final int FLAG_FIX_COMPATIBILITY_ALL_DISABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should disable all
		 * additional functionality. The default behavior is to run
		 * {@link StandOutWindow.Window#addFunctionality(View, int)} on the view
		 * returned by the implementation.
		 * 
		 * @see {@link StandOutWindow#addFunctionality(View, int)}
		 */
		public static final int FLAG_ADD_FUNCTIONALITY_ALL_DISABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should disable adding the
		 * resize handle additional functionality to a custom View R.id.corner.
		 * 
		 * <p>
		 * If {@link #FLAG_DECORATION_SYSTEM} is set, the user will always be
		 * able to resize the window with the default corner.
		 * 
		 * @see {@link StandOutWindow.Window#addFunctionality(View, int)}
		 */
		public static final int FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE = 1 << flag_bit++;

		/**
		 * Setting this flag indicates that the system should disable adding the
		 * drop down menu additional functionality to a custom View
		 * R.id.window_icon.
		 * 
		 * <p>
		 * If {@link #FLAG_DECORATION_SYSTEM} is set, the user will always be
		 * able to show the drop down menu with the default window icon.
		 * 
		 * @see {@link StandOutWindow.Window#addFunctionality(View, int)}
		 */
		public static final int FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE = 1 << flag_bit++;
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
	 * 
	 * @see #show(int)
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
	 * @see #hide(int)
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
	 * @see #close(int)
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
	 * @see #closeAll()
	 */
	public static void closeAll(Context context,
			Class<? extends StandOutWindow> cls) {
		context.startService(getCloseAllIntent(context, cls));
	}

	/**
	 * This allows windows of different applications to communicate with each
	 * other.
	 * 
	 * <p>
	 * Send {@link Parceleable} data in a {@link Bundle} to a new or existing
	 * windows. The implementation of the recipient window can handle what to do
	 * with the data. To receive a result, provide the class and id of the
	 * sender.
	 * 
	 * @param context
	 *            A Context of the application package implementing the class of
	 *            the sending window.
	 * @param toCls
	 *            The Service's class extending {@link StandOutWindow} that is
	 *            managing the receiving window.
	 * @param toId
	 *            The id of the receiving window, or DISREGARD_ID.
	 * @param requestCode
	 *            Provide a request code to declare what kind of data is being
	 *            sent.
	 * @param data
	 *            A bundle of parceleable data to be sent to the receiving
	 *            window.
	 * @param fromCls
	 *            Provide the class of the sending window if you want a result.
	 * @param fromId
	 *            Provide the id of the sending window if you want a result.
	 * @see #sendData(int, Class, int, int, Bundle)
	 */
	public static void sendData(Context context,
			Class<? extends StandOutWindow> toCls, int toId, int requestCode,
			Bundle data, Class<? extends StandOutWindow> fromCls, int fromId) {
		context.startService(getSendDataIntent(context, toCls, toId,
				requestCode, data, fromCls, fromId));
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
		boolean cached = isCached(id, cls);
		String action = cached ? ACTION_RESTORE : ACTION_SHOW;
		Uri uri = cached ? Uri.parse("standout://" + cls + '/' + id) : null;
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

	/**
	 * See {@link #sendData(Context, Class, int, int, Bundle, Class, int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing the class of
	 *            the sending window.
	 * @param toCls
	 *            The Service's class extending {@link StandOutWindow} that is
	 *            managing the receiving window.
	 * @param toId
	 *            The id of the receiving window.
	 * @param requestCode
	 *            Provide a request code to declare what kind of data is being
	 *            sent.
	 * @param data
	 *            A bundle of parceleable data to be sent to the receiving
	 *            window.
	 * @param fromCls
	 *            If the sending window wants a result, provide the class of the
	 *            sending window.
	 * @param fromId
	 *            If the sending window wants a result, provide the id of the
	 *            sending window.
	 * @return An {@link Intnet} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getSendDataIntent(Context context,
			Class<? extends StandOutWindow> toCls, int toId, int requestCode,
			Bundle data, Class<? extends StandOutWindow> fromCls, int fromId) {
		return new Intent(context, toCls).putExtra("id", toId)
				.putExtra("requestCode", requestCode)
				.putExtra("wei.mark.standout.data", data)
				.putExtra("wei.mark.standout.fromCls", fromCls)
				.putExtra("fromId", fromId).setAction(ACTION_SEND_DATA);
	}

	// internal map of ids to shown/hidden views
	private static Map<Class<? extends StandOutWindow>, SparseArray<Window>> sWindows;
	private static Window sFocusedWindow;

	// static constructors
	static {
		sWindows = new HashMap<Class<? extends StandOutWindow>, SparseArray<Window>>();
		sFocusedWindow = null;
	}

	/**
	 * Returns whether the window corresponding to the class and id exists in
	 * the {@link #sWindows} cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param cls
	 *            Class corresponding to the window.
	 * @return True if the window corresponding to the class and id exists in
	 *         the cache, or false if it does not exist.
	 */
	private static boolean isCached(int id, Class<? extends StandOutWindow> cls) {
		return getCache(id, cls) != null;
	}

	/**
	 * Returns the window corresponding to the id from the {@link #sWindows}
	 * cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param cls
	 *            The class of the implementation of the window.
	 * @return The window corresponding to the id if it exists in the cache, or
	 *         null if it does not.
	 */
	private static Window getCache(int id, Class<? extends StandOutWindow> cls) {
		SparseArray<Window> l2 = sWindows.get(cls);
		if (l2 == null) {
			return null;
		}

		return l2.get(id);
	}

	/**
	 * Add the window corresponding to the id in the {@link #sWindows} cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param cls
	 *            The class of the implementation of the window.
	 * @param window
	 *            The window to be put in the cache.
	 */
	private static void putCache(int id, Class<? extends StandOutWindow> cls,
			Window window) {
		SparseArray<Window> l2 = sWindows.get(cls);
		if (l2 == null) {
			l2 = new SparseArray<Window>();
			sWindows.put(cls, l2);
		}

		l2.put(id, window);
	}

	/**
	 * Remove the window corresponding to the id from the {@link #sWindows}
	 * cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param cls
	 *            The class of the implementation of the window.
	 */
	private static void removeCache(int id, Class<? extends StandOutWindow> cls) {
		SparseArray<Window> l2 = sWindows.get(cls);
		if (l2 != null) {
			l2.remove(id);
			if (l2.size() == 0) {
				sWindows.remove(cls);
			}
		}
	}

	/**
	 * Returns the size of the {@link #sWindows} cache.
	 * 
	 * @return True if the cache corresponding to this class is empty, false if
	 *         it is not empty.
	 * @param cls
	 *            The class of the implementation of the window.
	 */
	private static int getCacheSize(Class<? extends StandOutWindow> cls) {
		SparseArray<Window> l2 = sWindows.get(cls);
		if (l2 == null) {
			return 0;
		}

		return l2.size();
	}

	/**
	 * Returns the ids in the {@link #sWindows} cache.
	 * 
	 * @param cls
	 *            The class of the implementation of the window.
	 * @return The ids representing the cached windows.
	 */
	private static Set<Integer> getCacheIds(Class<? extends StandOutWindow> cls) {
		SparseArray<Window> l2 = sWindows.get(cls);
		if (l2 == null) {
			return new HashSet<Integer>();
		}

		Set<Integer> keys = new HashSet<Integer>();
		for (int i = 0; i < l2.size(); i++) {
			keys.add(l2.keyAt(i));
		}
		return keys;
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

			// this will interfere with getPersistentNotification()
			if (id == ONGOING_NOTIFICATION_ID) {
				throw new RuntimeException(
						"ID cannot equals StandOutWindow.ONGOING_NOTIFICATION_ID");
			}

			if (ACTION_SHOW.equals(action) || ACTION_RESTORE.equals(action)) {
				show(id);
			} else if (ACTION_HIDE.equals(action)) {
				hide(id);
			} else if (ACTION_CLOSE.equals(action)) {
				close(id);
			} else if (ACTION_CLOSE_ALL.equals(action)) {
				closeAll();
			} else if (ACTION_SEND_DATA.equals(action)) {
				if (isExistingId(id) || id == DISREGARD_ID) {
					Bundle data = intent
							.getBundleExtra("wei.mark.standout.data");
					int requestCode = intent.getIntExtra("requestCode", 0);
					@SuppressWarnings("unchecked")
					Class<? extends StandOutWindow> fromCls = (Class<? extends StandOutWindow>) intent
							.getSerializableExtra("wei.mark.standout.fromCls");
					int fromId = intent.getIntExtra("fromId", DEFAULT_ID);

					onReceiveData(id, requestCode, data, fromCls, fromId);
				} else {
					Log.w(TAG,
							"Failed to send data to non-existant window. Make sure toId is either an existing window's id, or is DISREGARD_ID.");
				}
			}
		} else {
			Log.w(TAG, "Tried to onStartCommand() with a null intent.");
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
	 * Return the name of every window in this implementation. The name will
	 * appear in the default implementations of the system window decoration
	 * title and notification titles.
	 * 
	 * @return The name.
	 */
	protected abstract String getAppName();

	/**
	 * Return the icon resource for every window in this implementation. The
	 * icon will appear in the default implementations of the system window
	 * decoration and notifications.
	 * 
	 * @return The icon.
	 */
	protected abstract int getAppIcon();

	/**
	 * Create a new {@link View} corresponding to the id, and add it as a child
	 * to the frame. The view will become the contents of this StandOut window.
	 * The view MUST be newly created, and you MUST attach it to the frame.
	 * 
	 * <p>
	 * If you are inflating your view from XML, make sure you use
	 * {@link LayoutInflater#inflate(int, ViewGroup, boolean)} to attach your
	 * view to frame. Set the ViewGroup to be frame, and the boolean to true.
	 * 
	 * <p>
	 * If you are creating your view programmatically, make sure you use
	 * {@link FrameLayout#addView(View)} to add your view to the frame.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param frame
	 *            The {@link FrameLayout} to attach your view as a child to.
	 */
	protected abstract void createAndAttachView(int id, FrameLayout frame);

	/**
	 * Return the {@link StandOutWindow#LayoutParams} for the corresponding id.
	 * The system will set the layout params on the view for this StandOut
	 * window. The layout params may be reused.
	 * 
	 * 
	 * @param id
	 *            The id of the window.
	 * @param window
	 *            The window corresponding to the id. Given as courtesy, so you
	 *            may get the existing layout params.
	 * @return The {@link StandOutWindow#LayoutParams} corresponding to the id.
	 *         The layout params will be set on the window. The layout params
	 *         returned will be reused whenever possible, minimizing the number
	 *         of times getParams() will be called.
	 */
	protected abstract LayoutParams getParams(int id, Window window);

	/**
	 * Implement this method to change modify the behavior and appearance of the
	 * window corresponding to the id.
	 * 
	 * <p>
	 * You may use any of the flags defined in {@link StandOutFlags}. This
	 * method will be called many times, so keep it fast.
	 * 
	 * <p>
	 * Use bitwise OR (|) to set flags, and bitwise XOR (^) to unset flags. To
	 * test if a flag is set, use {@link Utils#isSet(int, int)}.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return A combination of flags.
	 */
	protected int getFlags(int id) {
		return 0;
	}

	/**
	 * Implement this method to set a custom title for the window corresponding
	 * to the id.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The title of the window.
	 */
	protected String getTitle(int id) {
		return getAppName();
	}

	/**
	 * Implement this method to set a custom icon for the window corresponding
	 * to the id.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The icon of the window.
	 */
	protected int getIcon(int id) {
		return getAppIcon();
	}

	/**
	 * Return the title for the persistent notification. This is called every
	 * time {@link #show(int)} is called.
	 * 
	 * @param id
	 *            The id of the window shown.
	 * @return The title for the persistent notification.
	 */
	protected String getPersistentNotificationTitle(int id) {
		return getAppName() + " Running";
	}

	/**
	 * Return the message for the persistent notification. This is called every
	 * time {@link #show(int)} is called.
	 * 
	 * @param id
	 *            The id of the window shown.
	 * @return The message for the persistent notification.
	 */
	protected String getPersistentNotificationMessage(int id) {
		return "";
	}

	/**
	 * Return the intent for the persistent notification. This is called every
	 * time {@link #show(int)} is called.
	 * 
	 * <p>
	 * The returned intent will be packaged into a {@link PendingIntent} to be
	 * invoked when the user clicks the notification.
	 * 
	 * @param id
	 *            The id of the window shown.
	 * @return The intent for the persistent notification.
	 */
	protected Intent getPersistentNotificationIntent(int id) {
		return null;
	}

	/**
	 * Return the icon resource for every hidden window in this implementation.
	 * The icon will appear in the default implementations of the hidden
	 * notifications.
	 * 
	 * @return The icon.
	 */
	protected int getHiddenIcon() {
		return getAppIcon();
	}

	/**
	 * Return the title for the hidden notification corresponding to the window
	 * being hidden.
	 * 
	 * @param id
	 *            The id of the hidden window.
	 * @return The title for the hidden notification.
	 */
	protected String getHiddenNotificationTitle(int id) {
		return getAppName() + " Hidden";
	}

	/**
	 * Return the message for the hidden notification corresponding to the
	 * window being hidden.
	 * 
	 * @param id
	 *            The id of the hidden window.
	 * @return The message for the hidden notification.
	 */
	protected String getHiddenNotificationMessage(int id) {
		return "";
	}

	/**
	 * Return the intent for the hidden notification corresponding to the window
	 * being hidden.
	 * 
	 * <p>
	 * The returned intent will be packaged into a {@link PendingIntent} to be
	 * invoked when the user clicks the notification.
	 * 
	 * @param id
	 *            The id of the hidden window.
	 * @return The intent for the hidden notification.
	 */
	protected Intent getHiddenNotificationIntent(int id) {
		return null;
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
	protected Notification getPersistentNotification(int id) {
		// basic notification stuff
		// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
		int icon = getAppIcon();
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = getPersistentNotificationTitle(id);
		String contentText = getPersistentNotificationMessage(id);
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		// getPersistentNotification() is called for every new window
		// so we replace the old notification with a new one that has
		// a bigger id
		Intent notificationIntent = getPersistentNotificationIntent(id);

		PendingIntent contentIntent = null;

		if (notificationIntent != null) {
			contentIntent = PendingIntent.getService(this, 0,
					notificationIntent,
					// flag updates existing persistent notification
					PendingIntent.FLAG_UPDATE_CURRENT);
		}

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(c, contentTitle, contentText,
				contentIntent);
		return notification;
	}

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
		// same basics as getPersistentNotification()
		int icon = getHiddenIcon();
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = getHiddenNotificationTitle(id);
		String contentText = getHiddenNotificationMessage(id);
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		// the difference here is we are providing the same id
		Intent notificationIntent = getHiddenNotificationIntent(id);

		PendingIntent contentIntent = null;

		if (notificationIntent != null) {
			contentIntent = PendingIntent.getService(this, 0,
					notificationIntent,
					// flag updates existing persistent notification
					PendingIntent.FLAG_UPDATE_CURRENT);
		}

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(c, contentTitle, contentText,
				contentIntent);
		return notification;
	}

	/**
	 * Return the animation to play when the window corresponding to the id is
	 * shown.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The animation to play or null.
	 */
	protected Animation getShowAnimation(int id) {
		return AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
	}

	/**
	 * Return the animation to play when the window corresponding to the id is
	 * hidden.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The animation to play or null.
	 */
	protected Animation getHideAnimation(int id) {
		return AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
	}

	/**
	 * Return the animation to play when the window corresponding to the id is
	 * closed.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The animation to play or null.
	 */
	protected Animation getCloseAnimation(int id) {
		return AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
	}

	/**
	 * Implement this method to set a custom theme for all windows in this
	 * implementation.
	 * 
	 * @return The theme to set on the window, or 0 for device default.
	 */
	protected int getThemeStyle() {
		return 0;
	}

	/**
	 * You probably want to leave this method alone and implement
	 * {@link #getDropDownItems(int)} instead. Only implement this method if you
	 * want more control over the drop down menu.
	 * 
	 * <p>
	 * Implement this method to set a custom drop down menu when the user clicks
	 * on the icon of the window corresponding to the id. The icon is only shown
	 * when {@link StandOutFlags#FLAG_DECORATION_SYSTEM} is set.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The drop down menu to be anchored to the icon, or null to have no
	 *         dropdown menu.
	 */
	protected PopupWindow getDropDown(final int id) {
		final List<DropDownListItem> items;

		List<DropDownListItem> dropDownListItems = getDropDownItems(id);
		if (dropDownListItems != null) {
			items = dropDownListItems;
		} else {
			items = new ArrayList<StandOutWindow.DropDownListItem>();
		}

		// add default drop down items
		items.add(new DropDownListItem(
				android.R.drawable.ic_menu_close_clear_cancel, "Quit "
						+ getAppName(), new Runnable() {

					@Override
					public void run() {
						closeAll();
					}
				}));

		// turn item list into views in PopupWindow
		LinearLayout list = new LinearLayout(this);
		list.setOrientation(LinearLayout.VERTICAL);

		final PopupWindow dropDown = new PopupWindow(list,
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);

		for (final DropDownListItem item : items) {
			ViewGroup listItem = (ViewGroup) mLayoutInflater.inflate(
					R.layout.drop_down_list_item, null);
			list.addView(listItem);

			ImageView icon = (ImageView) listItem.findViewById(R.id.icon);
			icon.setImageResource(item.icon);

			TextView description = (TextView) listItem
					.findViewById(R.id.description);
			description.setText(item.description);

			listItem.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					item.action.run();
					dropDown.dismiss();
				}
			});
		}

		Drawable background = getResources().getDrawable(
				android.R.drawable.editbox_dropdown_dark_frame);
		dropDown.setBackgroundDrawable(background);
		return dropDown;
	}

	/**
	 * Implement this method to populate the drop down menu when the user clicks
	 * on the icon of the window corresponding to the id. The icon is only shown
	 * when {@link StandOutFlags#FLAG_DECORATION_SYSTEM} is set.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The list of items to show in the drop down menu, or null or empty
	 *         to have no dropdown menu.
	 */
	protected List<DropDownListItem> getDropDownItems(int id) {
		return null;
	}

	/**
	 * Implement this method to be alerted to touch events in the body of the
	 * window corresponding to the id.
	 * 
	 * <p>
	 * Note that even if you set {@link #FLAG_DECORATION_SYSTEM}, you will not
	 * receive touch events from the system window decorations.
	 * 
	 * @see {@link View.OnTouchListener#onTouch(View, MotionEvent)}
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param window
	 *            The window corresponding to the id, provided as a courtesy.
	 * @param view
	 *            The view where the event originated from.
	 * @param event
	 *            See linked method.
	 */
	protected boolean onTouchBody(int id, Window window, View view,
			MotionEvent event) {
		return false;
	}

	/**
	 * Implement this method to be alerted to when the window corresponding to
	 * the id is moved.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param window
	 *            The window corresponding to the id, provided as a courtesy.
	 * @param view
	 *            The view where the event originated from.
	 * @param event
	 *            See linked method.
	 * @see {@link #onTouchHandleMove(int, Window, View, MotionEvent)}
	 */
	protected void onMove(int id, Window window, View view, MotionEvent event) {
	}

	/**
	 * Implement this method to be alerted to when the window corresponding to
	 * the id is resized.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param window
	 *            The window corresponding to the id, provided as a courtesy.
	 * @param view
	 *            The view where the event originated from.
	 * @param event
	 *            See linked method.
	 * @see {@link #onTouchHandleResize(int, Window, View, MotionEvent)}
	 */
	protected void onResize(int id, Window window, View view, MotionEvent event) {
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be shown. This callback will occur before the view is
	 * added to the window manager.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be shown.
	 * @return Return true to cancel the view from being shown, or false to
	 *         continue.
	 * @see #show(int)
	 */
	protected boolean onShow(int id, Window window) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be hidden. This callback will occur before the view is
	 * removed from the window manager and {@link #getHiddenNotification(int)}
	 * is called.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be hidden.
	 * @return Return true to cancel the view from being hidden, or false to
	 *         continue.
	 * @see #hide(int)
	 */
	protected boolean onHide(int id, Window window) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be closed. This callback will occur before the view is
	 * removed from the window manager.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be closed.
	 * @return Return true to cancel the view from being closed, or false to
	 *         continue.
	 * @see #close(int)
	 */
	protected boolean onClose(int id, Window window) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when all windows are about to be
	 * closed. This callback will occur before any views are removed from the
	 * window manager.
	 * 
	 * @return Return true to cancel the views from being closed, or false to
	 *         continue.
	 * @see #closeAll()
	 */
	protected boolean onCloseAll() {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id has received some data. The sender is described by fromCls and fromId
	 * if the sender wants a result. To send a result, use
	 * {@link #sendData(int, Class, int, int, Bundle)}.
	 * 
	 * @param id
	 *            The id of your receiving window.
	 * @param requestCode
	 *            The sending window provided this request code to declare what
	 *            kind of data is being sent.
	 * @param data
	 *            A bundle of parceleable data that was sent to your receiving
	 *            window.
	 * @param fromCls
	 *            The sending window's class. Provided if the sender wants a
	 *            result.
	 * @param fromId
	 *            The sending window's id. Provided if the sender wants a
	 *            result.
	 */
	protected void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends StandOutWindow> fromCls, int fromId) {
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be updated in the layout. This callback will occur before
	 * the view is updated by the window manager.
	 * 
	 * @param id
	 *            The id of the window, provided as a courtesy.
	 * @param view
	 *            The window about to be updated.
	 * @param params
	 *            The updated layout params.
	 * @return Return true to cancel the window from being updated, or false to
	 *         continue.
	 * @see #updateViewLayout(int, Window, LayoutParams)
	 */
	protected boolean onUpdate(int id, Window window,
			StandOutWindow.LayoutParams params) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be bought to the front. This callback will occur before
	 * the window is brought to the front by the window manager.
	 * 
	 * @param id
	 *            The id of the window, provided as a courtesy.
	 * @param view
	 *            The window about to be brought to the front.
	 * @return Return true to cancel the window from being brought to the front,
	 *         or false to continue.
	 * @see #bringToFront(int)
	 */
	protected boolean onBringToFront(int id, Window window) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to have its focus changed. This callback will occur before
	 * the window's focus is changed.
	 * 
	 * @param id
	 *            The id of the window, provided as a courtesy.
	 * @param view
	 *            The window about to be brought to the front.
	 * @param focus
	 *            Whether the window is gaining or losing focus.
	 * @return Return true to cancel the window's focus from being changed, or
	 *         false to continue.
	 * @see #focus(int)
	 */
	protected boolean onFocusChange(int id, Window window, boolean focus) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id receives a key event. This callback will occur before the window
	 * handles the event with {@link Window#dispatchKeyEvent(KeyEvent)}.
	 * 
	 * @param id
	 *            The id of the window, provided as a courtesy.
	 * @param view
	 *            The window about to receive the key event.
	 * @param event
	 *            The key event.
	 * @return Return true to cancel the window from handling the key event, or
	 *         false to let the window handle the key event.
	 * @see {@link Window#dispatchKeyEvent(KeyEvent)}
	 */
	protected boolean onKeyEvent(int id, Window window, KeyEvent event) {
		return false;
	}

	/**
	 * Show or restore a window corresponding to the id. Return the window that
	 * was shown/restored.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The window shown.
	 */
	protected final synchronized Window show(int id) {
		// get the window corresponding to the id
		Window cachedWindow = getWindow(id);
		final Window window;

		// check cache first
		if (cachedWindow != null) {
			window = cachedWindow;
		} else {
			window = new Window(id);
		}

		if (window.visibility == Window.VISIBILITY_VISIBLE) {
			throw new IllegalStateException("Tried to show(" + id
					+ ") a window that is already shown.");
		}

		// alert callbacks and cancel if instructed
		if (onShow(id, window)) {
			Log.d(TAG, "Window " + id + " show cancelled by implementation.");
			return null;
		}

		window.visibility = Window.VISIBILITY_VISIBLE;

		// get animation
		Animation animation = getShowAnimation(id);

		// get the params corresponding to the id
		LayoutParams params = window.getLayoutParams();

		try {
			// add the view to the window manager
			mWindowManager.addView(window, params);

			// animate
			if (animation != null) {
				window.getChildAt(0).startAnimation(animation);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// add view to internal map
		putCache(id, getClass(), window);

		// get the persistent notification
		Notification notification = getPersistentNotification(id);

		// show the notification
		if (notification != null) {
			notification.flags = notification.flags
					| Notification.FLAG_NO_CLEAR;

			// only show notification if not shown before
			if (!startedForeground) {
				// tell Android system to show notification
				startForeground(
						getClass().hashCode() + ONGOING_NOTIFICATION_ID,
						notification);
				startedForeground = true;
			} else {
				// update notification if shown before
				mNotificationManager.notify(getClass().hashCode()
						+ ONGOING_NOTIFICATION_ID, notification);
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

		focus(id);

		return window;
	}

	/**
	 * Hide a window corresponding to the id. Show a notification for the hidden
	 * window.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final synchronized void hide(int id) {
		// get the view corresponding to the id
		final Window window = getWindow(id);

		if (window == null) {
			throw new IllegalArgumentException("Tried to hide(" + id
					+ ") a null window.");
		}

		if (window.visibility == Window.VISIBILITY_GONE) {
			throw new IllegalStateException("Tried to hide(" + id
					+ ") a window that is not shown.");
		}

		// alert callbacks and cancel if instructed
		if (onHide(id, window)) {
			Log.w(TAG, "Window " + id + " hide cancelled by implementation.");
			return;
		}

		// check if hide enabled
		if (Utils.isSet(window.flags, StandOutFlags.FLAG_WINDOW_HIDE_ENABLE)) {
			window.visibility = Window.VISIBILITY_TRANSITION;

			// get the hidden notification for this view
			Notification notification = getHiddenNotification(id);

			// get animation
			Animation animation = getHideAnimation(id);

			try {
				// animate
				if (animation != null) {
					animation.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationStart(Animation animation) {
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

						@Override
						public void onAnimationEnd(Animation animation) {
							// remove the window from the window manager
							mWindowManager.removeView(window);
							window.visibility = Window.VISIBILITY_GONE;
						}
					});
					window.getChildAt(0).startAnimation(animation);
				} else {
					// remove the window from the window manager
					mWindowManager.removeView(window);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			// display the notification
			notification.flags = notification.flags
					| Notification.FLAG_NO_CLEAR
					| Notification.FLAG_AUTO_CANCEL;

			mNotificationManager.notify(getClass().hashCode() + id,
					notification);

		} else {
			// if hide not enabled, close window
			close(id);
		}
	}

	/**
	 * Close a window corresponding to the id.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final synchronized void close(final int id) {
		// get the view corresponding to the id
		final Window window = getWindow(id);

		if (window == null) {
			throw new IllegalArgumentException("Tried to close(" + id
					+ ") a null window.");
		}

		if (window.visibility == window.VISIBILITY_TRANSITION) {
			return;
		}

		// alert callbacks and cancel if instructed
		if (onClose(id, window)) {
			Log.w(TAG, "Window " + id + " close cancelled by implementation.");
			return;
		}

		// remove hidden notification
		mNotificationManager.cancel(getClass().hashCode() + id);

		unfocus(window);

		window.visibility = Window.VISIBILITY_TRANSITION;

		// get animation
		Animation animation = getCloseAnimation(id);

		// remove window
		try {
			// animate
			if (animation != null) {
				animation.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						// remove the window from the window manager
						mWindowManager.removeView(window);
						window.visibility = Window.VISIBILITY_GONE;

						// remove view from internal map
						removeCache(id, StandOutWindow.this.getClass());

						// if we just released the last window, quit
						if (getExistingIds().size() == 0) {
							// tell Android to remove the persistent
							// notification
							// the Service will be shutdown by the system on low
							// memory
							startedForeground = false;
							stopForeground(true);
						}
					}
				});
				window.getChildAt(0).startAnimation(animation);
			} else {
				// remove the window from the window manager
				mWindowManager.removeView(window);

				// remove view from internal map
				removeCache(id, getClass());

				// if we just released the last window, quit
				if (getCacheSize(getClass()) == 0) {
					// tell Android to remove the persistent notification
					// the Service will be shutdown by the system on low memory
					startedForeground = false;
					stopForeground(true);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Close all existing windows.
	 */
	protected final synchronized void closeAll() {
		// alert callbacks and cancel if instructed
		if (onCloseAll()) {
			Log.w(TAG, "Windows close all cancelled by implementation.");
			return;
		}

		// add ids to temporary set to avoid concurrent modification
		LinkedList<Integer> ids = new LinkedList<Integer>();
		for (int id : getExistingIds()) {
			ids.add(id);
		}

		// close each window
		for (int id : ids) {
			close(id);
		}
	}

	/**
	 * Send {@link Parceleable} data in a {@link Bundle} to a new or existing
	 * windows. The implementation of the recipient window can handle what to do
	 * with the data. To receive a result, provide the id of the sender.
	 * 
	 * @param fromId
	 *            Provide the id of the sending window if you want a result.
	 * @param toCls
	 *            The Service's class extending {@link StandOutWindow} that is
	 *            managing the receiving window.
	 * @param toId
	 *            The id of the receiving window.
	 * @param requestCode
	 *            Provide a request code to declare what kind of data is being
	 *            sent.
	 * @param data
	 *            A bundle of parceleable data to be sent to the receiving
	 *            window.
	 */
	protected final void sendData(int fromId,
			Class<? extends StandOutWindow> toCls, int toId, int requestCode,
			Bundle data) {
		StandOutWindow.sendData(this, toCls, toId, requestCode, data,
				getClass(), fromId);
	}

	/**
	 * Update the window corresponding to this id with the given params.
	 * 
	 * <p>
	 * This method is now deprecated. You may wish to use {@link Window#edit()}
	 * to get an {@link Editor}, which simplifies resizing and repositioning.
	 * The Editor also will take care of take care of window size and position
	 * constraints, as well as actions that occur when the window touches an
	 * edge of the screen.
	 * 
	 * @param id
	 *            The id of the window.
	 * @param window
	 *            The window to update.
	 * @param params
	 *            The updated layout params to apply.
	 */
	@Deprecated
	protected final void updateViewLayout(int id, Window window,
			LayoutParams params) {
		updateViewLayout(id, params);
	}

	/**
	 * Bring the window corresponding to this id in front of all other windows.
	 * The window may flicker as it is removed and restored by the system.
	 * 
	 * @param id
	 *            The id of the window to bring to the front.
	 */
	protected final synchronized void bringToFront(int id) {
		Window window = getWindow(id);
		if (window == null) {
			throw new IllegalArgumentException("Tried to bringToFront(" + id
					+ ") a null window.");
		}

		if (window.visibility == Window.VISIBILITY_GONE) {
			throw new IllegalStateException("Tried to bringToFront(" + id
					+ ") a window that is not shown.");
		}

		if (window.visibility == Window.VISIBILITY_TRANSITION) {
			return;
		}

		// alert callbacks and cancel if instructed
		if (onBringToFront(id, window)) {
			Log.w(TAG, "Window " + id
					+ " bring to front cancelled by implementation.");
			return;
		}

		LayoutParams params = window.getLayoutParams();

		// remove from window manager then add back
		try {
			mWindowManager.removeView(window);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			mWindowManager.addView(window, params);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Request focus for the window corresponding to this id. A maximum of one
	 * window can have focus, and that window will receive all key events,
	 * including Back and Menu.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return True if focus changed successfully, false if it failed.
	 */
	protected final synchronized boolean focus(int id) {
		// check if that window is focusable
		final Window window = getWindow(id);
		if (window == null) {
			throw new IllegalArgumentException("Tried to focus(" + id
					+ ") a null window.");
		}

		if (!Utils.isSet(window.flags,
				StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE)) {
			// remove focus from previously focused window
			if (sFocusedWindow != null) {
				unfocus(sFocusedWindow);
			}

			return window.onFocus(true);
		}

		return false;
	}

	/**
	 * Remove focus for the window corresponding to this id. Once a window is
	 * unfocused, it will stop receiving key events.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return True if focus changed successfully, false if it failed.
	 */
	protected final synchronized boolean unfocus(int id) {
		Window window = getWindow(id);
		return unfocus(window);
	}

	/**
	 * Courtesy method for your implementation to use if you want to. Gets a
	 * unique id to assign to a new window.
	 * 
	 * @return The unique id.
	 */
	protected final int getUniqueId() {
		int unique = DEFAULT_ID;
		for (int id : getExistingIds()) {
			unique = Math.max(unique, id + 1);
		}
		return unique;
	}

	/**
	 * Return whether the window corresponding to the id exists. This is useful
	 * for testing if the id is being restored (return true) or shown for the
	 * first time (return false).
	 * 
	 * @param id
	 *            The id of the window.
	 * @return True if the window corresponding to the id is either shown or
	 *         hidden, or false if it has never been shown or was previously
	 *         closed.
	 */
	protected final boolean isExistingId(int id) {
		return isCached(id, getClass());
	}

	/**
	 * Return the ids of all shown or hidden windows.
	 * 
	 * @return A set of ids, or an empty set.
	 */
	protected final Set<Integer> getExistingIds() {
		return getCacheIds(getClass());
	}

	/**
	 * Return the window corresponding to the id, if it exists in cache. The
	 * window will not be created with
	 * {@link #createAndAttachView(int, ViewGroup)}. This means the returned
	 * value will be null if the window is not shown or hidden.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The window if it is shown/hidden, or null if it is closed.
	 */
	protected final Window getWindow(int id) {
		return getCache(id, getClass());
	}

	/**
	 * Change the title of the window, if such a title exists. A title exists if
	 * {@link StandOutFlags#FLAG_DECORATION_SYSTEM} is set, or if your own view
	 * contains a TextView with id R.id.title.
	 * 
	 * @param id
	 *            The id of the window.
	 * @param text
	 *            The new title.
	 */
	protected final void setTitle(int id, String text) {
		Window window = getWindow(id);
		if (window != null) {
			View title = window.findViewById(R.id.title);
			if (title instanceof TextView) {
				((TextView) title).setText(text);
			}
		}
	}

	/**
	 * Change the icon of the window, if such a icon exists. A icon exists if
	 * {@link StandOutFlags#FLAG_DECORATION_SYSTEM} is set, or if your own view
	 * contains a TextView with id R.id.window_icon.
	 * 
	 * @param id
	 *            The id of the window.
	 * @param drawableRes
	 *            The new icon.
	 */
	protected final void setIcon(int id, int drawableRes) {
		Window window = getWindow(id);
		if (window != null) {
			View icon = window.findViewById(R.id.window_icon);
			if (icon instanceof ImageView) {
				((ImageView) icon).setImageResource(drawableRes);
			}
		}
	}

	/**
	 * Internal touch handler for handling moving the window.
	 * 
	 * @see {@link View#onTouchEvent(MotionEvent)}
	 * 
	 * @param id
	 * @param window
	 * @param view
	 * @param event
	 * @return
	 */
	private boolean onTouchHandleMove(int id, Window window, View view,
			MotionEvent event) {
		LayoutParams params = window.getLayoutParams();

		// how much you have to move in either direction in order for the
		// gesture to be a move and not tap

		int totalDeltaX = window.touchInfo.lastX - window.touchInfo.firstX;
		int totalDeltaY = window.touchInfo.lastY - window.touchInfo.firstY;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				window.touchInfo.lastX = (int) event.getRawX();
				window.touchInfo.lastY = (int) event.getRawY();

				window.touchInfo.firstX = window.touchInfo.lastX;
				window.touchInfo.firstY = window.touchInfo.lastY;
				break;
			case MotionEvent.ACTION_MOVE:
				int deltaX = (int) event.getRawX() - window.touchInfo.lastX;
				int deltaY = (int) event.getRawY() - window.touchInfo.lastY;

				window.touchInfo.lastX = (int) event.getRawX();
				window.touchInfo.lastY = (int) event.getRawY();

				if (window.touchInfo.moving
						|| Math.abs(totalDeltaX) >= params.threshold
						|| Math.abs(totalDeltaY) >= params.threshold) {
					window.touchInfo.moving = true;

					// if window is moveable
					if (Utils.isSet(window.flags,
							StandOutFlags.FLAG_BODY_MOVE_ENABLE)) {

						// update the position of the window
						if (event.getPointerCount() == 1) {
							params.x += deltaX;
							params.y += deltaY;
						}

						window.edit().setPosition(params.x, params.y).commit();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				window.touchInfo.moving = false;

				if (event.getPointerCount() == 1) {

					// bring to front on tap
					boolean tap = Math.abs(totalDeltaX) < params.threshold
							&& Math.abs(totalDeltaY) < params.threshold;
					if (tap
							&& Utils.isSet(
									window.flags,
									StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP)) {
						StandOutWindow.this.bringToFront(id);
					}
				}

				// bring to front on touch
				else if (Utils.isSet(window.flags,
						StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH)) {
					StandOutWindow.this.bringToFront(id);
				}

				break;
		}

		onMove(id, window, view, event);

		return true;
	}

	/**
	 * Internal touch handler for handling resizing the window.
	 * 
	 * @see {@link View#onTouchEvent(MotionEvent)}
	 * 
	 * @param id
	 * @param window
	 * @param view
	 * @param event
	 * @return
	 */
	private boolean onTouchHandleResize(int id, Window window, View view,
			MotionEvent event) {
		StandOutWindow.LayoutParams params = (LayoutParams) window
				.getLayoutParams();

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				window.touchInfo.lastX = (int) event.getRawX();
				window.touchInfo.lastY = (int) event.getRawY();

				window.touchInfo.firstX = window.touchInfo.lastX;
				window.touchInfo.firstY = window.touchInfo.lastY;
				break;
			case MotionEvent.ACTION_MOVE:
				int deltaX = (int) event.getRawX() - window.touchInfo.lastX;
				int deltaY = (int) event.getRawY() - window.touchInfo.lastY;

				// update the size of the window
				params.width += deltaX;
				params.height += deltaY;

				// keep window between min/max width/height
				if (params.width >= params.minWidth
						&& params.width <= params.maxWidth) {
					window.touchInfo.lastX = (int) event.getRawX();
				}

				if (params.height >= params.minHeight
						&& params.height <= params.maxHeight) {
					window.touchInfo.lastY = (int) event.getRawY();
				}

				window.edit().setSize(params.width, params.height).commit();
				break;
			case MotionEvent.ACTION_UP:
				break;
		}

		onResize(id, window, view, event);

		return true;
	}

	/**
	 * Remove focus for the window, which could belong to another application.
	 * Since we don't allow windows from different applications to directly
	 * interact with each other, except for
	 * {@link #sendData(Context, Class, int, int, Bundle, Class, int)}, this
	 * method is private.
	 * 
	 * @param window
	 *            The window to unfocus.
	 * @return True if focus changed successfully, false if it failed.
	 */
	private synchronized boolean unfocus(Window window) {
		if (window == null) {
			throw new IllegalArgumentException(
					"Tried to unfocus a null window.");
		}
		return window.onFocus(false);
	}

	/**
	 * Update the window corresponding to this id with the given params.
	 * 
	 * @param id
	 *            The id of the window.
	 * @param params
	 *            The updated layout params to apply.
	 */
	private void updateViewLayout(int id, LayoutParams params) {
		Window window = getWindow(id);

		if (window == null) {
			throw new IllegalArgumentException("Tried to updateViewLayout("
					+ id + ") a null window.");
		}

		if (window.visibility == Window.VISIBILITY_GONE) {
			return;
		}

		if (window.visibility == Window.VISIBILITY_TRANSITION) {
			return;
		}

		// alert callbacks and cancel if instructed
		if (onUpdate(id, window, params)) {
			Log.w(TAG, "Window " + id + " update cancelled by implementation.");
			return;
		}

		try {
			window.setLayoutParams(params);
			mWindowManager.updateViewLayout(window, params);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Special view that represents a floating window.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	public class Window extends FrameLayout {
		public static final int VISIBILITY_GONE = 0;
		public static final int VISIBILITY_VISIBLE = 1;
		public static final int VISIBILITY_TRANSITION = 2;
		/**
		 * Context of the window.
		 */
		StandOutWindow context;
		/**
		 * Class of the window, indicating which application the window belongs
		 * to.
		 */
		public Class<? extends StandOutWindow> cls;
		/**
		 * Id of the window.
		 */
		public int id;

		/**
		 * Whether the window is shown, hidden/closed, or in transition.
		 */
		public int visibility;

		/**
		 * Whether the window is focused.
		 */
		public boolean focused;

		/**
		 * Original params from {@link StandOutWindow#getParams(int, Window)}.
		 */
		public StandOutWindow.LayoutParams originalParams;
		/**
		 * Original flags from {@link StandOutWindow#getFlags(int)}.
		 */
		public int flags;

		/**
		 * Touch information of the window.
		 */
		public TouchInfo touchInfo;

		/**
		 * Data attached to the window.
		 */
		public Bundle data;

		public Window(int id) {
			super(StandOutWindow.this);

			setTheme(getThemeStyle());

			this.context = StandOutWindow.this;

			this.cls = context.getClass();
			this.id = id;
			this.originalParams = getParams(id, this);
			this.flags = getFlags(id);
			this.touchInfo = new TouchInfo();
			touchInfo.ratio = (float) originalParams.width
					/ originalParams.height;
			this.data = new Bundle();

			// create the window contents
			View content;
			FrameLayout body;

			if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_SYSTEM)) {
				// requested system window decorations
				content = getSystemDecorations();
				body = (FrameLayout) content.findViewById(R.id.body);
			} else {
				// did not request decorations. will provide own implementation
				content = new FrameLayout(context);
				content.setId(R.id.content);
				body = (FrameLayout) content;
			}

			addView(content);

			body.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// pass all touch events to the implementation
					boolean consumed = false;

					// handle move and bring to front
					consumed = onTouchHandleMove(Window.this.id, Window.this,
							v, event) || consumed;

					// alert implementation
					consumed = onTouchBody(Window.this.id, Window.this, v,
							event) || consumed;

					return consumed;
				}
			});

			// attach the view corresponding to the id from the
			// implementation
			createAndAttachView(id, body);

			// make sure the implementation attached the view
			if (body.getChildCount() == 0) {
				throw new RuntimeException(
						"You must attach your view to the given frame in createAndAttachView()");
			}

			// implement StandOut specific workarounds
			if (!Utils.isSet(flags,
					StandOutFlags.FLAG_FIX_COMPATIBILITY_ALL_DISABLE)) {
				fixCompatibility(body);
			}
			// implement StandOut specific additional functionality
			if (!Utils.isSet(flags,
					StandOutFlags.FLAG_ADD_FUNCTIONALITY_ALL_DISABLE)) {
				addFunctionality(body);
			}

			// attach the existing tag from the frame to the window
			setTag(body.getTag());
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent event) {
			StandOutWindow.LayoutParams params = getLayoutParams();

			// focus window
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (sFocusedWindow != this) {
					focus(id);
				}
			}

			// multitouch
			if (event.getPointerCount() >= 2
					&& Utils.isSet(flags,
							StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)
					&& (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
				touchInfo.scale = 1;
				touchInfo.dist = -1;
				touchInfo.firstWidth = params.width;
				touchInfo.firstHeight = params.height;
				return true;
			}

			return false;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			// handle touching outside
			switch (event.getAction()) {
				case MotionEvent.ACTION_OUTSIDE:
					// unfocus window
					if (sFocusedWindow == this) {
						unfocus(this);
					}

					// notify implementation that ACTION_OUTSIDE occurred
					onTouchBody(id, this, this, event);
					break;
			}

			// handle multitouch
			if (event.getPointerCount() >= 2
					&& Utils.isSet(flags,
							StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)) {
				// 2 fingers or more

				float x0 = event.getX(0);
				float y0 = event.getY(0);
				float x1 = event.getX(1);
				float y1 = event.getY(1);

				double dist = Math.sqrt(Math.pow(x0 - x1, 2)
						+ Math.pow(y0 - y1, 2));

				switch (event.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_MOVE:
						if (touchInfo.dist == -1) {
							touchInfo.dist = dist;
						}
						touchInfo.scale *= dist / touchInfo.dist;
						touchInfo.dist = dist;

						// scale the window with anchor point set to middle
						edit().setAnchorPoint(.5f, .5f)
								.setSize(
										(int) (touchInfo.firstWidth * touchInfo.scale),
										(int) (touchInfo.firstHeight * touchInfo.scale))
								.commit();
						break;
				}
			}

			return true;
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			if (onKeyEvent(id, this, event)) {
				Log.d(TAG, "Window " + id + " key event " + event
						+ " cancelled by implementation.");
				return false;
			}

			if (event.getAction() == KeyEvent.ACTION_UP) {
				switch (event.getKeyCode()) {
					case KeyEvent.KEYCODE_BACK:
						unfocus(this);
						return true;
				}
			}

			return super.dispatchKeyEvent(event);
		}

		/**
		 * Request or remove the focus from this window.
		 * 
		 * @param focus
		 *            Whether we want to gain or lose focus.
		 * @return True if focus changed successfully, false if it failed.
		 */
		public boolean onFocus(boolean focus) {
			if (!Utils
					.isSet(flags, StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE)) {
				// window is focusable

				if (focus == focused) {
					// window already focused/unfocused
					return false;
				}

				focused = focus;

				// alert callbacks and cancel if instructed
				if (context.onFocusChange(id, this, focus)) {
					Log.d(TAG, "Window " + id + " focus change "
							+ (focus ? "(true)" : "(false)")
							+ " cancelled by implementation.");
					focused = !focus;
					return false;
				}

				if (!Utils.isSet(flags,
						StandOutFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE)) {
					// change visual state
					View content = findViewById(R.id.content);
					if (focus) {
						// gaining focus
						content.setBackgroundResource(R.drawable.border_focused);
					} else {
						// losing focus
						if (Utils.isSet(flags,
								StandOutFlags.FLAG_DECORATION_SYSTEM)) {
							// system decorations
							content.setBackgroundResource(R.drawable.border);
						} else {
							// no decorations
							content.setBackgroundResource(0);
						}
					}
				}

				// set window manager params
				StandOutWindow.LayoutParams params = getLayoutParams();
				params.setFocusFlag(focus);
				context.updateViewLayout(id, this, params);

				if (focus) {
					sFocusedWindow = this;
				} else {
					if (sFocusedWindow == this) {
						sFocusedWindow = null;
					}
				}

				return true;
			}
			return false;
		}

		@Override
		public void setLayoutParams(ViewGroup.LayoutParams params) {
			if (params instanceof StandOutWindow.LayoutParams) {
				super.setLayoutParams(params);
			} else {
				throw new IllegalArgumentException(
						"Window"
								+ id
								+ ": LayoutParams must be an instance of StandOutWindow.LayoutParams.");
			}
		}

		/**
		 * Convenience method to start editting the size and position of this
		 * window. Make sure you call {@link Editor#commit()} when you are done
		 * to update the window.
		 * 
		 * @return The Editor associated with this window.
		 */
		public Editor edit() {
			return new Editor();
		}

		@Override
		public StandOutWindow.LayoutParams getLayoutParams() {
			StandOutWindow.LayoutParams params = (StandOutWindow.LayoutParams) super
					.getLayoutParams();
			if (params == null) {
				params = originalParams;
			}
			return params;
		}

		/**
		 * Returns the system window decorations if the implementation sets
		 * {@link #FLAG_DECORATION_SYSTEM}.
		 * 
		 * <p>
		 * The system window decorations support hiding, closing, moving, and
		 * resizing.
		 * 
		 * @return The frame view containing the system window decorations.
		 */
		private View getSystemDecorations() {
			final View decorations = mLayoutInflater.inflate(
					R.layout.system_window_decorators, null);

			// icon
			final ImageView icon = (ImageView) decorations
					.findViewById(R.id.window_icon);
			icon.setImageResource(getAppIcon());
			icon.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					PopupWindow dropDown = getDropDown(id);
					if (dropDown != null) {
						dropDown.showAsDropDown(icon);
					}
				}
			});

			// title
			TextView title = (TextView) decorations.findViewById(R.id.title);
			title.setText(getTitle(id));

			// hide
			View hide = decorations.findViewById(R.id.hide);
			hide.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					hide(id);
				}
			});
			hide.setVisibility(View.GONE);

			// close
			View close = decorations.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					close(id);
				}
			});

			// move
			View titlebar = decorations.findViewById(R.id.titlebar);
			titlebar.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// handle dragging to move
					boolean consumed = onTouchHandleMove(id, Window.this, v,
							event);
					return consumed;
				}
			});

			// resize
			View corner = decorations.findViewById(R.id.corner);
			corner.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// handle dragging to move
					boolean consumed = onTouchHandleResize(id, Window.this, v,
							event);

					return consumed;
				}
			});

			// set window appearance and behavior based on flags
			if (Utils.isSet(flags, StandOutFlags.FLAG_WINDOW_HIDE_ENABLE)) {
				hide.setVisibility(View.VISIBLE);
			}
			if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_CLOSE_DISABLE)) {
				close.setVisibility(View.GONE);
			}
			if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_MOVE_DISABLE)) {
				titlebar.setOnTouchListener(null);
			}
			if (Utils
					.isSet(flags, StandOutFlags.FLAG_DECORATION_RESIZE_DISABLE)) {
				corner.setVisibility(View.GONE);
			}

			return decorations;
		}

		/**
		 * Implement StandOut specific additional functionalities.
		 * 
		 * <p>
		 * Currently, this method does the following:
		 * 
		 * <p>
		 * Attach resize handles: For every View found to have id R.id.corner,
		 * attach an OnTouchListener that implements resizing the window.
		 * 
		 * @param root
		 *            The view hierarchy that is part of the window.
		 */
		private void addFunctionality(View root) {
			// corner for resize
			if (!Utils.isSet(flags,
					StandOutFlags.FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE)) {
				View corner = root.findViewById(R.id.corner);
				if (corner != null) {
					corner.setOnTouchListener(new OnTouchListener() {

						@Override
						public boolean onTouch(View v, MotionEvent event) {
							// handle dragging to move
							boolean consumed = onTouchHandleResize(id,
									Window.this, v, event);

							return consumed;
						}
					});
				}
			}

			// window_icon for drop down
			if (!Utils.isSet(flags,
					StandOutFlags.FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE)) {
				final View icon = root.findViewById(R.id.window_icon);
				if (icon != null) {
					icon.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							PopupWindow dropDown = getDropDown(id);
							if (dropDown != null) {
								dropDown.showAsDropDown(icon);
							}
						}
					});
				}
			}
		}

		/**
		 * Iterate through each View in the view hiearchy and implement StandOut
		 * specific compatibility workarounds.
		 * 
		 * <p>
		 * Currently, this method does the following:
		 * 
		 * <p>
		 * Nothing yet.
		 * 
		 * @param root
		 *            The root view hierarchy to iterate through and check.
		 */
		private void fixCompatibility(View root) {
			Queue<View> queue = new LinkedList<View>();
			queue.add(root);

			View view = null;
			while ((view = queue.poll()) != null) {
				// do nothing yet

				// iterate through children
				if (view instanceof ViewGroup) {
					ViewGroup group = (ViewGroup) view;
					for (int i = 0; i < group.getChildCount(); i++) {
						queue.add(group.getChildAt(i));
					}
				}
			}
		}

		/**
		 * This class holds temporal touch and gesture information. Mainly used
		 * to hold temporary data for onTouchEvent(MotionEvent).
		 * 
		 * @author Mark Wei <markwei@gmail.com>
		 * 
		 */
		public class TouchInfo {
			/**
			 * The state of the window.
			 */
			public int firstX, firstY, lastX, lastY;
			public double dist, scale, firstWidth, firstHeight;
			public float ratio;

			/**
			 * Whether we're past the move threshold already.
			 */
			public boolean moving;

			@Override
			public String toString() {
				return String
						.format("WindowTouchInfo { firstX=%d, firstY=%d,lastX=%d, lastY=%d, firstWidth=%d, firstHeight=%d }",
								firstX, firstY, lastX, lastY, firstWidth,
								firstHeight);
			}
		}

		/**
		 * Convenient way to resize or reposition a Window. The Editor allows
		 * you to easily resize and reposition the window around anchor points.
		 * 
		 * @author Mark Wei <markwei@gmail.com>
		 * 
		 */
		public class Editor {
			/**
			 * Special value for width, height, x, or y positions that
			 * represents that the value should not be changed.
			 */
			public static final int UNCHANGED = Integer.MIN_VALUE;

			/**
			 * Layout params of the window associated with this Editor.
			 */
			StandOutWindow.LayoutParams mParams;

			/**
			 * The relative position of the anchor point. The anchor point is
			 * only used by the {@link Editor}.
			 * 
			 * <p>
			 * The anchor point effects the following methods:
			 * 
			 * <p>
			 * {@link #setSize(int, int)} and {@link #setSize(float, float)}.
			 * The window will expand or shrink around the anchor point.
			 * 
			 * <p>
			 * Values must be between 0 and 1, inclusive. 0 means the left/top,
			 * 0.5 is the center, 1 is the right/bottom.
			 */
			float anchorX, anchorY;

			/**
			 * Width and height of the screen.
			 */
			int displayWidth, displayHeight;

			public Editor() {
				mParams = getLayoutParams();
				anchorX = anchorY = 0;

				Display display = mWindowManager.getDefaultDisplay();
				DisplayMetrics metrics = new DisplayMetrics();
				display.getMetrics(metrics);
				displayWidth = metrics.widthPixels;
				displayHeight = (int) (metrics.heightPixels - 25 * metrics.density);
			}

			public Editor setAnchorPoint(float x, float y) {
				if (x < 0 || x > 1 || y < 0 || y > 1) {
					throw new IllegalArgumentException(
							"Anchor point must be between 0 and 1, inclusive.");
				}

				anchorX = x;
				anchorY = y;

				return this;
			}

			/**
			 * Set the size of this window in absolute pixels.
			 * 
			 * @param width
			 * @param height
			 * @return The same Editor, useful for method chaining.
			 */
			public Editor setSize(int width, int height) {
				return setSize(width, height, false);
			}

			/**
			 * Set the size of this window in absolute pixels.
			 * 
			 * @param width
			 * @param height
			 * @param skip
			 *            Don't call {@link #setPosition(int, int)} to avoid
			 *            stack overflow.
			 * @return The same Editor, useful for method chaining.
			 */
			private Editor setSize(int width, int height, boolean skip) {
				if (mParams != null) {
					if (anchorX < 0 || anchorX > 1 || anchorY < 0
							|| anchorY > 1) {
						throw new IllegalStateException(
								"Anchor point must be between 0 and 1, inclusive.");
					}

					int lastWidth = mParams.width;
					int lastHeight = mParams.height;

					if (width != UNCHANGED) {
						mParams.width = width;
					}
					if (height != UNCHANGED) {
						mParams.height = height;
					}

					// set max width/height
					int maxWidth = mParams.maxWidth;
					int maxHeight = mParams.maxHeight;

					if (Utils.isSet(flags,
							StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
						maxWidth = (int) Math.min(maxWidth, displayWidth);
						maxHeight = (int) Math.min(maxHeight, displayHeight);
					}

					// keep window between min and max
					mParams.width = Math
							.min(Math.max(mParams.width, mParams.minWidth),
									maxWidth);
					mParams.height = Math.min(
							Math.max(mParams.height, mParams.minHeight),
							maxHeight);

					// keep window in aspect ratio
					if (Utils.isSet(flags,
							StandOutFlags.FLAG_WINDOW_ASPECT_RATIO_ENABLE)) {
						int ratioWidth = (int) (mParams.height * touchInfo.ratio);
						int ratioHeight = (int) (mParams.width / touchInfo.ratio);
						if (ratioHeight >= mParams.minHeight
								&& ratioHeight <= mParams.maxHeight) {
							// width good adjust height
							mParams.height = ratioHeight;
						} else {
							// height good adjust width
							mParams.width = ratioWidth;
						}
					}

					if (!skip) {
						// set position based on anchor point
						setPosition((int) (mParams.x + lastWidth * anchorX),
								(int) (mParams.y + lastHeight * anchorY));
					}
				}

				return this;
			}

			/**
			 * Set the position of this window in absolute pixels.
			 * 
			 * @param x
			 * @param y
			 * @return The same Editor, useful for method chaining.
			 */
			public Editor setPosition(int x, int y) {
				return setPosition(x, y, false);
			}

			/**
			 * Set the position of this window in absolute pixels.
			 * 
			 * @param x
			 * @param y
			 * @param skip
			 *            Don't call {@link #setPosition(int, int)} and
			 *            {@link #setSize(int, int)} to avoid stack overflow.
			 * @return The same Editor, useful for method chaining.
			 */
			private Editor setPosition(int x, int y, boolean skip) {
				if (mParams != null) {
					if (anchorX < 0 || anchorX > 1 || anchorY < 0
							|| anchorY > 1) {
						throw new IllegalStateException(
								"Anchor point must be between 0 and 1, inclusive.");
					}

					// sets the x and y correctly according to anchorX and
					// anchorY
					if (x != UNCHANGED) {
						mParams.x = (int) (x - mParams.width * anchorX);
					}
					if (y != UNCHANGED) {
						mParams.y = (int) (y - mParams.height * anchorY);
					}

					if (Utils.isSet(flags,
							StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
						// if gravity is not TOP|LEFT throw exception
						if (mParams.gravity != (Gravity.TOP | Gravity.LEFT)) {
							throw new IllegalStateException(
									"The window "
											+ id
											+ " gravity must be TOP|LEFT if FLAG_WINDOW_EDGE_LIMITS_ENABLE or FLAG_WINDOW_EDGE_TILE_ENABLE is set.");
						}

						// keep window inside edges
						mParams.x = Math.min(Math.max(mParams.x, 0),
								displayWidth - mParams.width);
						mParams.y = Math.min(Math.max(mParams.y, 0),
								displayHeight - mParams.height);

						// tile window if hit edge
						if (Utils.isSet(flags,
								StandOutFlags.FLAG_WINDOW_EDGE_TILE_ENABLE)) {
							boolean left = mParams.x == 0;
							// boolean top = mParams.y == 0;
							// boolean right = mParams.x == displayWidth
							// - mParams.width;
							// boolean bottom = mParams.x == displayHeight
							// - mParams.height;

							if (!skip) {
								if (left) {
									setAnchorPoint(0, 0).setPosition(0,
											displayHeight / 2, true).setSize(
											UNCHANGED, displayHeight, true);
								}

								// if (left && top) {
								// setAnchorPoint(0, 0)
								// .setPosition(0, 0, true).setSize(
								// displayWidth / 2,
								// displayHeight / 2, true);
								// } else if (top) {
								// setAnchorPoint(0, 0).setPosition(
								// displayWidth / 2, 0, true).setSize(
								// displayWidth, UNCHANGED, true);
								// } else if (left) {
								// setAnchorPoint(0, 0).setPosition(0,
								// displayHeight / 2, true).setSize(
								// UNCHANGED, displayHeight, true);
								// }
							}
						}
					}
				}

				return this;
			}

			/**
			 * Commit the changes to this window. Updates the layout. This
			 * Editor cannot be used after you commit.
			 */
			public void commit() {
				if (mParams != null) {
					StandOutWindow.this.updateViewLayout(id, Window.this,
							mParams);
					mParams = null;
				}
			}
		}
	}

	/**
	 * LayoutParams specific to floating StandOut windows.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	protected class LayoutParams extends WindowManager.LayoutParams {
		/**
		 * Special value for x position that represents the left of the screen.
		 */
		public static final int LEFT = 0;
		/**
		 * Special value for y position that represents the top of the screen.
		 */
		public static final int TOP = 0;
		/**
		 * Special value for x position that represents the right of the screen.
		 */
		public static final int RIGHT = Integer.MAX_VALUE;
		/**
		 * Special value for y position that represents the bottom of the
		 * screen.
		 */
		public static final int BOTTOM = Integer.MAX_VALUE;
		/**
		 * Special value for x or y position that represents the center of the
		 * screen.
		 */
		public static final int CENTER = Integer.MIN_VALUE;
		/**
		 * Special value for x or y position which requests that the system
		 * determine the position.
		 */
		public static final int AUTO_POSITION = Integer.MIN_VALUE + 1;

		/**
		 * The distance that distinguishes a tap from a drag.
		 */
		public int threshold;

		/**
		 * Optional constraints of the window.
		 */
		public int minWidth, minHeight, maxWidth, maxHeight;

		/**
		 * @param id
		 *            The id of the window.
		 */
		public LayoutParams(int id) {
			super(200, 200, TYPE_PHONE, LayoutParams.FLAG_NOT_TOUCH_MODAL
					| LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
					PixelFormat.TRANSLUCENT);

			int windowFlags = getFlags(id);

			setFocusFlag(false);

			if (!Utils.isSet(windowFlags,
					StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
				// windows may be moved beyond edges
				flags |= FLAG_LAYOUT_NO_LIMITS;
			}

			x = getX(id, width);
			y = getY(id, height);

			gravity = Gravity.TOP | Gravity.LEFT;

			threshold = 10;
			minWidth = minHeight = 0;
			maxWidth = maxHeight = Integer.MAX_VALUE;
		}

		/**
		 * @param id
		 *            The id of the window.
		 * @param w
		 *            The width of the window.
		 * @param h
		 *            The height of the window.
		 */
		public LayoutParams(int id, int w, int h) {
			this(id);
			width = w;
			height = h;
		}

		/**
		 * @param id
		 *            The id of the window.
		 * @param w
		 *            The width of the window.
		 * @param h
		 *            The height of the window.
		 * @param xpos
		 *            The x position of the window.
		 * @param ypos
		 *            The y position of the window.
		 */
		public LayoutParams(int id, int w, int h, int xpos, int ypos) {
			this(id, w, h);

			if (xpos != AUTO_POSITION) {
				x = xpos;
			}
			if (ypos != AUTO_POSITION) {
				y = ypos;
			}

			Display display = mWindowManager.getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();

			if (x == RIGHT) {
				x = width - w;
			} else if (x == CENTER) {
				x = (width - w) / 2;
			}

			if (y == BOTTOM) {
				y = height - h;
			} else if (y == CENTER) {
				y = (height - h) / 2;
			}
		}

		/**
		 * @param id
		 *            The id of the window.
		 * @param w
		 *            The width of the window.
		 * @param h
		 *            The height of the window.
		 * @param xpos
		 *            The x position of the window.
		 * @param ypos
		 *            The y position of the window.
		 * @param minWidth
		 *            The minimum width of the window.
		 * @param minHeight
		 *            The mininum height of the window.
		 */
		public LayoutParams(int id, int w, int h, int xpos, int ypos,
				int minWidth, int minHeight) {
			this(id, w, h, xpos, ypos);

			this.minWidth = minWidth;
			this.minHeight = minHeight;
		}

		/**
		 * @param id
		 *            The id of the window.
		 * @param w
		 *            The width of the window.
		 * @param h
		 *            The height of the window.
		 * @param xpos
		 *            The x position of the window.
		 * @param ypos
		 *            The y position of the window.
		 * @param minWidth
		 *            The minimum width of the window.
		 * @param minHeight
		 *            The mininum height of the window.
		 * @param threshold
		 *            The touch distance threshold that distinguishes a tap from
		 *            a drag.
		 */
		public LayoutParams(int id, int w, int h, int xpos, int ypos,
				int minWidth, int minHeight, int threshold) {
			this(id, w, h, xpos, ypos, minWidth, minHeight);

			this.threshold = threshold;
		}

		// helper to create cascading windows
		private int getX(int id, int width) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();

			int types = sWindows.size();

			int initialX = 100 * types;
			int variableX = 100 * id;
			int rawX = initialX + variableX;

			return rawX % (displayWidth - width);
		}

		// helper to create cascading windows
		private int getY(int id, int height) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();
			int displayHeight = display.getHeight();

			int types = sWindows.size();

			int initialY = 100 * types;
			int variableY = x + 200 * (100 * id) / (displayWidth - width);

			int rawY = initialY + variableY;

			return rawY % (displayHeight - height);
		}

		private void setFocusFlag(boolean focused) {
			if (focused) {
				flags = flags ^ LayoutParams.FLAG_NOT_FOCUSABLE;
			} else {
				flags = flags | LayoutParams.FLAG_NOT_FOCUSABLE;
			}
		}
	}

	protected class DropDownListItem {
		public int icon;
		public String description;
		public Runnable action;

		public DropDownListItem(int icon, String description, Runnable action) {
			super();
			this.icon = icon;
			this.description = description;
			this.action = action;
		}

		@Override
		public String toString() {
			return description;
		}
	}
}
