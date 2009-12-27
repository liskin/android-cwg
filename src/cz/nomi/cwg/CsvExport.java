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
