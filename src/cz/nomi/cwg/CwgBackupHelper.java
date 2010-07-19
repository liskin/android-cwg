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

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CwgBackupHelper implements BackupHelper {
	private static final String TAG = "CwgBackupHelper";
	private static final String KEY_CWG = "cwg";
	private static final String DELIMITER = "\0";
	private static final byte[] EMPTY_DATA = new byte[0];
	private static final byte VERSION = 1;
	private Context context;

	public CwgBackupHelper(Context context) {
		this.context = context;
	}

	private byte[] buildData() {
		DatabaseAdapter db = new DatabaseAdapter(this.context);
		db.open();

		Cursor cursor = db.getAllCwg();

		if (cursor == null) {
			return EMPTY_DATA;
		}

		if (!cursor.moveToFirst()) {
			cursor.close();
			return EMPTY_DATA;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream(cursor.getCount() * 10);
		try {
			GZIPOutputStream gzip = new GZIPOutputStream(baos);
			int index;
			
			gzip.write(VERSION);
			while (!cursor.isAfterLast()) {
				StringBuilder out = new StringBuilder();
				index = cursor.getColumnIndex("title");
				if (!cursor.isNull(index)) {
					out.append(cursor.getString(index));
				}
				out.append(DELIMITER);

				index = cursor.getColumnIndex("catalog_title");
				if (!cursor.isNull(index)) {
					out.append(cursor.getString(index));
				}
				out.append(DELIMITER);

				index = cursor.getColumnIndex("catalog_id");
				if (!cursor.isNull(index)) {
					out.append(cursor.getString(index));
				}
				out.append(DELIMITER);

				index = cursor.getColumnIndex("jpg");
				if (!cursor.isNull(index)) {
					out.append(cursor.getString(index));
				}
				out.append(DELIMITER);

				index = cursor.getColumnIndex("count");
				if (!cursor.isNull(index)) {
					out.append(cursor.getString(index));
				}
				out.append(DELIMITER);

				byte[] line = out.toString().getBytes();
				gzip.write(line);
				cursor.moveToNext();
			}
			gzip.finish();
		} catch (IOException ioe) {
			Log.e(TAG, "Couldn't compress the CWG:\n" + ioe);
			return EMPTY_DATA;
		} finally {
			cursor.close();
			db.close();
		}

		return baos.toByteArray();
	}

	public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
		byte[] cwgData = buildData();

		try {
			// Load old checksum
			DataInputStream dataInput = new DataInputStream(
				new FileInputStream(oldState.getFileDescriptor()));
			long oldSum = dataInput.readLong();
			dataInput.close();

			// Count new checksum
			CRC32 newSum = new CRC32();
			newSum.update(cwgData);
			long newSumL = newSum.getValue();

			if (oldSum != newSumL) {
				Log.d(TAG, "Backup data is different, writing: " + oldSum + " vs. " + newSumL);
				// Different
				data.writeEntityHeader(KEY_CWG, cwgData.length);
				data.writeEntityData(cwgData, cwgData.length);
			} else {
				Log.d(TAG, "Backup data is same");
			}

			// Save new checksum
			DataOutputStream dataOutput = new DataOutputStream(
				new FileOutputStream(newState.getFileDescriptor()));
			dataOutput.writeLong(newSumL);
			dataOutput.close();
		} catch (IOException ioe) {
			Log.e(TAG, ioe.toString());
		}
	}

	public void restoreEntity(BackupDataInputStream data) {
		if (data.getKey().equals(KEY_CWG)) {
			DatabaseAdapter db = new DatabaseAdapter(this.context);
			db.open();

			byte[] dataUn = null;
			try {
				GZIPInputStream gzip = new GZIPInputStream(data);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] tempData = new byte[1024];
				int got;
				while ((got = gzip.read(tempData)) > 0) {
					baos.write(tempData, 0, got);
				}
				gzip.close();
				dataUn = baos.toByteArray();
			} catch (IOException ioe) {
				Log.e(TAG, "Couldn't read and uncompress entity data:\n" + ioe);
				return;
			}

			byte version = dataUn[0];

			Log.d(TAG, "Backup version " + version);
			if (version == 1) {
				StringTokenizerWithEmpty st =
						new StringTokenizerWithEmpty(new String(dataUn, 1, dataUn.length - 1), DELIMITER);

				BackupAgentWrapper.disableBackup();
				while (st.hasMoreTokens()) {
					String title = st.nextToken();
					if (title.length() == 0) {
						break;
					}
					String catalogTitle = st.nextToken();
					if (catalogTitle.length() == 0) {
						catalogTitle = null;
					}
					String catalogId = st.nextToken();
					if (catalogId.length() == 0) {
						catalogId = null;
					}
					String jpg = st.nextToken();
					if (jpg.length() == 0) {
						jpg = null;
					}
					int count = Integer.parseInt(st.nextToken());

					db.addCwg(title, catalogTitle, catalogId, jpg, count);
				}
				BackupAgentWrapper.enableBackup();
			} else {
				Log.e(TAG, "Unknown backup version, ignoring");
			}

			db.close();
		}
	}

	public void writeNewStateDescription(ParcelFileDescriptor newState) {
	}
}
