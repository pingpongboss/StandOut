package wei.mark.standout.constants;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.ui.Window;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

/**
 * Flags to be returned from {@link StandOutWindow#getFlags(int)}.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class StandOutFlags {
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
	 * @see Window#onInterceptTouchEvent(MotionEvent)
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
	 * @see {@link Window#onFocus(boolean)}
	 * 
	 */
	public static final int FLAG_WINDOW_FOCUS_INDICATOR_DISABLE = 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the system should disable all
	 * compatibility workarounds. The default behavior is to run
	 * {@link Window#fixCompatibility(View, int)} on the view
	 * returned by the implementation.
	 * 
	 * @see {@link Window#fixCompatibility(View, int)}
	 */
	public static final int FLAG_FIX_COMPATIBILITY_ALL_DISABLE = 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the system should disable all
	 * additional functionality. The default behavior is to run
	 * {@link Window#addFunctionality(View, int)} on the view
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
	 * @see {@link Window#addFunctionality(View, int)}
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
	 * @see {@link Window#addFunctionality(View, int)}
	 */
	public static final int FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE = 1 << flag_bit++;
}