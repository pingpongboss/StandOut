package wei.mark.standout;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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
	 * Special StandOut window id: You may NOT use this id for any windows.
	 */
	public static final int ONGOING_NOTIFICATION_ID = -1;

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
	 * that the window decorator should NOT provide a close button.
	 */
	public static final int FLAG_DECORATION_CLOSE_DISABLE = 0x00000002;

	/**
	 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag indicates
	 * that the window decorator should NOT provide a resize handle.
	 */
	public static final int FLAG_DECORATION_RESIZE_DISABLE = 0x00000004;

	/**
	 * If {@link #FLAG_DECORATION_SYSTEM} is set, setting this flag indicates
	 * that the window decorator should NOT provide a resize handle.
	 */
	public static final int FLAG_DECORATION_MOVE_DISABLE = 0x00000008;

	/**
	 * Setting this flag indicates that the window can be moved by dragging the
	 * body.
	 * 
	 * <p>
	 * Note that if {@link #FLAG_DECORATION_SYSTEM} is set, the window can
	 * always be moved by dragging the titlebar.
	 */
	public static final int FLAG_BODY_MOVE_ENABLE = 0x00000010;

	/**
	 * Setting this flag indicates that the window should be brought to the
	 * front upon user interaction.
	 * 
	 * <p>
	 * Note that if you set this flag, there is a noticeable flashing of the
	 * window during {@link MotionEvent#ACTION_UP}. This the hack that allows
	 * the system to bring the window to the front.
	 */
	public static final int FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH = 0x00000020;

	/**
	 * Setting this flag indicates that the window should be brought to the
	 * front upon user tap.
	 * 
	 * <p>
	 * Note that if you set this flag, there is a noticeable flashing of the
	 * window during {@link MotionEvent#ACTION_UP}. This the hack that allows
	 * the system to bring the window to the front.
	 */
	public static final int FLAG_WINDOW_BRING_TO_FRONT_ON_TAP = 0x00000040;

	/**
	 * Setting this flag indicates that windows are able to be hidden, that
	 * {@link #getHiddenIcon(int)}, {@link #getHiddenTitle(int)}, and
	 * {@link #getHiddenMessage(int)} are implemented, and that the system
	 * window decorator should provide a hide button if
	 * {@link #FLAG_DECORATION_SYSTEM} is set.
	 */
	public static final int FLAG_HIDE_ENABLE = 0x00000080;

	/**
	 * Setting this flag indicates that the system should disable all
	 * compatibility workarounds. The default behavior is to run
	 * {@link #fixCompatibility(View)} on the view returned by the
	 * implementation.
	 * 
	 * @see #fixCompatibility(View)
	 */
	public static final int FLAG_FIX_COMPATIBILITY_ALL_DISABLE = 0x00000100;

	/**
	 * Setting this flag indicates that the system should disable EditText
	 * compatibility workarounds.
	 * 
	 * @see #FLAG_FIX_COMPATIBILITY_ALL_DISABLE
	 */
	public static final int FLAG_FIX_COMPATIBILITY_EDITTEXT_DISABLE = 0x00000200;

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
		boolean cached = isCached(cls, id);
		String action = cached ? ACTION_RESTORE : ACTION_SHOW;
		Uri uri = cached ? Uri.parse("standout://" + cls + '/' + id) : null;
		Log.d("StandOutWindow", "getShowIntent() gets "
				+ (cached ? "restore" : "show"));
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
	private static Map<Class<? extends StandOutWindow>, Map<Integer, View>> sViews;

	// static constructors
	static {
		sViews = new HashMap<Class<? extends StandOutWindow>, Map<Integer, View>>();
	}

	/**
	 * Returns whether the window corresponding to the class and id exists in
	 * the {@link #sViews} cache.
	 * 
	 * @param cls
	 *            Class corresponding to the window.
	 * @param id
	 *            The id representing the window.
	 * @return True if the window corresponding to the class and id exists in
	 *         the cache, or false if it does not exist.
	 */
	private static boolean isCached(Class<? extends StandOutWindow> cls, int id) {
		Map<Integer, View> l2 = sViews.get(cls);
		return l2 != null && l2.containsKey(id);
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
				Bundle data = intent.getBundleExtra("wei.mark.standout.data");
				int requestCode = intent.getIntExtra("requestCode", 0);
				@SuppressWarnings("unchecked")
				Class<? extends StandOutWindow> fromCls = (Class<? extends StandOutWindow>) intent
						.getSerializableExtra("wei.mark.standout.fromCls");
				int fromId = intent.getIntExtra("fromId", DEFAULT_ID);

				onReceiveData(id, requestCode, data, fromCls, fromId);
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
	 * to the root. The view will become the contents of this StandOut window.
	 * The view MUST be newly created, and you MUST attach it to root.
	 * 
	 * <p>
	 * If you are inflating your view from XML, make sure you use
	 * {@link LayoutInflater#inflate(int, ViewGroup, boolean)} to attach your
	 * view to root. Set the ViewGroup to be root, and the boolean to true.
	 * 
	 * <p>
	 * If you are creating your view programmatically, make sure you use
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
	 * @param touchInfo
	 *            The touch information of the window, provided as a courtesy.
	 * @param view
	 *            The view where the event originated from.
	 * @param event
	 *            See linked method.
	 */
	protected boolean onTouchBody(int id, View window,
			WindowTouchInfo touchInfo, View view, MotionEvent event) {
		return false;
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
	protected boolean onShow(int id, View window) {
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
	protected boolean onHide(int id, View window) {
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
	protected boolean onClose(int id, View window) {
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
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be closed.
	 * @param params
	 *            The updated layout params.
	 * @return Return true to cancel the view from being updated, or false to
	 *         continue.
	 * @see #updateViewLayout(int, View, LayoutParams)
	 */
	protected boolean onUpdate(int id, View window,
			StandOutWindow.LayoutParams params) {
		return false;
	}

	/**
	 * Implement this callback to be alerted when a window corresponding to the
	 * id is about to be bought to the front. This callback will occur before
	 * the view is brought to the front by the window manager.
	 * 
	 * @param id
	 *            The id of the view, provided as a courtesy.
	 * @param view
	 *            The view about to be brought to the front.
	 * @return Return true to cancel the view from being brought to the front,
	 *         or false to continue.
	 * @see #bringToFront(int)
	 */
	protected boolean onBringToFront(int id, View window) {
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
	protected final View show(int id) {
		// get the view corresponding to the id
		View window = getWrappedView(id);

		if (window == null) {
			Log.w("StandOutWindow", "Tried to show(" + id + ") a null view");
			return null;
		}

		// alert callbacks and cancel if instructed
		if (onShow(id, window))
			return null;

		// get animation
		Animation animation = getShowAnimation(id);

		WrappedTag tag = (WrappedTag) window.getTag();
		tag.shown = true;

		// add view to internal map
		putCache(id, window);

		// get the params corresponding to the id
		StandOutWindow.LayoutParams params = (LayoutParams) window
				.getLayoutParams();
		if (params == null) {
			params = getParams(id, window);
		}

		try {
			// add the view to the window manager
			mWindowManager.addView(window, params);

			// animate
			if (animation != null) {
				((ViewGroup) window).getChildAt(0).startAnimation(animation);
			}
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

		return window;
	}

	/**
	 * Hide a window corresponding to the id. Show a notification for the hidden
	 * window.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	protected final void hide(int id) {
		int flags = getFlags(id);

		// check if hide enabled
		if ((flags & FLAG_HIDE_ENABLE) != 0) {
			// get the hidden notification for this view
			Notification notification = getHiddenNotification(id);

			// get the view corresponding to the id
			final View window = getWrappedView(id);

			if (window == null) {
				Log.w("StandOutWindow", "Tried to hide(" + id + ") a null view");
				return;
			}

			// alert callbacks and cancel if instructed
			if (onHide(id, window))
				return;

			// get animation
			Animation animation = getHideAnimation(id);

			WrappedTag tag = (WrappedTag) window.getTag();
			tag.shown = false;

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
							// remove the view from the window manager
							mWindowManager.removeView(window);
						}
					});
					((ViewGroup) window).getChildAt(0)
							.startAnimation(animation);
				} else {
					// remove the view from the window manager
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
	protected final void close(int id) {
		// get the view corresponding to the id
		final View window = getWrappedView(id);

		if (window == null) {
			Log.w("StandOutWindow", "Tried to close(" + id + ") a null view");
			return;
		}

		// alert callbacks and cancel if instructed
		if (onClose(id, window))
			return;

		// get animation
		Animation animation = getCloseAnimation(id);

		WrappedTag tag = (WrappedTag) window.getTag();

		if (tag.shown) {
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
							// remove the view from the window manager
							mWindowManager.removeView(window);
						}
					});
					((ViewGroup) window).getChildAt(0)
							.startAnimation(animation);
				} else {
					// remove the view from the window manager
					mWindowManager.removeView(window);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			tag.shown = false;
			// cancel hidden notification
			mNotificationManager.cancel(id);
		}

		// remove view from internal map
		removeCache(id);

		// if we just released the last view, quit
		if (getCacheSize() == 0) {
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
		// alert callbacks and cancel if instructed
		if (onCloseAll())
			return;

		// add ids to temporary set to avoid concurrent modification
		LinkedList<Integer> ids = new LinkedList<Integer>();
		for (int id : getCacheIds()) {
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
	 *            If your window wants a result, provide the id of the sending
	 *            window.
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
	 * Update the window corresponding to this view with the given params.
	 * 
	 * @param window
	 *            The window to update.
	 * @param params
	 *            The updated layout params to apply.
	 */
	protected final void updateViewLayout(int id, View window,
			StandOutWindow.LayoutParams params) {
		// alert callbacks and cancel if instructed
		if (onUpdate(id, window, params))
			return;

		if (window == null) {
			Log.w("StandOutWindow", "Tried to updateViewLayout() a null window");
			return;
		}

		try {
			mWindowManager.updateViewLayout(window, params);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Bring the window corresponding to this id in front of all other windows.
	 * The window may flicker as it is removed and restored by the system.
	 * 
	 * @param id
	 *            The id of the window to bring to the front.
	 */
	protected final void bringToFront(int id) {
		View window = getWrappedView(id);
		if (window == null) {
			Log.w("StandOutWindow", "Tried to bringToFront() a null view");
			return;
		}

		// alert callbacks and cancel if instructed
		if (onBringToFront(id, window))
			return;

		StandOutWindow.LayoutParams params = (LayoutParams) window
				.getLayoutParams();
		if (params == null) {
			params = getParams(id, window);
		}

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
	 * Gets a unique id to assign to a new window.
	 * 
	 * @return The unique id.
	 */
	protected final int getUniqueId() {
		Map<Integer, View> l2 = sViews.get(getClass());
		if (l2 == null) {
			l2 = new HashMap<Integer, View>();
			sViews.put(getClass(), l2);
		}

		int unique = DEFAULT_ID;
		for (int id : l2.keySet()) {
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
		return isCached(getClass(), id);
	}

	/**
	 * Return the window corresponding to the id, if it exists in cache. The
	 * window will not be created with
	 * {@link #createAndAttachView(int, ViewGroup)}. This means the returned
	 * value will be null if the window is not shown/hidden.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The window if it is shown/hidden, or null if it is closed.
	 */
	protected final View getWindow(int id) {
		return getCache(id);
	}

	/**
	 * Wraps the view from getView() into a frame that is easier to manage. The
	 * frame allows us to pass touch input to implementations and set a
	 * {@link WrappedTag} to keep track of the id, visibility, etc.
	 * 
	 * @param id
	 *            The id representing the wrapped view.
	 * @return The wrapped view from cache, or created from the implementation.
	 */
	private View getWrappedView(int id) {
		// try get the wrapped view from the internal map
		View cachedView = getCache(id);

		// if the wrapped view exists, then return it rather than creating one
		if (cachedView != null) {
			return cachedView;
		}

		// create the wrapping frame and body
		final FrameLayout window = new FrameLayout(this);
		View content;
		FrameLayout body;

		int flags = getFlags(id);

		if ((flags & FLAG_DECORATION_SYSTEM) != 0) {
			// requested system window decorations
			content = getSystemWindowContent(id);
			body = (FrameLayout) content.findViewById(R.id.body);
		} else {
			// did not request decorations. will provide own implementation
			content = new FrameLayout(this);
			body = (FrameLayout) content;
		}

		window.addView(content);
		content.setTag(window);

		// body should always send touch events to onTouchBody()
		final boolean bodyMoveEnabled = (flags & FLAG_BODY_MOVE_ENABLE) != 0;
		body.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// pass all touch events to the implementation
				WrappedTag tag = (WrappedTag) window.getTag();
				int id = tag.id;
				WindowTouchInfo touchInfo = tag.touchInfo;

				boolean consumed = onTouchBody(id, window, touchInfo, v, event);

				consumed = onTouchWindow(id, window, touchInfo, v, event)
						|| consumed;

				// if set FLAG_BODY_MOVE_ENABLE, move the window
				if (bodyMoveEnabled) {
					consumed = onTouchHandleMove(id, window, touchInfo, v,
							event) || consumed;
				}

				return consumed;
			}
		});

		// attach the view corresponding to the id from the implementation
		View view = createAndAttachView(id, body);

		// make sure the implemention created a view
		if (view == null) {
			throw new RuntimeException(
					"Your view must not be null in createAndAttachView()");
		}
		// make sure the implementation attached the view
		if (view != body && view.getParent() == null) {
			throw new RuntimeException(
					"You must attach your view to the given root ViewGroup in createAndAttachView()");
		}

		// clean up view and implement StandOut specific workarounds
		if ((flags & FLAG_FIX_COMPATIBILITY_ALL_DISABLE) == 0) {
			fixCompatibility(view, id);
		}

		// wrap the existing tag and attach it to the frame
		window.setTag(new WrappedTag(id, false, view.getTag()));

		window.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d("StandOutWindow", "Event: " + event);
				return false;
			}
		});

		return window;
	}

	/**
	 * Iterate through each View in the view hiearchy and implement StandOut
	 * specific compatibility workarounds.
	 * 
	 * <p>
	 * Currently, this method does the following:
	 * 
	 * <p>
	 * Fix {@link EditText} because they are not focusable. To make EditText
	 * usable, hook up an onclick listener and start a new Activity for the sole
	 * purpose of getting user input.
	 * 
	 * @param root
	 *            The root view hiearchy to iterate through and check.
	 */
	private void fixCompatibility(View root, final int id) {
		Queue<View> queue = new LinkedList<View>();
		queue.add(root);

		int flags = getFlags(id);

		View view = null;
		while ((view = queue.poll()) != null) {
			// fix EditText
			if (view instanceof EditText
					&& (flags & FLAG_FIX_COMPATIBILITY_EDITTEXT_DISABLE) == 0) {
				final EditText editText = (EditText) view;

				// when user clicks edittext, show FixEditTextActivity helper
				editText.setOnFocusChangeListener(new OnFocusChangeListener() {

					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							FixEditTextActivity.edit = editText;
							try {
								String text = editText.getText().toString();
								int caret = editText.getSelectionStart();

								Log.d("StandOutWindow", "Touch text: " + text
										+ " caret: " + caret);
								startActivity(new Intent(StandOutWindow.this,
										FixEditTextActivity.class)
										.addFlags(
												Intent.FLAG_ACTIVITY_NEW_TASK
														| Intent.FLAG_ACTIVITY_SINGLE_TOP)
										.putExtra("text", text)
										.putExtra("caret", caret));
							} catch (ActivityNotFoundException ex) {
								ex.printStackTrace();
								Log.e("StandOutWindow",
										"EditText can only be used in StandOut windows after applying a workaround.\n"
												+ "Please edit your AndroidManifest.xml to include the following Activity:\n"
												+ "<activity "
												+ "android:name=\"wei.mark.standout.FixEditTextActivity\" "
												+ "android:excludeFromRecents=\"true\" "
												+ "android:theme=\"@android:style/Theme.Translucent.NoTitleBar.Fullscreen\" > "
												+ "</activity> ");
							}
						}
					}
				});

				// since one edittext is always already focused, use onClick to
				// trigger the focus change listener
				editText.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (v.isFocused() && FixEditTextActivity.edit == null) {
							v.clearFocus();
						}
					}
				});
			}

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
	private View getSystemWindowContent(final int id) {
		final View content = mLayoutInflater.inflate(
				R.layout.system_window_decorators, null);

		// icon
		ImageView icon = (ImageView) content.findViewById(R.id.icon);
		icon.setImageResource(getAppIcon());

		// title
		TextView title = (TextView) content.findViewById(R.id.title);
		title.setText(getAppName());

		// hide
		View hide = content.findViewById(R.id.hide);
		hide.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("StandOutHelloWorld", "Minimize button clicked: " + id);
				hide(id);
			}
		});
		hide.setVisibility(View.GONE);

		// close
		View close = content.findViewById(R.id.close);
		close.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("StandOutHelloWorld", "Close button clicked: " + id);
				close(id);
			}
		});

		// move
		View titlebar = content.findViewById(R.id.titlebar);
		titlebar.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				View window = (View) content.getTag();
				WindowTouchInfo touchInfo = ((WrappedTag) window.getTag()).touchInfo;
				// handle dragging to move
				boolean consumed = onTouchWindow(id, window, touchInfo, v,
						event);
				consumed = onTouchHandleMove(id, window, touchInfo, v, event)
						|| consumed;
				return consumed;
			}
		});

		// resize
		View corner = content.findViewById(R.id.corner);
		corner.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (id) {
					default:
						View window = (View) content.getTag();
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

						// update the position of the window
						params.width = Math.max(0, touchInfo.width
								+ touchInfo.deltaX);
						params.height = Math.max(0, touchInfo.height
								+ touchInfo.deltaY);
						updateViewLayout(id, window, params);

						return true;
				}
			}
		});

		// set window appearance and behavior based on flags
		int flags = getFlags(id);

		if ((flags & FLAG_HIDE_ENABLE) != 0) {
			hide.setVisibility(View.VISIBLE);
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

		return content;
	}

	/**
	 * Generic internal touch handler for window-level views such as the
	 * titlebar and body.
	 * 
	 * @see {@link View#onTouchEvent(MotionEvent)}
	 * 
	 * @param id
	 * @param window
	 * @param view
	 * @param event
	 * @return
	 */
	private boolean onTouchWindow(int id, View window,
			WindowTouchInfo touchInfo, View view, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				int flags = getFlags(id);

				boolean tap = touchInfo.deltaX == 0 && touchInfo.deltaY == 0;
				if ((flags & FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH) != 0) {
					bringToFront(id);
				} else if ((flags & FLAG_WINDOW_BRING_TO_FRONT_ON_TAP) != 0
						&& tap) {
					bringToFront(id);
				}
				break;
		}

		return false;
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
	private boolean onTouchHandleMove(int id, View window,
			WindowTouchInfo touchInfo, View view, MotionEvent event) {
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
				touchInfo.deltaX = (int) event.getRawX() - touchInfo.downX;
				touchInfo.deltaY = (int) event.getRawY() - touchInfo.downY;
				break;
			case MotionEvent.ACTION_UP:
				touchInfo.x = touchInfo.x + touchInfo.deltaX;
				touchInfo.y = touchInfo.y + touchInfo.deltaY;

				touchInfo.deltaX = touchInfo.deltaY = 0;
				touchInfo.downX = touchInfo.downY = 0;
				break;
		}

		// update the position of the window
		params.x = touchInfo.x + touchInfo.deltaX;
		params.y = touchInfo.y + touchInfo.deltaY;
		updateViewLayout(id, window, params);

		return true;
	}

	/**
	 * Add the window corresponding to the id in the {@link #sViews} cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @param window
	 *            The window to be put in the cache.
	 */
	private void putCache(int id, View window) {
		HashMap<Integer, View> l2 = (HashMap<Integer, View>) sViews
				.get(getClass());
		if (l2 == null) {
			l2 = new HashMap<Integer, View>();
			sViews.put(getClass(), l2);
		}

		l2.put(id, window);
	}

	/**
	 * Remove the window corresponding to the id from the {@link #sViews} cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 */
	private void removeCache(int id) {
		HashMap<Integer, View> l2 = (HashMap<Integer, View>) sViews
				.get(getClass());
		if (l2 == null) {
			l2 = new HashMap<Integer, View>();
			sViews.put(getClass(), l2);
		}

		l2.remove(id);
	}

	/**
	 * Returns whether the {@link #sViews} cache is empty.
	 * 
	 * @return True if the cache corresponding to this class is empty, false if
	 *         it is not empty.
	 */
	private int getCacheSize() {
		HashMap<Integer, View> l2 = (HashMap<Integer, View>) sViews
				.get(getClass());
		if (l2 == null) {
			l2 = new HashMap<Integer, View>();
			sViews.put(getClass(), l2);
		}

		return l2.size();
	}

	/**
	 * Returns the ids in the {@link #sViews} cache.
	 * 
	 * @return The ids representing the cached windows.
	 */
	private Set<Integer> getCacheIds() {
		HashMap<Integer, View> l2 = (HashMap<Integer, View>) sViews
				.get(getClass());
		if (l2 == null) {
			l2 = new HashMap<Integer, View>();
			sViews.put(getClass(), l2);
		}

		return l2.keySet();
	}

	/**
	 * Returns the window corresponding to the id from the {@link #sViews}
	 * cache.
	 * 
	 * @param id
	 *            The id representing the window.
	 * @return The window corresponding to the id if it exists in the cache, or
	 *         null if it does not.
	 */
	private View getCache(int id) {
		HashMap<Integer, View> l2 = (HashMap<Integer, View>) sViews
				.get(getClass());
		if (l2 == null) {
			l2 = new HashMap<Integer, View>();
			sViews.put(getClass(), l2);
		}

		return l2.get(id);
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
		 * Id of the window.
		 */
		public int id;

		/**
		 * Whether the window is shown or hidden/closed.
		 */
		public boolean shown;

		/**
		 * Touch information of the window.
		 */
		public WindowTouchInfo touchInfo;

		/**
		 * Data attached to the window.
		 */
		public Bundle data;

		/**
		 * Original tag of the wrapped view.
		 */
		public Object tag;

		public WrappedTag(int id, boolean shown, Object tag) {
			super();
			this.id = id;
			this.shown = shown;
			this.touchInfo = new WindowTouchInfo();
			this.data = new Bundle();
			this.tag = tag;
		}
	}

	/**
	 * This class holds temporal touch and gesture information.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	public class WindowTouchInfo {
		/**
		 * The state of the window on ACTION_DOWN.
		 */
		public int x, y, width, height;
		/**
		 * The touch location on ACTION_DOWN.
		 */
		public int downX, downY;
		/**
		 * The delta between the current touch location and the touch location
		 * on ACTION_DOWN.
		 */
		public int deltaX, deltaY;
	}

	/**
	 * LayoutParams specific to floating StandOut windows.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	protected class LayoutParams extends WindowManager.LayoutParams {
		public LayoutParams() {
			super(200, 200, TYPE_SYSTEM_ALERT, FLAG_NOT_FOCUSABLE
					| FLAG_ALT_FOCUSABLE_IM, PixelFormat.TRANSLUCENT);

			x = getX(width);
			y = getY(height);

			gravity = Gravity.TOP | Gravity.LEFT;
		}

		public LayoutParams(int w, int h) {
			this();
			width = w;
			height = h;

			x = getX(width);
			y = getY(height);
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

		private int getX(int width) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();

			return (100 * getCacheSize()) % (displayWidth - width);
		}

		private int getY(int height) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();
			int displayHeight = display.getHeight();

			return (x + 200 * (100 * getCacheSize()) / (displayWidth - width))
					% (displayHeight - height);
		}
	}
}
