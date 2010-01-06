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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

class ProgressInputStream extends BufferedInputStream {
	private long readed = 0;
	private Context context;
	private ProgressDialog dialog;
	private Handler handler;

	public ProgressInputStream(Context context, InputStream in, long size) {
		super(in);

		this.context = context;
		dialog = new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setTitle(R.string.importing);
		dialog.setMessage(context.getText(R.string.please_wait));
		dialog.setCancelable(false);
		dialog.setMax((int) size);
		dialog.show();

		handler = new Handler();
	}

	private void updateDialog() {
		dialog.setProgress((int) readed);
	}

	@Override
	public synchronized int read() throws IOException {
		this.readed++;
		updateDialog();
		return super.read();
	}

	@Override
	public synchronized int read(byte[] buffer, int offset, int length) throws IOException {
		int c = super.read(buffer, offset, length);
		if (c > 0) {
			this.readed += c;
			updateDialog();
		}
		return c;
	}

	@Override
	public synchronized void close() throws IOException {
		handler.post(new Runnable() {
			public void run() {
				dialog.dismiss();
			}
		});

		super.close();
	}
}
