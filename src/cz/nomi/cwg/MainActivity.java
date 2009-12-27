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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
	DatabaseAdapter db;
	boolean duplicity = false;
	SimpleCursorAdapter listAdapter;
	Cursor listCursor;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.main);

		db = new DatabaseAdapter(this);
		db.open();

		final MainActivity activity = this;
		final ListView list = (ListView) findViewById(R.id.list);
		final EditText search = (EditText) findViewById(R.id.search);
		final ImageView button = (ImageView) findViewById(R.id.button);

		// Title list
		listCursor = db.getAllCwg();
		listAdapter = new SimpleCursorAdapter(
				this,
				R.layout.list_item,
				listCursor,
				new String[]{
					"title",
					"version",
					"count"
				},
				new int[]{
					R.id.listItemTitle,
					R.id.listItemVersion,
					R.id.listItemCount
				});
		listAdapter.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence arg0) {
				if (activity.duplicity) {
					listCursor = db.getDuplicityFilteredCwg(arg0.toString());
				} else {
					listCursor = db.getFilteredCwg(arg0.toString());
				}
				return listCursor;
			}
		});
		list.setAdapter(listAdapter);
		list.setTextFilterEnabled(true);
		registerForContextMenu(list);

		// Search as you type
		search.addTextChangedListener(new TextWatcher() {
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				return;
			}

			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				return;
			}

			public void afterTextChanged(Editable arg0) {
				String text = search.getText().toString().trim();
				if (text.length() == 0) {
					list.clearTextFilter();
				} else {
					list.setFilterText(text);
				}
			}
		});

		// Add title
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				String title = search.getText().toString().trim();
				if (title.length() > 0) {
					db.addCwg(title, 0);
					listCursor.requery();
					listAdapter.notifyDataSetInvalidated();
					listAdapter.notifyDataSetChanged();
				}
			}
		});

		// List click
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				activity.openContextMenu(arg1);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		db.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuDuplicity:
				this.duplicity = !this.duplicity;
				if (this.duplicity) {
					listCursor = db.getDuplicityCwg();
				} else {
					listCursor = db.getAllCwg();
				}
				listAdapter.changeCursor(listCursor);
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			case R.id.menuExportText:
				try {
					File root = Environment.getExternalStorageDirectory();
					if (root.canWrite()) {
						File file = new File(root, "cwg.txt");
						OutputStream out = new FileOutputStream(file);
						TextExport export = new TextExport(out);
						export.exportData(db.getAllCwg());
						out.close();
					}
				} catch (IOException e) {
					Log.e(null, "Could not write file " + e.getMessage());
				}
				return true;
			case R.id.menuImportText:
				try {
					File root = Environment.getExternalStorageDirectory();
					if (root.canRead()) {
						File file = new File(root, "cwg.txt");
						InputStream in = new FileInputStream(file);
						TextImport tImport = new TextImport(in);
						tImport.importData(db);
						in.close();
						listCursor.requery();
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
					}
				} catch (IOException e) {
					Log.e(null, "Could not write file " + e.getMessage());
				}
				return true;
			case R.id.menuExportCsv:
				try {
					File root = Environment.getExternalStorageDirectory();
					if (root.canWrite()) {
						File file = new File(root, "cwg.csv");
						OutputStream out = new FileOutputStream(file);
						CsvExport export = new CsvExport(out);
						export.exportData(db.getAllCwg());
						out.close();
					}
				} catch (IOException e) {
					Log.e(null, "Could not write file " + e.getMessage());
				}
				return true;
			case R.id.menuImportCsv:
				try {
					File root = Environment.getExternalStorageDirectory();
					if (root.canRead()) {
						File file = new File(root, "cwg.csv");
						InputStream in = new FileInputStream(file);
						CsvImport tImport = new CsvImport(in);
						tImport.importData(db);
						in.close();
						listCursor.requery();
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
					}
				} catch (IOException e) {
					Log.e(null, "Could not write file " + e.getMessage());
				}
				return true;
			case R.id.menuEraseDb:
				AlertDialog dialog = new AlertDialog.Builder(this).create();
				dialog.setTitle(R.string.erase_database);
				dialog.setMessage(getText(R.string.are_you_sure_clean_whole_db));
				dialog.setButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						db.cleanDb();
						listCursor.requery();
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
						return;
					}
				});
				dialog.setButton2(getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				dialog.show();
				return true;
	    }
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Cursor cur = db.getCwg(info.id);
		menu.setHeaderTitle(cur.getString(cur.getColumnIndex("title")));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.menuAddSame:
				Cursor cur = db.getCwg(info.id);
				db.addCwg(
						cur.getString(cur.getColumnIndex("title")),
						cur.getInt(cur.getColumnIndex("version"))
				);
				listCursor.requery();
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			case R.id.menuAddOther:
				Cursor cur2 = db.getCwg(info.id);
				db.addCwg(
						cur2.getString(cur2.getColumnIndex("title")),
						0
				);
				listCursor.requery();
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			case R.id.menuCopy:
				Cursor cur3 = db.getCwg(info.id);
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboard.setText(cur3.getString(cur3.getColumnIndex("title")));
				return true;
			case R.id.menuRemove:
				db.removeCwg(info.id);
				listCursor.requery();
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
}
