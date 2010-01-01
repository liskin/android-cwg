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

public abstract class Export {
	private OutputStream output;

	public Export() {
	}

	public void setOutput(OutputStream output) {
		this.output = output;
	}

	protected OutputStream getOutput() {
		return this.output;
	}

	public abstract void exportData(Cursor cursor) throws IOException;
}
