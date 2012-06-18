package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.StandOutWindow.StandOutFlags;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MostBasicWindow extends StandOutWindow {

	@Override
	protected String getAppName() {
		return "MostBasicWindow";
	}

	@Override
	protected int getAppIcon() {
		return android.R.drawable.btn_star;
	}

	@Override
	protected View createAndAttachView(int id, ViewGroup root) {
		TextView view = new TextView(this);
		view.setText("MostBasicWindow");
		view.setBackgroundColor(Color.CYAN);

		root.addView(view);

		return view;
	}

	@Override
	protected LayoutParams getParams(int id, View view) {
		return new LayoutParams(id, 200, 150, 100, 100, Gravity.LEFT
				| Gravity.TOP);
	}
	
	@Override
	protected int getFlags(int id) {
		// TODO Auto-generated method stub
		return super.getFlags(id)|StandOutFlags.FLAG_FIX_COMPATIBILITY_ALL_DISABLE;
	}
}
