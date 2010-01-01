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
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class CatalogImport extends Import {
	private static final String TAG = "CwgCatalogImport";

	public CatalogImport() {
	}

	public void importData(DatabaseAdapter db) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.parse(this.getInput());
			Node node = dom.getDocumentElement().getFirstChild();
			while (node != null) {
				if (node.getNodeName().equals("cwg")) {
					Node node2 = node.getFirstChild();
					String idCatalog = null;
					String catalogTitle = null;
					String jpg = null;
					while (node2 != null) {
						if (node2.getNodeName().equals("id_katalog")) {
							idCatalog = node2.getFirstChild().getNodeValue();
						} else if (node2.getNodeName().equals("jmeno")) {
							catalogTitle = node2.getFirstChild().getNodeValue();
						} else if (node2.getNodeName().equals("jpg_oficialni")) {
							jpg = node2.getFirstChild().getNodeValue();
						}
						node2 = node2.getNextSibling();
					}

					if (idCatalog != null && catalogTitle != null && jpg != null) {
						Cursor cur = db.getCwgByCatalogId(idCatalog);
						if (cur == null) {
							db.addCwg(catalogTitle, catalogTitle, idCatalog, jpg, 0);
						} else {
							long id = cur.getLong(cur.getColumnIndex("_id"));
							int count = cur.getInt(cur.getColumnIndex("count"));
							String title = cur.getString(cur.getColumnIndex("title"));
							db.updateCwg(id, title, catalogTitle, idCatalog, jpg, count);
							cur.close();
						}
					}
				}
				node = node.getNextSibling();
			}
		} catch (FactoryConfigurationError fce) {
			Log.e(TAG, "Factory configuration error: " + fce.getMessage());
		} catch (ParserConfigurationException pce) {
			Log.e(TAG, "Parser configuration error: " + pce.getMessage());
		} catch (SAXException se) {
			Log.e(TAG, "SAX error: " + se.getMessage());
		} catch (IOException ioe) {
			Log.e(TAG, "IO exception: " + ioe.getMessage());
		}
	}
}
