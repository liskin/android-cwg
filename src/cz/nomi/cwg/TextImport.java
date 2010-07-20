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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class TextImport extends Import {
	TextImport() {
	}

	@Override
	void importData(DatabaseAdapter db) throws ImportException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getInput()));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					db.addCwg(line, null, null, null, 1);
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
