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

public class ImportException extends Exception {
	private String localizedMessage = null;

	public ImportException(String message) {
		super(message);
	}

	public ImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImportException(String message, String localizedMessage, Throwable cause) {
		super(message, cause);

		this.localizedMessage = localizedMessage;
	}

	@Override
	public String getLocalizedMessage() {
		if (this.localizedMessage == null) {
			return getMessage();
		} else {
			return this.localizedMessage;
		}
	}
}
