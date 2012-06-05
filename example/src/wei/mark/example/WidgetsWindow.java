package wei.mark.example;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WidgetsWindow extends MultiWindow {
	@Override
	protected View createAndAttachView(int id, ViewGroup root) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.widgets, root, true);

		final TextView status = (TextView) view.findViewById(R.id.status);
		final EditText edit = (EditText) view.findViewById(R.id.edit);
		Button button = (Button) view.findViewById(R.id.button);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String text = edit.getText().toString();
				status.setText("Entered: " + text);
				edit.setText("");
				edit.setHint("Write something else");
			}
		});

		return view;
	}

	@Override
	protected LayoutParams getParams(int id, View view) {
		return new LayoutParams(300, 500);
	}
}
