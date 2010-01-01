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

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CsvImport extends Import {
	private static final String TAG = "CwgCsvImport";

	public CsvImport() {
	}

	public void importData(DatabaseAdapter db) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(getInput()));
		String line;
		while ((line = reader.readLine()) != null) {
			String title = "";
			String catalogTitle = "";
			String catalogId = "";
			String jpg = "";
			String count = "";
			int col = 0;
			int length = line.length();
			boolean quote = false;
			boolean escape = false;
			for (int pos = 0 ; pos < length ; pos++) {
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
					continue;
				}
				switch (col) {
					case 0:
						title += line.charAt(pos);
						break;
					case 1:
						catalogTitle += line.charAt(pos);
						break;
					case 2:
						catalogId += line.charAt(pos);
						break;
					case 3:
						jpg += line.charAt(pos);
						break;
					case 4:
						count += line.charAt(pos);
						break;
				}
				escape = false;
			}

			try {
				int countI = Integer.parseInt(count);
				title = title.trim();
				if (title.length() > 0) {
					if (catalogTitle.length() == 0) {
						catalogTitle = null;
					}
					if (catalogId.length() == 0) {
						catalogId = null;
					}
					if (jpg.length() == 0) {
						jpg = null;
					}
					db.addCwg(title, catalogTitle, catalogId, jpg, countI);
				}
			} catch (NumberFormatException nfe) {
				Log.d(TAG, "Count is not number, skipping:" +
						" count=" + count);
			}
		}
	}
}
