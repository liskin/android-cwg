package cz.nomi.cwg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextImport extends Import {
	public TextImport(InputStream input) {
		super(input);
	}

	public void importData(DatabaseAdapter db) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0) {
				db.addCwg(line, 0);
			}
		}
	}
}
