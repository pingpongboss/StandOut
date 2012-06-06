package wei.mark.standout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;

public class FixEditTextActivity extends Activity {
	public static EditText edit;

	EditText editText;
	TextWatcher textWatcher;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edittext);

		View window = findViewById(R.id.window);
		window.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				finish();
				return false;
			}
		});

		editText = (EditText) findViewById(R.id.edittext);

		String text = getIntent().getStringExtra("text");
		int caret = getIntent().getIntExtra("caret", text.length());
		editText.setText(text);
		editText.setSelection(caret);

		textWatcher = new TextWatcher() {

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
		};

		editText.addTextChangedListener(textWatcher);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		String text = intent.getStringExtra("text");
		int caret = intent.getIntExtra("caret", text.length());

		editText.removeTextChangedListener(textWatcher);
		editText.setText(text);
		editText.setSelection(caret);
		editText.addTextChangedListener(textWatcher);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		edit = null;
	}
}
