package wei.mark.standouttest;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import hyperionics.com.standouttest.R;
import wei.mark.standouttest.floatingfolders.FloatingFolder;
import wei.mark.standout.StandOutWindow;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            /** if not construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE);
            return;
        }

        showOverlay();
    }

    private void showOverlay() {
        StandOutWindow.closeAll(this, SimpleWindow.class);
        StandOutWindow.closeAll(this, MultiWindow.class);
        StandOutWindow.closeAll(this, WidgetsWindow.class);
        StandOutWindow.closeAll(this, FloatingFolder.class);

//        StandOutWindow.show(this, MostBasicWindow.class, StandOutWindow.DEFAULT_ID);
//        StandOutWindow.show(this, SimpleWindow.class, StandOutWindow.DEFAULT_ID);
//        StandOutWindow.show(this, MultiWindow.class, StandOutWindow.DEFAULT_ID);
        StandOutWindow.show(this, WidgetsWindow.class, StandOutWindow.DEFAULT_ID);
        FloatingFolder.showFolders(this);

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if (requestCode == REQUEST_CODE) {
            // if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(this)) {
                showOverlay();
            }
            else {
                finish();
            }
        }
    }
}
