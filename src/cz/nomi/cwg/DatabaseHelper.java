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

class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "cwg";
	private static final int DATABASE_VERSION = 1;

	DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE title (_id INTEGER PRIMARY KEY,"
				+ "title VARCHAR NOT NULL COLLATE LOCALIZED);");
		db.execSQL("CREATE TABLE cwg (_id INTEGER PRIMARY KEY,"
				+ "title_id INT NOT NULL,"
				+ "version INT NOT NULL,"
				+ "count INT NOT NULL);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
