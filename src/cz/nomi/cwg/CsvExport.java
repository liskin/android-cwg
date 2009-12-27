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
import java.io.OutputStream;

public class CsvExport extends Export {
	public CsvExport(OutputStream output) {
		super(output);
	}

	public void exportData(Cursor cursor) throws IOException {
		output.write("\"Title\";\"Version\";\"Count\"\n".getBytes("UTF-8"));
		while (cursor.moveToNext()) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("\"");
			buffer.append(cursor.getString(cursor.getColumnIndex("title")).replace("\"", "\\\""));
			buffer.append("\";\"");
			buffer.append(cursor.getInt(cursor.getColumnIndex("version")));
			buffer.append("\";\"");
			buffer.append(cursor.getInt(cursor.getColumnIndex("count")));
			buffer.append("\"");
			buffer.append("\n");
			output.write(
				buffer.toString().getBytes("UTF-8")
			);
		}
	}
}
