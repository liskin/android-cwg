package cz.nomi.cwg;

import android.database.Cursor;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Export {
	protected OutputStream output;

	public Export(OutputStream output) {
		this.output = output;
	}

	public abstract void exportData(Cursor cursor) throws IOException;
}
