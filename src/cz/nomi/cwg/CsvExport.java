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

import android.database.Cursor;
import java.io.IOException;

class CsvExport extends Export {
	CsvExport() {
	}

	void exportData(Cursor cursor) throws IOException {
		getOutput().write("\"Title\";\"Catalog title\";\"Catalog ID\";\"JPG\";\"Count\"\n".getBytes("UTF-8"));
		while (cursor.moveToNext()) {
			StringBuilder buffer = new StringBuilder();
			String catalogTitle = cursor.getString(cursor.getColumnIndex("catalog_title"));
			String catalogId = cursor.getString(cursor.getColumnIndex("catalog_id"));
			String jpg = cursor.getString(cursor.getColumnIndex("jpg"));
			buffer.append("\"");
			buffer.append(cursor.getString(cursor.getColumnIndex("title")).replace("\"", "\\\""));
			buffer.append("\";\"");
			buffer.append(catalogTitle == null ? "" : catalogTitle);
			buffer.append("\";\"");
			buffer.append(catalogId == null ? "" : catalogId);
			buffer.append("\";\"");
			buffer.append(jpg == null ? "" : jpg);
			buffer.append("\";\"");
			buffer.append(cursor.getInt(cursor.getColumnIndex("count")));
			buffer.append("\"");
			buffer.append("\n");
			getOutput().write(
				buffer.toString().getBytes("UTF-8")
			);
		}
	}
}
