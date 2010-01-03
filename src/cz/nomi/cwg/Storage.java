/*
	This file is part of Android-CWG.

	Android-CWG is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Android-CWG is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Android-CWG.  If not, see <http://www.gnu.org/licenses/>.
*/

package cz.nomi.cwg;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;

class Storage {
	private static final String TAG = "CwgStorage";
	private static final String DIRECTORY = "cwg";
	private static final String JPG_CACHE = "cache";
	private Context context;

	Storage(Context context) {
		this.context = context;
	}

	File getJpgCacheFile(String name) {
		File root = Environment.getExternalStorageDirectory();
		if (root.canWrite()) {
			File file = new File(root, DIRECTORY);
			if (!file.exists()) {
				file.mkdir();
			}
			file = new File(file, JPG_CACHE);
			if (!file.exists()) {
				file.mkdir();
			}

			return new File(file, name);
		} else {
			Log.d(TAG, "Can't write to external storage");
			return null;
		}
	}
}
