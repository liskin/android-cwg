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

public class OutputProgressDialog {
	private ProgressDialog dialog = null;
	private Handler handler = null;
	private int position = 0;

	public OutputProgressDialog(final Context context, Handler handler) {
		this.handler = handler;
		handler.post(new Runnable() {
			public void run() {
				dialog = new ProgressDialog(context);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setTitle(R.string.exporting);
				dialog.setMessage(context.getText(R.string.please_wait));
				dialog.setCancelable(false);
				dialog.show();
			}
		});
	}
	
	public void dismiss() {
		handler.post(new Runnable() {
			public void run() {
				if (dialog != null) {
					dialog.dismiss();
				}
			}
		});
	}
}
