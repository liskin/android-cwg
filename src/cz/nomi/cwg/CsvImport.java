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
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

class CsvImport extends Import {
	private static final String TAG = "CwgCsvImport";

	CsvImport() {
	}

	@Override
	void importData(DatabaseAdapter db) throws ImportException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getInput()));
			String line;
			while ((line = reader.readLine()) != null) {
				int col = 0;
				int length = line.length();
				boolean quote = false;
				boolean escape = false;
				Vector<StringBuilder> columns = new Vector<StringBuilder>();
				columns.add(new StringBuilder());
				for (int pos = 0; pos < length; pos++) {
					if (line.charAt(pos) == '\\') {
						escape = !escape;
						if (escape) {
							continue;
						}
					}
					if (!escape && (line.charAt(pos) == '"' || line.charAt(pos) == '\'')) {
						quote = !quote;
						continue;
					}
					if (!escape && !quote && (line.charAt(pos) == ',' || line.charAt(pos) == ';')) {
						col++;
						columns.add(new StringBuilder());
						continue;
					}
					columns.get(col).append(line.charAt(pos));
					escape = false;
				}

				switch (col) {
					case 2:
						// Version 0.1 + 0.2
						String title = columns.get(0).toString().trim();
						if (title.length() > 0) {
							// Column 1 was version -> ignoring
							String count = columns.get(2).toString().trim();
							try {
								int countI = Integer.parseInt(count);
								db.addCwg(title, null, null, null, countI);
							} catch (NumberFormatException nfe) {
								Log.d(TAG, "Count is not number, skipping:"
										+ " count=" + count);
							}
						}
						break;
					case 4:
						// Version 0.3+
						title = columns.get(0).toString().trim();
						if (title.length() > 0) {
							String catalogTitle = columns.get(1).toString().trim();
							String catalogId = columns.get(2).toString().trim();
							String jpg = columns.get(3).toString().trim();
							String count = columns.get(4).toString().trim();

							if (catalogTitle.length() == 0) {
								catalogTitle = null;
							}
							if (catalogId.length() == 0) {
								catalogId = null;
							}
							if (jpg.length() == 0) {
								jpg = null;
							}
							try {
								int countI = Integer.parseInt(count);

								Cursor cur = db.getCwgByCatalogId(catalogId);
								if (cur == null) {
									db.addCwg(title, catalogTitle, catalogId, jpg, countI);
								} else {
									long id = cur.getLong(cur.getColumnIndex("_id"));
									db.updateCwg(id, title, catalogTitle, catalogId, jpg, countI);
									cur.close();
								}
							} catch (NumberFormatException nfe) {
								Log.d(TAG, "Count is not number, skipping:"
										+ " count=" + count);
							}
						}
						break;
					default:
						throw new ImportException("Unknown CSV format");
				}
			}
		} catch (IOException ioe) {
			throw new ImportException(
					ioe.getClass().getName() + ": " + ioe.getMessage(),
					ioe.getClass().getName() + ": " + ioe.getLocalizedMessage(),
					ioe);
		}
	}
}
