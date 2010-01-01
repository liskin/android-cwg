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
import android.util.Log;

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

	public void beginTransaction() {
		db.beginTransaction();
	}

	public void endTransaction() {
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public Cursor getCwg(long id) {
		Cursor cursor =
			db.query("cwg", new String[]{
					"_id",
					"title",
					"catalog_title",
					"catalog_id",
					"jpg",
					"count"},
				"_id = " + id,
				null,
				null,
				null,
				null);
		cursor.moveToFirst();
		if (cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			return cursor;
		} else {
			return null;
		}
	}

	public Cursor getCwgByCatalogId(String catalogId) {
		Cursor cursor =
			db.query("cwg", new String[]{
					"_id",
					"title",
					"catalog_title",
					"catalog_id",
					"jpg",
					"count"},
				"catalog_id = ?",
				new String[] {
					catalogId
				},
				null,
				null,
				null);
		if (cursor == null) {
			return null;
		}
		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			return cursor;
		} else {
			cursor.close();
			return null;
		}
	}

	public Cursor getAllCwg() {
		return db.query("cwg", new String[]{
					"_id",
					"title",
					"catalog_title",
					"catalog_id",
					"jpg",
					"count"},
				"count > 0",
				null,
				null,
				null,
				"title, catalog_id");
	}

	public Cursor getDuplicityCwg() {
		return db.query("cwg", new String[]{
					"_id",
					"title",
					"catalog_title",
					"catalog_id",
					"jpg",
					"count"},
				"count > 1",
				null,
				null,
				null,
				"title, catalog_id");
	}

	public Cursor getFilteredCwg(String title) {
		if (title == null || title.length() == 0) {
			return this.getAllCwg();
		}
		return db.query("cwg", new String[]{
					"_id",
					"title",
					"catalog_title",
					"catalog_id",
					"jpg",
					"count"},
				"title LIKE '%' || ? || '%'",
				new String[]{
					title
				},
				null,
				null,
				"count > 0 DESC, title, catalog_id");
	}

	public Cursor getDuplicityFilteredCwg(String title) {
		return db.query("cwg, title", new String[]{
					"_id",
					"title",
					"catalog_title",
					"catalog_id",
					"jpg",
					"count"},
				"count > 1 AND title LIKE '%' || ? || '%'",
				new String[]{
					title
				},
				null,
				null,
				"title, catalog_id");
	}

	public void addCwg(String title, String catalogTitle, String catalogId, String jpg, int count) {
		ContentValues val = new ContentValues();
		val.put("title", title);
		val.put("catalog_title", catalogTitle);
		val.put("catalog_id", catalogId);
		val.put("jpg", jpg);
		val.put("count", count);
		db.insert("cwg", null, val);

		return;
	}

	public void deleteCwg(long id) {
		db.delete("cwg", "_id = " + id, null);

		return;
	}

	public void updateCwg(long id, String title, String catalogTitle,
			String catalogId, String jpg, int count) {
		ContentValues val = new ContentValues();
		val.put("title", title);
		val.put("catalog_title", catalogTitle);
		val.put("catalog_id", catalogId);
		val.put("jpg", jpg);
		val.put("count", count);
		db.update("cwg", val, "_id = " + id, null);
	}

	public void incCwg(long id) {
		db.execSQL("UPDATE cwg SET count = count + 1 WHERE _id = " + id);
	}

	public void decCwg(long id) {
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
			mCursor.close();
			return;
		}
		mCursor.moveToFirst();
		if (mCursor.getInt(0) > 0) {
			// Update
			db.execSQL("UPDATE cwg SET count = count - 1 WHERE _id = " + id);
		} else {
			Log.e(null, "Try to remove zero CWG");
		}
		mCursor.close();
	}

	public void eraseDb() {
		db.delete("cwg", null, null);
	}

	public int countCwgSumCount() {
		Cursor mCursor =
				db.query(false, "cwg", new String[]{
					"SUM(count)",},
				null,
				null,
				null,
				null,
				null,
				null);
		if (mCursor == null) {
			return 0;
		}
		if (mCursor.getCount() == 0) {
			mCursor.close();
			return 0;
		}
		mCursor.moveToFirst();
		int count = mCursor.getInt(0);
		mCursor.close();
		return count;
	}

	public int countCwg() {
		Cursor mCursor =
				db.query(false, "cwg", new String[]{
					"COUNT(*)"},
				"count > 0",
				null,
				null,
				null,
				null,
				null);
		if (mCursor == null) {
			return 0;
		}
		if (mCursor.getCount() == 0) {
			mCursor.close();
			return 0;
		}
		mCursor.moveToFirst();
		int count = mCursor.getInt(0);
		mCursor.close();
		return count;
	}

	public int countCwgDuplicity() {
		Cursor mCursor =
				db.query(false, "cwg", new String[]{
					"COUNT(*)"},
				"count > 1",
				null,
				null,
				null,
				null,
				null);
		if (mCursor == null) {
			return 0;
		}
		if (mCursor.getCount() == 0) {
			mCursor.close();
			return 0;
		}
		mCursor.moveToFirst();
		int count = mCursor.getInt(0);
		mCursor.close();
		return count;
	}
}
