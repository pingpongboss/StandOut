package wei.mark.floatingfolders;

import wei.mark.standout.StandOutWindow;
import android.app.Activity;
import android.os.Bundle;

public class FloatingFoldersLauncher extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		StandOutWindow.closeAll(this, FloatingFolder.class);
		FloatingFolder.showFolders(this);
		
		finish();
	}
}
