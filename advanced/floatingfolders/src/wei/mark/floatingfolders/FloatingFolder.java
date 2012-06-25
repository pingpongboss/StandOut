package wei.mark.floatingfolders;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wei.mark.standout.StandOutWindow;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public final class FloatingFolder extends StandOutWindow {
	private static final int APP_SELECTOR_ID = -2;

	private static final int APP_SELECTOR_CODE = 2;
	private static final int APP_SELECTOR_FINISHED_CODE = 3;
	public static final int STARTUP_CODE = 4;

	PackageManager mPackageManager;
	WindowManager mWindowManager;

	int iconSize;
	int squareWidth;

	Map<Integer, FolderModel> mFolders;

	Animation mFadeOut, mFadeIn;

	public static void showFolders(Context context) {
		sendData(context, FloatingFolder.class, DISREGARD_ID, STARTUP_CODE,
				null, null, DISREGARD_ID);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mPackageManager = getPackageManager();
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		iconSize = (int) getResources().getDimension(
				android.R.dimen.app_icon_size);
		squareWidth = iconSize + 8 * 8;

		mFadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		mFadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

		int duration = 100;
		mFadeOut.setDuration(duration);
		mFadeIn.setDuration(duration);
	}

	@Override
	protected String getAppName() {
		return "Floating Folders";
	}

	@Override
	protected int getAppIcon() {
		return R.drawable.ic_launcher;
	}

	@Override
	protected void createAndAttachView(final int id, FrameLayout frame) {
		LayoutInflater inflater = LayoutInflater.from(this);

		// choose which type of window to show
		if (APP_SELECTOR_ID == id) {
			final View view = inflater.inflate(R.layout.app_selector, frame,
					true);
			final ListView listView = (ListView) view.findViewById(R.id.list);
			final List<ActivityInfo> apps = new ArrayList<ActivityInfo>();

			listView.setClickable(true);

			final AppAdapter adapter = new AppAdapter(this, R.layout.app_row,
					apps);
			listView.setAdapter(adapter);

			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long rowId) {
					Window window = getWindow(id);

					// close self
					close(id);

					ActivityInfo app = (ActivityInfo) parent
							.getItemAtPosition(position);

					// send data back
					if (window.data.containsKey("fromId")) {
						Bundle data = new Bundle();
						data.putParcelable("app", app);
						sendData(id, FloatingFolder.class,
								window.data.getInt("fromId"),
								APP_SELECTOR_FINISHED_CODE, data);
					}
				}
			});

			new Thread(new Runnable() {

				@Override
				public void run() {
					final Intent mainIntent = new Intent(Intent.ACTION_MAIN,
							null);
					mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
					final List<ResolveInfo> resolveApps = getPackageManager()
							.queryIntentActivities(mainIntent, 0);

					Collections.sort(resolveApps,
							new Comparator<ResolveInfo>() {

								@Override
								public int compare(ResolveInfo app1,
										ResolveInfo app2) {
									String label1 = app1.loadLabel(
											mPackageManager).toString();
									String label2 = app2.loadLabel(
											mPackageManager).toString();
									return label1.compareTo(label2);
								}
							});

					for (ResolveInfo resolveApp : resolveApps) {
						apps.add(resolveApp.activityInfo);
					}

					Log.d("FloatingFolder", "before");
					view.post(new Runnable() {

						@Override
						public void run() {
							Log.d("FloatingFolder", "after");
							adapter.notifyDataSetChanged();
						}
					});
				}
			}).start();

			View cancel = view.findViewById(R.id.cancel);
			cancel.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// close self
					close(id);
				}
			});
		} else {
			// id is not app selector
			View view = inflater.inflate(R.layout.folder, frame, true);

			View add = view.findViewById(R.id.add);
			add.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					sendData(id, FloatingFolder.class, APP_SELECTOR_ID,
							APP_SELECTOR_CODE, null);
				}
			});

			FlowLayout flow = (FlowLayout) view.findViewById(R.id.flow);

			if (mFolders == null) {
				loadAllFolders();
			}

			FolderModel folder = mFolders.get(id);
			if (folder != null) {
				for (ActivityInfo app : folder.apps) {
					addAppToFolder(id, app, flow);
				}
			}
		}
	}

	@Override
	protected LayoutParams getParams(int id, Window window) {
		if (APP_SELECTOR_ID == id) {
			return new LayoutParams(id, 400, LayoutParams.FILL_PARENT, 0, 0,
					Gravity.CENTER);
		} else {
			return new LayoutParams(id, 400, 400, 50, 50, 250, 300);
		}
	}

	@Override
	protected int getFlags(int id) {
		if (APP_SELECTOR_ID == id) {
			return super.getFlags(id);
		} else {
			return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
					| StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
					| StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
		}
	}

	@Override
	protected void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends StandOutWindow> fromCls, int fromId) {
		switch (requestCode) {
			case APP_SELECTOR_CODE:
				if (APP_SELECTOR_ID == id) {
					// app selector receives data
					Window window = show(APP_SELECTOR_ID);
					window.data.putInt("fromId", fromId);
				}
				break;
			case APP_SELECTOR_FINISHED_CODE:
				final ActivityInfo app = data.getParcelable("app");
				Log.d("FloatingFolder", "Received app: " + app);

				View window = getWindow(id);
				ViewGroup flow = (ViewGroup) window.findViewById(R.id.flow);

				addAppToFolder(id, app, flow);

				onUserAddApp(id, app);
				break;
			case STARTUP_CODE:
				loadAllFolders();
				if (mFolders.isEmpty()) {
					mFolders.put(DEFAULT_ID, new FolderModel());
					show(DEFAULT_ID);
				} else {
					for (FolderModel folder : mFolders.values()) {
						if (folder.shown) {
							show(folder.id);
						}
					}
				}
				break;
		}
	}

	private void addAppToFolder(int id, ActivityInfo app, ViewGroup flow) {
		View frame = getAppView(id, app);

		flow.addView(frame);
	}

	private void removeAppFromFolder(int id, View frame, ViewGroup flow) {
		flow.removeView(frame);
	}

	private void onUserAddApp(int id, ActivityInfo app) {
		snapToSize(id, -1);

		FolderModel folder = mFolders.get(id);
		folder.apps.add(app);

		FileOutputStream out = null;
		try {
			out = openFileOutput(String.format("folder%d", id), MODE_APPEND);
			ComponentName name = new ComponentName(app.packageName, app.name);

			out.write((name.flattenToString() + "\n").getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void onUserRemoveApp(int id, ActivityInfo app) {
		snapToSize(id, -1);

		List<ActivityInfo> apps = mFolders.get(id).apps;
		apps.remove(app);

		FileOutputStream out = null;
		try {
			out = openFileOutput(String.format("folder%d", id), MODE_PRIVATE);

			for (ActivityInfo appInFolder : apps) {
				ComponentName name = new ComponentName(appInFolder.packageName,
						appInFolder.name);

				out.write((name.flattenToString() + "\n").getBytes());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void loadAllFolders() {
		mFolders = new HashMap<Integer, FolderModel>();
		String[] folders = fileList();
		for (String folderFileName : folders) {

			FileInputStream in = null;
			try {
				if (folderFileName.startsWith("folder")) {
					FolderModel folder = new FolderModel();
					folder.id = Integer.parseInt(folderFileName
							.substring("folder".length()));

					in = openFileInput(folderFileName);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					byte[] b = new byte[1024];
					int bytesRead;
					while ((bytesRead = in.read(b)) != -1) {
						bos.write(b, 0, bytesRead);
					}
					byte[] bytes = bos.toByteArray();
					String appNames = new String(bytes);

					for (String appName : appNames.split("\n")) {
						if (appName.length() > 0) {
							ComponentName name = ComponentName
									.unflattenFromString(appName);
							try {
								ActivityInfo app = mPackageManager
										.getActivityInfo(name, 0);
								folder.apps.add(app);
								mFolders.put(folder.id, folder);
							} catch (NameNotFoundException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private View getAppView(final int id, final ActivityInfo app) {
		LayoutInflater inflater = LayoutInflater.from(this);
		final View frame = inflater.inflate(R.layout.app_square, null);

		frame.setTag(app);

		frame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = mPackageManager
						.getLaunchIntentForPackage(app.packageName);
				startActivity(intent);
			}
		});

		frame.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				ActivityInfo app = (ActivityInfo) v.getTag();
				Log.d("FloatingFolder",
						"Long clicked: " + app.loadLabel(mPackageManager));

				View window = getWindow(id);
				ViewGroup flow = (ViewGroup) window.findViewById(R.id.flow);

				removeAppFromFolder(id, frame, flow);
				onUserRemoveApp(id, app);
				return true;
			}
		});

		ImageView icon = (ImageView) frame.findViewById(R.id.icon);
		icon.setImageDrawable(app.loadIcon(mPackageManager));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				iconSize, iconSize);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		icon.setLayoutParams(params);

		TextView name = (TextView) frame.findViewById(R.id.name);
		name.setText(app.loadLabel(mPackageManager));

		View square = frame.findViewById(R.id.square);
		square.setLayoutParams(new FrameLayout.LayoutParams(squareWidth,
				FrameLayout.LayoutParams.WRAP_CONTENT));

		return frame;
	}

	@Override
	protected void onResize(int id, Window window, View view, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			snapToSize(id, -1);
		}
	}

	private void snapToSize(int id, int cols) {
		Window window = getWindow(id);
		FlowLayout flow = (FlowLayout) window.findViewById(R.id.flow);

		int count = flow.getChildCount();

		if (cols == -1) {
			cols = flow.getCols();
		}

		if (count == 0 || cols == 0) {
			count = 4;
			cols = 2;
		}

		int rows = count / cols;
		if (count % cols > 0) {
			rows++;
		}

		View child = flow.getChildAt(0);
		int width = flow.getLeft()
				+ (((ViewGroup) flow.getParent()).getWidth() - flow.getRight())
				+ cols * child.getWidth();
		int height = flow.getTop()
				+ (((ViewGroup) flow.getParent()).getHeight() - flow
						.getBottom()) + rows * child.getHeight();

		LayoutParams params = window.getLayoutParams();
		params.width = width;
		params.height = height;
		updateViewLayout(id, window, params);
	}

	@Override
	protected boolean onTouchBody(final int id, final Window window,
			final View view, MotionEvent event) {
		Log.d("FloatingFolder", "Event: " + event);
		switch (event.getAction()) {
			case MotionEvent.ACTION_OUTSIDE:
				close(APP_SELECTOR_ID);
				break;
			case MotionEvent.ACTION_MOVE:
				if (id != APP_SELECTOR_ID) {
					final LayoutParams params = (LayoutParams) window
							.getLayoutParams();

					final View folder = window.findViewById(R.id.folder);
					final ImageView screenshot = (ImageView) window
							.findViewById(R.id.preview);

					// if touch edge
					if (params.x <= 0) {
						// first time touch edge
						if (window.shown) {
							window.shown = false;

							final Drawable drawable = getResources()
									.getDrawable(R.drawable.ic_menu_archive);

							screenshot.setImageDrawable(drawable);

							mFadeOut.setAnimationListener(new AnimationListener() {

								@Override
								public void onAnimationStart(Animation animation) {
								}

								@Override
								public void onAnimationRepeat(
										Animation animation) {
								}

								@Override
								public void onAnimationEnd(Animation animation) {
									folder.setVisibility(View.GONE);

									// post so that the folder is invisible
									// before
									// anything else happens
									screenshot.post(new Runnable() {

										@Override
										public void run() {
											// preview should be centered
											// vertically
											params.y = params.y
													+ params.height
													/ 2
													- drawable
															.getIntrinsicHeight()
													/ 2;

											params.width = drawable
													.getIntrinsicWidth();
											params.height = drawable
													.getIntrinsicHeight();

											updateViewLayout(id, window, params);

											screenshot
													.setVisibility(View.VISIBLE);
											screenshot.startAnimation(mFadeIn);
										}
									});
								}
							});

							folder.startAnimation(mFadeOut);
						}
					} else { // not touch edge

						// first time not touch edge
						if (!window.shown) {
							window.shown = true;

							mFadeOut.setAnimationListener(new AnimationListener() {

								@Override
								public void onAnimationStart(Animation animation) {
									Log.d("FloatingFolder", "Animation started");
								}

								@Override
								public void onAnimationRepeat(
										Animation animation) {
								}

								@Override
								public void onAnimationEnd(Animation animation) {
									Log.d("FloatingFolder", "Animation ended");
									screenshot.setVisibility(View.GONE);

									// post so that screenshot is invisible
									// before anything else happens
									screenshot.post(new Runnable() {

										@Override
										public void run() {
											LayoutParams originalParams = getParams(
													id, window);

											Drawable drawable = screenshot
													.getDrawable();
											screenshot.setImageDrawable(null);

											params.y = params.y
													- originalParams.height
													/ 2
													+ drawable
															.getIntrinsicHeight()
													/ 2;

											params.width = originalParams.width;
											params.height = originalParams.height;

											updateViewLayout(id, window, params);

											folder.setVisibility(View.VISIBLE);

											folder.startAnimation(mFadeIn);
										}
									});
								}
							});

							screenshot.startAnimation(mFadeOut);
						}
					}
				}

				break;
		}

		return false;
	}

	protected String getPersistentNotificationMessage(int id) {
		return "Click to close all windows.";
	}

	protected Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getCloseAllIntent(this, FloatingFolder.class);
	}
}
