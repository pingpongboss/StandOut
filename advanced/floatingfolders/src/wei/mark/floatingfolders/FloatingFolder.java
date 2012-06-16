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
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
	protected View createAndAttachView(final int id, ViewGroup root) {
		LayoutInflater inflater = LayoutInflater.from(this);

		// choose which type of window to show
		if (APP_SELECTOR_ID == id) {
			final View view = inflater.inflate(R.layout.app_selector, root, true);
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
					View window = getWindow(id);

					// close self
					close(id);

					ActivityInfo app = (ActivityInfo) parent
							.getItemAtPosition(position);

					// send data back
					Bundle windowData = ((WrappedTag) window.getTag()).data;
					if (windowData.containsKey("fromId")) {
						Bundle data = new Bundle();
						data.putParcelable("app", app);
						sendData(id, FloatingFolder.class,
								windowData.getInt("fromId"),
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

					listView.post(new Runnable() {

						@Override
						public void run() {
							ProgressBar progress = (ProgressBar) view
									.findViewById(R.id.progress);
							progress.setVisibility(View.GONE);
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

			return view;
		} else {
			View view = inflater.inflate(R.layout.folder, root, true);

			View add = view.findViewById(R.id.add);
			add.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					sendData(id, FloatingFolder.class, APP_SELECTOR_ID,
							APP_SELECTOR_CODE, null);
				}
			});

			ViewGroup flow = (ViewGroup) view.findViewById(R.id.flow);

			if (mFolders == null) {
				loadAllFolders();
			}

			FolderModel folder = mFolders.get(id);
			if (folder != null) {
				for (ActivityInfo app : folder.apps) {
					addAppToFolder(id, app, flow);
				}
			}

			return view;
		}
	}

	@Override
	protected LayoutParams getParams(int id, View view) {
		if (APP_SELECTOR_ID == id) {
			return new LayoutParams(id, 400, LayoutParams.FILL_PARENT, 0, 0,
					Gravity.CENTER);
		} else {
			return new LayoutParams(id, 400, 400);
		}
	}

	@Override
	protected int getFlags(int id) {
		if (APP_SELECTOR_ID == id) {
			return super.getFlags(id);
			// | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE;
		} else {
			return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE;
		}
	}

	@Override
	protected void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends StandOutWindow> fromCls, int fromId) {
		switch (requestCode) {
			case APP_SELECTOR_CODE:
				if (APP_SELECTOR_ID == id) {
					// app selector receives data
					View window = show(APP_SELECTOR_ID);
					WrappedTag tag = (WrappedTag) window.getTag();
					tag.data.putInt("fromId", fromId);
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
		View frame = getAppView(app);

		flow.addView(frame);
	}

	private void onUserAddApp(int id, ActivityInfo app) {
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
					folder.apps = new ArrayList<ActivityInfo>();
					folder.shown = true;
					folder.name = "";

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

	private View getAppView(final ActivityInfo app) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View frame = inflater.inflate(R.layout.app_square, null);

		frame.setTag(app);
		frame.setOnLongClickListener(appLongClickListener);
		frame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = mPackageManager
						.getLaunchIntentForPackage(app.packageName);
				startActivity(intent);
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

	OnLongClickListener appLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			ActivityInfo app = (ActivityInfo) v.getTag();
			Log.d("FloatingFolder",
					"Long clicked: " + app.loadLabel(mPackageManager));
			return true;
		}
	};

	// @Override
	// protected boolean onTouchBody(final int id, final View window,
	// final WindowTouchInfo touchInfo, final View view,
	// android.view.MotionEvent event) {
	// switch (event.getAction()) {
	// case MotionEvent.ACTION_OUTSIDE:
	// close(APP_SELECTOR_ID);
	// break;
	// case MotionEvent.ACTION_MOVE:
	// if (id != APP_SELECTOR_ID) {
	// final LayoutParams params = (LayoutParams) window
	// .getLayoutParams();
	//
	// Display display = mWindowManager.getDefaultDisplay();
	// int displayWidth = display.getWidth();
	// int displayHeight = display.getHeight();
	//
	// final View folder = window.findViewById(R.id.folder);
	// final ImageView screenshot = (ImageView) window
	// .findViewById(R.id.preview);
	//
	// // if touch edge
	// if (params.x == 0
	// || params.x + params.width == displayWidth
	// || params.y == 0
	// || params.y + params.height == displayHeight) {
	// // first time touch edge
	// if (screenshot.getDrawable() == null) {
	// folder.setVisibility(View.GONE);
	//
	// // post so that the folder is invisible before
	// // anything else happens
	// screenshot.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// Drawable drawable = getResources()
	// .getDrawable(
	// R.drawable.ic_menu_archive);
	//
	// screenshot.setImageDrawable(drawable);
	//
	// // preview should be centered vertically
	// params.y = params.y + params.height / 2
	// - drawable.getIntrinsicHeight() / 2;
	//
	// params.width = drawable.getIntrinsicWidth();
	// params.height = drawable
	// .getIntrinsicHeight();
	//
	// updateViewLayout(id, window, params);
	//
	// screenshot.setVisibility(View.VISIBLE);
	// }
	// });
	// }
	// } else { // not touch edge
	// final Drawable drawable = screenshot.getDrawable();
	//
	// // first time not touch edge
	// if (drawable != null) {
	// screenshot.setVisibility(View.GONE);
	//
	// // post so that screenshot is invisible before
	// // anything else happens
	// screenshot.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// screenshot.setImageDrawable(null);
	//
	// LayoutParams originalParams = getParams(id,
	// view);
	//
	// params.y = params.y - originalParams.height
	// / 2 + drawable.getIntrinsicHeight();
	//
	// params.width = originalParams.width;
	// params.height = originalParams.height;
	//
	// updateViewLayout(id, window, params);
	//
	// folder.setVisibility(View.VISIBLE);
	// }
	// });
	// }
	// }
	// }
	//
	// break;
	// }
	//
	// return false;
	// }

	protected String getPersistentNotificationMessage(int id) {
		return "Click to close all windows.";
	}

	protected Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getCloseAllIntent(this, FloatingFolder.class);
	}
}
