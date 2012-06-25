package wei.mark.example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WidgetsWindow extends MultiWindow {
	public static final int DATA_CHANGED_TEXT = 0;

	@Override
	protected void createAndAttachView(final int id, FrameLayout frame) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.widgets, frame, true);

		final TextView status = (TextView) view.findViewById(R.id.status);
		final EditText edit = (EditText) view.findViewById(R.id.edit);
		final EditText edit2 = (EditText) view.findViewById(R.id.edit2);
		Button button = (Button) view.findViewById(R.id.button);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String text = edit.getText().toString();
				String text2 = edit2.getText().toString();

				String changedText = "Entered: "
						+ text
						+ (text.length() == 0 || text2.length() == 0 ? ""
								: " and ") + text2;

				status.setText(changedText);
				edit.setText("");
				edit2.setText("");

				// update MultiWindow:0 when button is pressed
				// to show off the data sending framework
				Bundle data = new Bundle();
				data.putString("changedText", changedText);
				sendData(id, MultiWindow.class, DEFAULT_ID, DATA_CHANGED_TEXT,
						data);
			}
		});
	}

	@Override
	protected LayoutParams getParams(int id, Window window) {
		return new LayoutParams(id, 300, 500);
	}

	@Override
	protected String getAppName() {
		return "WidgetWindow";
	}

	@Override
	protected int getTheme(int id) {
		return android.R.style.Theme_Light;
	}
}