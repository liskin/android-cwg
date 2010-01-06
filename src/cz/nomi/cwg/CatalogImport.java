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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class CatalogHandler extends DefaultHandler {
	private DatabaseAdapter db;
	private StringBuilder catalogTitle;
	private StringBuilder catalogId;
	private StringBuilder jpg;
	enum State {
		NONE, CWG, ID_KATALOG, JMENO, JPG_OFICIALNI
	}
	private State state;

	public CatalogHandler(DatabaseAdapter db) {
		this.db = db;
		this.state = State.NONE;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
		switch (this.state) {
			case ID_KATALOG:
				this.catalogId.append(ch, start, length);
			break;
			case JMENO:
				this.catalogTitle.append(ch, start, length);
			break;
			case JPG_OFICIALNI:
				this.jpg.append(ch, start, length);
			break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (this.state) {
			case CWG:
				if (localName.equals("cwg")) {
					this.state = State.NONE;

					// Save to DB
					String sCatalogId = this.catalogId.toString().trim();
					String sCatalogTitle = this.catalogTitle.toString().trim();
					String sJpg = this.jpg.toString().trim();
					Cursor cur = db.getCwgByCatalogId(sCatalogId);
					if (cur == null) {
						db.addCwg(sCatalogTitle, sCatalogTitle, sCatalogId, sJpg, 0);
					} else {
						long id = cur.getLong(cur.getColumnIndex("_id"));
						int count = cur.getInt(cur.getColumnIndex("count"));
						String title = cur.getString(cur.getColumnIndex("title"));
						db.updateCwg(id, title, sCatalogTitle, sCatalogId, sJpg, count);
						cur.close();
					}
				}
			break;
			case ID_KATALOG:
				if (localName.equals("id_katalog")) {
					this.state = State.CWG;
				}
			break;
			case JMENO:
				if (localName.equals("jmeno")) {
					this.state = State.CWG;
				}
			break;
			case JPG_OFICIALNI:
				if (localName.equals("jpg_oficialni")) {
					this.state = State.CWG;
				}
			break;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		switch (this.state) {
			case NONE:
				if (localName.equals("cwg")) {
					this.state = State.CWG;
					this.catalogId = new StringBuilder();
					this.catalogTitle = new StringBuilder();
					this.jpg = new StringBuilder();
				}
			break;
			case CWG:
				if (localName.equals("id_katalog")) {
					this.state = State.ID_KATALOG;
				} else if (localName.equals("jmeno")) {
					this.state = State.JMENO;
				} else if (localName.equals("jpg_oficialni")) {
					this.state = State.JPG_OFICIALNI;
				}
			break;
		}
	}

}

class CatalogImport extends Import {
	private static final String TAG = "CwgCatalogImport";

	CatalogImport() {
	}

	void importData(DatabaseAdapter db) throws ImportException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser parser = factory.newSAXParser();
			CatalogHandler handler = new CatalogHandler(db);
			parser.parse(this.getInput(), handler);
		} catch (ParserConfigurationException pce) {
			throw new ImportException(pce.getClass().getName() + ": " + pce.getMessage(), pce);
		} catch (SAXException saxe) {
			throw new ImportException(saxe.getClass().getName() + ": " + saxe.getMessage(), saxe);
		} catch (IOException ioe) {
			throw new ImportException(ioe.getClass().getName() + ": " + ioe.getMessage(), ioe);
		}
	}
}
