package cz.nomi.cwg;

import android.database.Cursor;
import java.io.IOException;
import java.io.OutputStream;

public class TextExport extends Export {
	public TextExport(OutputStream output) {
		super(output);
	}

	public void exportData(Cursor cursor) throws IOException {
		while (cursor.moveToNext()) {
			output.write(
					(cursor.getString(cursor.getColumnIndex("title")) +
					"\n")
					.getBytes("UTF-8"));
		}
	}
}
