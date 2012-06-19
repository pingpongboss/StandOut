package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
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
	protected void createAndAttachView(int id, FrameLayout frame) {
		TextView view = new TextView(this);
		view.setText("MostBasicWindow");
		view.setBackgroundColor(Color.CYAN);

		frame.addView(view);
	}

	@Override
	protected LayoutParams getParams(int id, Window window) {
		return new LayoutParams(id, 200, 150, 100, 100, Gravity.LEFT
				| Gravity.TOP);
	}
}
