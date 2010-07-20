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
import android.util.Log;

public class BackupManager {
	private static final String TAG = "CwgBackupAgentWrapper";
	private static boolean backupAvailable = true;
	private static boolean backupEnabled = true;

	protected static void enableBackup() {
		BackupManager.backupEnabled = true;
		Log.d(TAG, "Enabling backup");
	}

	protected static void disableBackup() {
		BackupManager.backupEnabled = false;
		Log.d(TAG, "Disabling backup");
	}

	protected static void dataChanged(Context context) {
		if (!BackupManager.backupEnabled) {
			return;
		}
		if (BackupManager.backupAvailable) {
			try {
				BackupManagerWrapper.dataChanged(context.getPackageName());
			} catch (Throwable t) {
				Log.d(TAG, "BackupManager is not available");
				BackupManager.backupAvailable = false;
			}
		}
	}
}
