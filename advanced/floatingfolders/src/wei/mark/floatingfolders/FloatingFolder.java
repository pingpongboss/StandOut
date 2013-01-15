package wei.mark.floatingfolders;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;
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
import android.util.SparseArray;
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
import android.widget.Toast;

public final class FloatingFolder extends StandOutWindow {
	private static final int APP_SELECTOR_ID = -2;

	private static final int APP_SELECTOR_CODE = 2;
	private static final int APP_SELECTOR_FINISHED_CODE = 3;
	public static final int STARTUP_CODE = 4;

	PackageManager mPackageManager;
	WindowManager mWindowManager;

	int iconSize;
	int squareWidth;

	SparseArray<FolderModel> mFolders;

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
	public String getAppName() {
		return "Floating Folders";
	}

	@Override
	public int getAppIcon() {
		return R.drawable.ic_launcher;
	}

	@Override
	public void createAndAttachView(final int id, FrameLayout frame) {
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
	public StandOutLayoutParams getParams(int id, Window window) {
		if (APP_SELECTOR_ID == id) {
			return new StandOutLayoutParams(id, 400,
					StandOutLayoutParams.FILL_PARENT,
					StandOutLayoutParams.CENTER, StandOutLayoutParams.TOP);
		} else {
			FolderModel folder = mFolders.get(id);
			int width = folder.width;
			int height = folder.height;

			if (width == 0) {
				width = 400;
			}
			if (height == 0) {
				height = 400;
			}
			return new StandOutLayoutParams(id, width, height, 50, 50);
		}
	}

	@Override
	public int getFlags(int id) {
		if (APP_SELECTOR_ID == id) {
			return super.getFlags(id);
		} else {
			return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
					| StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
					| StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
		}
	}

	@Override
	public void onReceiveData(int id, int requestCode, Bundle data,
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

				Window window = getWindow(id);
				if (window == null) {
					return;
				}

				ViewGroup flow = (ViewGroup) window.findViewById(R.id.flow);

				addAppToFolder(id, app, flow);

				onUserAddApp(id, app);
				break;
			case STARTUP_CODE:
				loadAllFolders();
				if (mFolders.size() == 0) {
					mFolders.put(DEFAULT_ID, new FolderModel());
					show(DEFAULT_ID);
				} else {
					for (int i = 0; i < mFolders.size(); i++) {
						FolderModel folder = mFolders.get(mFolders.keyAt(i));
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

	private void removeAppFromView(int id, ActivityInfo app) {
		Window window = getWindow(id);
		View frame = window.findViewWithTag(app);
		ViewGroup flow = (ViewGroup) window.findViewById(R.id.flow);

		flow.removeView(frame);
	}

	private void onUserAddApp(int id, ActivityInfo app) {
		FolderModel folder = mFolders.get(id);
		folder.apps.add(app);

		resizeToGridAndSave(id, -1);
	}

	private void onUserRemoveApp(int id, ActivityInfo app) {
		removeAppFromView(id, app);

		FolderModel folder = mFolders.get(id);
		folder.apps.remove(app);

		resizeToGridAndSave(id, -1);
	}

	private void saveFolder(FolderModel folder) {
		FileOutputStream out = null;
		try {
			out = openFileOutput(String.format("folder%d", folder.id),
					MODE_PRIVATE);

			out.write(String.format("%d\n", folder.width).getBytes());
			out.write(String.format("%d\n", folder.height).getBytes());

			for (ActivityInfo appInFolder : folder.apps) {
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
		mFolders = new SparseArray<FolderModel>();
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

					int i = 0;
					for (String appName : appNames.split("\n")) {
						if (i < 2) {
							// width and height
							try {
								if (i == 0) {
									folder.width = Integer.parseInt(appName);
								} else if (i == 1) {
									folder.height = Integer.parseInt(appName);
								}
							} catch (NumberFormatException ex) {
								String msg = "Please uninstall Floating Folders and reinstall it. The folder format has changed.";
								Log.d("FloatingFolder", msg);
								Toast.makeText(this, msg, Toast.LENGTH_SHORT)
										.show();
								break;
							}
							i++;
						} else {
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
	public void onResize(int id, Window window, View view, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			resizeToGridAndSave(id, -1);
		}
	}

	private void resizeToGridAndSave(final int id, final int cols) {
		final Window window = getWindow(id);

		window.post(new Runnable() {

			@Override
			public void run() {
				FlowLayout flow = (FlowLayout) window.findViewById(R.id.flow);

				FolderModel folder = mFolders.get(id);

				int count = folder.apps.size();
				int columns = cols;

				if (cols == -1) {
					columns = flow.getCols();
				}

				if (columns < 2) {
					columns = 2;
				}

				int rows = count / columns;
				if (count % columns > 0) {
					rows++;
				}

				if (rows < 1) {
					rows = 1;
				}

				int width = flow.getLeft()
						+ (((ViewGroup) flow.getParent()).getWidth() - flow
								.getRight()) + columns * squareWidth;
				int height = width;

				if (count > 0) {
					height = flow.getTop()
							+ (((ViewGroup) flow.getParent()).getHeight() - flow
									.getBottom()) + rows
							* flow.getChildHeight();
				}

				StandOutLayoutParams params = window.getLayoutParams();
				params.width = width;
				params.height = height;
				updateViewLayout(id, params);

				folder.width = width;
				folder.height = height;

				saveFolder(folder);
			}
		});
	}

	@Override
	public boolean onFocusChange(int id, Window window, boolean focus) {
		if (id == APP_SELECTOR_ID && !focus) {
			close(APP_SELECTOR_ID);
			return false;
		}

		return super.onFocusChange(id, window, focus);
	}

	@Override
	public boolean onTouchBody(final int id, final Window window,
			final View view, MotionEvent event) {
		if (id != APP_SELECTOR_ID
				&& event.getAction() == MotionEvent.ACTION_MOVE) {
			final StandOutLayoutParams params = (StandOutLayoutParams) window
					.getLayoutParams();

			final View folderView = window.findViewById(R.id.folder);
			final ImageView screenshot = (ImageView) window
					.findViewById(R.id.preview);

			FolderModel folder = mFolders.get(id);

			// if touch edge
			if (params.x <= 0) {
				// first time touch edge
				if (folder.fullSize) {
					folder.fullSize = false;

					final Drawable drawable = getResources().getDrawable(
							R.drawable.ic_menu_archive);

					screenshot.setImageDrawable(drawable);

					mFadeOut.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationStart(Animation animation) {
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

						@Override
						public void onAnimationEnd(Animation animation) {
							folderView.setVisibility(View.GONE);

							// post so that the folder is invisible
							// before
							// anything else happens
							screenshot.post(new Runnable() {

								@Override
								public void run() {
									// preview should be centered
									// vertically
									params.y = params.y + params.height / 2
											- drawable.getIntrinsicHeight() / 2;

									params.width = drawable.getIntrinsicWidth();
									params.height = drawable
											.getIntrinsicHeight();

									updateViewLayout(id, params);

									screenshot.setVisibility(View.VISIBLE);
									screenshot.startAnimation(mFadeIn);
								}
							});
						}
					});

					folderView.startAnimation(mFadeOut);
				}
			} else { // not touch edge

				// first time not touch edge
				if (!folder.fullSize) {
					folder.fullSize = true;

					mFadeOut.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationStart(Animation animation) {
							Log.d("FloatingFolder", "Animation started");
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
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
									StandOutLayoutParams originalParams = getParams(
											id, window);

									Drawable drawable = screenshot
											.getDrawable();
									screenshot.setImageDrawable(null);

									params.y = params.y - originalParams.height
											/ 2 + drawable.getIntrinsicHeight()
											/ 2;

									params.width = originalParams.width;
									params.height = originalParams.height;

									updateViewLayout(id, params);

									folderView.setVisibility(View.VISIBLE);

									folderView.startAnimation(mFadeIn);
								}
							});
						}
					});

					screenshot.startAnimation(mFadeOut);
				}
			}
		}

		return false;
	}

	public String getPersistentNotificationMessage(int id) {
		return "Click to close all windows.";
	}

	public Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getCloseAllIntent(this, FloatingFolder.class);
	}

	@Override
	public List<DropDownListItem> getDropDownItems(final int id) {
		List<DropDownListItem> items = new ArrayList<DropDownListItem>();
		FolderModel folder = mFolders.get(id);

		// add
		items.add(new DropDownListItem(android.R.drawable.ic_menu_add,
				"Add Application", new Runnable() {

					@Override
					public void run() {
						// show app selector
						sendData(id, FloatingFolder.class, APP_SELECTOR_ID,
								APP_SELECTOR_CODE, null);
					}
				}));

		if (!folder.apps.isEmpty()) {
			// clear all
			items.add(new DropDownListItem(android.R.drawable.ic_menu_delete,
					"Clear All", new Runnable() {

						@Override
						public void run() {
							// show app selector
							FolderModel folder = mFolders.get(id);

							// copy to new array so we don't remove items while
							// we
							// iterate
							List<ActivityInfo> apps = new ArrayList<ActivityInfo>(
									folder.apps);

							for (ActivityInfo app : apps) {
								onUserRemoveApp(id, app);
							}
						}
					}));
		}

		return items;
	}
}
