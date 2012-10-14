package wei.mark.example;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.ui.Window;
import android.graphics.Color;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MostBasicWindow extends StandOutWindow {

	@Override
	public String getAppName() {
		return "MostBasicWindow";
	}

	@Override
	public int getAppIcon() {
		return android.R.drawable.btn_star;
	}

	@Override
	public void createAndAttachView(int id, FrameLayout frame) {
		TextView view = new TextView(this);
		view.setText("MostBasicWindow");
		view.setBackgroundColor(Color.CYAN);

		frame.addView(view);
	}

	@Override
	public StandOutLayoutParams getParams(int id, Window window) {
		return new StandOutLayoutParams(id, 200, 150, 100, 100);
	}
}
