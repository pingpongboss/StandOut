package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StandOutMostBasicWindow extends StandOutWindow {

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
		return new LayoutParams(200, 150, 100, 100, Gravity.LEFT);
	}
}
