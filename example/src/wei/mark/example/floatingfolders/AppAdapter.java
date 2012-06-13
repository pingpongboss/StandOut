package wei.mark.example.floatingfolders;

import java.util.List;

import wei.mark.example.R;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		ActivityInfo app = getItem(position);

		if (convertView == null) {
			convertView = mInflater.inflate(mTextViewResourceId, parent, false);
			holder = new ViewHolder();

			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.name = (TextView) convertView.findViewById(R.id.name);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.icon.setImageDrawable(app.loadIcon(mPackageManager));
		holder.name.setText(app.loadLabel(mPackageManager));

		return convertView;
	}
}
