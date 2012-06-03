package wei.mark.standout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class EditTextActivity extends Activity {
	public static Class<? extends StandOutWindow> cls;
	public static EditText edit;

	EditText editText;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edittext);

		View window = findViewById(R.id.window);
		window.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		String text = getIntent().getStringExtra("text");
		int caret = getIntent().getIntExtra("caret", text.length());

		editText = (EditText) findViewById(R.id.edittext);
		editText.setText(text);
		editText.setSelection(caret);

		editText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				edit.setText(editText.getText().toString());
				edit.setSelection(start + count);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		String text = intent.getStringExtra("text");
		int caret = intent.getIntExtra("caret", text.length());

		editText.setSelection(caret);
	}
}
