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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapter {
	private Context context;
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase db;

	public DatabaseAdapter(Context context) {
		this.context = context;
		this.databaseHelper = new DatabaseHelper(context);
	}

	public void open() throws SQLException {
		db = databaseHelper.getWritableDatabase();
	}

	public void close() {
		databaseHelper.close();
	}

	public Cursor getCwg(long id) {
		Cursor cursor =
			db.query("cwg, title", new String[]{
					"cwg._id",
					"title.title AS title",
					"cwg.version AS version",
					"cwg.count AS count"},
				"cwg.title_id = title._id AND cwg._id = " + id,
				null,
				null,
				null,
				"title.title, cwg.version");
		cursor.moveToFirst();
		return cursor;
	}

	public Cursor getAllCwg() {
		return db.query("cwg, title", new String[]{
					"cwg._id",
					"title.title",
					"cwg.version",
					"cwg.count"},
				"cwg.title_id = title._id",
				null,
				null,
				null,
				"title.title, cwg.version");
	}

	public Cursor getDuplicityCwg() {
		return db.query("cwg, title", new String[]{
					"cwg._id",
					"title.title",
					"cwg.version",
					"cwg.count"},
				"cwg.title_id = title._id AND count > 1",
				null,
				null,
				null,
				"title.title, cwg.version");
	}

	public Cursor getFilteredCwg(String title) {
		return db.query("cwg, title", new String[]{
					"cwg._id",
					"title.title",
					"cwg.version",
					"cwg.count"},
				"cwg.title_id = title._id AND title LIKE '%' || ? || '%'",
				new String[]{
					title
				},
				null,
				null,
				"title.title, cwg.version");
	}

	public Cursor getDuplicityFilteredCwg(String title) {
		return db.query("cwg, title", new String[]{
					"cwg._id",
					"title.title",
					"cwg.version",
					"cwg.count"},
				"cwg.title_id = title._id AND count > 1 AND title LIKE '%' || ? || '%'",
				new String[]{
					title
				},
				null,
				null,
				"title.title");
	}

	public void addCwg(String title, int version) {
		db.beginTransaction();

		// Title
		Cursor mCursor =
				db.query(true, "title", new String[]{
					"_id",},
				"title = ?",
				new String[]{
					title
				},
				null,
				null,
				null,
				null);
		if (mCursor == null) {
			return;
		}
		long titleId;
		if (mCursor.getCount() > 0) {
			mCursor.moveToFirst();
			titleId = mCursor.getLong(0);
		} else {
			ContentValues val = new ContentValues();
			val.put("title", title);
			titleId = db.insert("title", null, val);
		}

		// CWG
		mCursor =
				db.query(true, "cwg", new String[]{
					"_id",},
				"title_id = " + titleId + " AND version = " + version,
				null,
				null,
				null,
				null,
				null);
		if (mCursor == null) {
			return;
		}
		if (mCursor.getCount() > 0) {
			// Update
			mCursor.moveToFirst();
			long cwgId = mCursor.getLong(0);
			db.execSQL("UPDATE cwg SET count = count + 1 WHERE _id = " + Long.toString(cwgId));
		} else {
			// Insert
			if (version == 0) {
				mCursor =
					db.query(false, "cwg", new String[]{
						"MAX(version)",},
					"title_id = " + titleId,
					null,
					null,
					null,
					null,
					null);
				if (mCursor == null) {
					return;
				}
				if (mCursor.getCount() > 0) {
					mCursor.moveToFirst();
					version = mCursor.getInt(0) + 1;
				} else {
					version = 1;
				}
			}
			ContentValues val = new ContentValues();
			val.put("title_id", titleId);
			val.put("version", version);
			val.put("count", 1);
			titleId = db.insert("cwg", null, val);
		}

		db.setTransactionSuccessful();
		db.endTransaction();

		return;
	}

	public void removeCwg(long id) {
		Cursor mCursor =
				db.query(true, "cwg", new String[]{
					"count",},
				"_id = " + id,
				null,
				null,
				null,
				null,
				null);
		if (mCursor == null) {
			return;
		}
		if (mCursor.getCount() == 0) {
			return;
		}
		mCursor.moveToFirst();
		if (mCursor.getInt(0) > 1) {
			// Update
			db.execSQL("UPDATE cwg SET count = count - 1 WHERE _id = " + id);
		} else {
			// Delete
			db.delete("cwg", "_id = " + id, null);
		}
	}

	public void cleanDb() {
		db.delete("cwg", null, null);
		db.delete("title", null, null);
	}
}
