package wei.mark.floatingfolders;

import java.util.ArrayList;
import java.util.List;

import android.content.pm.ActivityInfo;

public class FolderModel {
	public int id;
	public String name;
	public List<ActivityInfo> apps;
	public boolean shown;
	public boolean fullSize;
	public int width;
	public int height;

	public FolderModel() {
		apps = new ArrayList<ActivityInfo>();
		shown = true;
		fullSize = true;
	}
}
