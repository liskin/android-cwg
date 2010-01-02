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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "CwgDatabaseHelper";
	private static final String DATABASE_NAME = "cwg";
	private static final int DATABASE_VERSION = 2;

	DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE cwg (_id INTEGER PRIMARY KEY,"
				+ "title VARCHAR NOT NULL COLLATE LOCALIZED,"
				+ "catalog_title VARCHAR COLLATE LOCALIZED,"
				+ "catalog_id VARCHAR,"
				+ "jpg VARCHAR,"
				+ "count INT NOT NULL)");
		db.execSQL("CREATE UNIQUE INDEX cwg_catalog_id_uix ON cwg (catalog_id)");
		db.execSQL("CREATE INDEX cwg_count_ix on cwg (count)");
		db.execSQL("CREATE INDEX cwg_title_ix on cwg (title)");
		db.execSQL("CREATE INDEX cwg_catalog_title_ix on cwg (catalog_title)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "Upgrading from " + oldVersion + " to " + newVersion);
		if (oldVersion == 1) {
			db.execSQL("ALTER TABLE cwg RENAME TO _cwg");
			db.execSQL("CREATE TABLE cwg (_id INTEGER PRIMARY KEY,"
				+ "title VARCHAR NOT NULL COLLATE LOCALIZED,"
				+ "catalog_title VARCHAR COLLATE LOCALIZED,"
				+ "catalog_id VARCHAR,"
				+ "jpg VARCHAR,"
				+ "count INT NOT NULL)");
			db.execSQL("CREATE UNIQUE INDEX cwg_catalog_id_uix ON cwg (catalog_id)");
			db.execSQL("CREATE INDEX cwg_count_ix on cwg (count)");
			db.execSQL("CREATE INDEX cwg_title_ix on cwg (title)");
			db.execSQL("CREATE INDEX cwg_catalog_title_ix on cwg (catalog_title)");
			db.execSQL("INSERT INTO cwg (title, count) " +
					"SELECT title, count FROM _cwg JOIN title ON (_cwg.title_id = title._id)");
			db.execSQL("DROP TABLE _cwg");
			db.execSQL("DROP TABLE title");
			oldVersion = 2;
		}
	}
}
