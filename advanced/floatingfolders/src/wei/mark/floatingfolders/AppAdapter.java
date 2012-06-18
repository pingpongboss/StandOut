package wei.mark.floatingfolders;

import java.util.List;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends ArrayAdapter<ActivityInfo> {
	class ViewHolder {
		ImageView icon;
		TextView name;
		int position;
	}

	LayoutInflater mInflater;
	PackageManager mPackageManager;
	int mTextViewResourceId;

	public AppAdapter(Context context, int textViewResourceId,
			List<ActivityInfo> objects) {
		super(context, textViewResourceId, objects);

		mInflater = LayoutInflater.from(context);
		mPackageManager = context.getPackageManager();
		mTextViewResourceId = textViewResourceId;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		final ActivityInfo app = getItem(position);

		if (convertView == null) {
			convertView = mInflater.inflate(mTextViewResourceId, parent, false);
			holder = new ViewHolder();

			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.name = (TextView) convertView.findViewById(R.id.name);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final View view = convertView;

		holder.position = position;

		// don't block the UI thread
		new Thread(new Runnable() {
			public void run() {
				final CharSequence label = app.loadLabel(mPackageManager);
				final Drawable drawable = app.loadIcon(mPackageManager);
				view.post(new Runnable() {

					@Override
					public void run() {
						if (holder.position == position) {
							holder.name.setText(label);
							holder.icon.setImageDrawable(drawable);
						}
					}
				});
			}
		}).start();

		return convertView;
	}
}
