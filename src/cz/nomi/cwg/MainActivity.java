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

import android.R.id;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
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
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

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
					"count"
				},
				new int[]{
					R.id.listItemTitle,
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
					db.addCwg(title, null, null, null, 1);
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

	private OutputStream newFileOutput(String fileName) {
		File root = Environment.getExternalStorageDirectory();
		if (root.canRead()) {
			File file = new File(root, fileName);
			try {
				OutputStream out = new FileOutputStream(file);
				return out;
			} catch (FileNotFoundException fnfe) {
				return null;
			}
		} else {
			return null;
		}
	}

	private InputStream newFileInput(String fileName) {
		File root = Environment.getExternalStorageDirectory();
		if (root.canWrite()) {
			File file = new File(root, fileName);
			try {
				InputStream in = new FileInputStream(file);
				return in;
			} catch (FileNotFoundException fnfe) {
				return null;
			}
		} else {
			return null;
		}
	}

	private void doImport(final Import importer, final InputStream input) {
				final ProgressDialog progress = new ProgressDialog(this);
				progress.setIndeterminate(true);
				progress.setCancelable(false);
				progress.setMessage(getText(R.string.please_wait));
				progress.setTitle(R.string.importing);
				progress.show();

				final Handler handler = new Handler();
				new Thread() {
					@Override
					public void run() {
						try {
							importer.setInput(input);
							db.beginTransaction();
							importer.importData(db);
							db.endTransaction();
						} catch (IOException ioe) {
							Toast.makeText(MainActivity.this, ioe.getClass().getName() +
								": " + ioe.getMessage(), Toast.LENGTH_LONG).show();
						}

						handler.post(new Runnable() {
							public void run() {
								listCursor.requery();
								listAdapter.notifyDataSetInvalidated();
								listAdapter.notifyDataSetChanged();
								progress.dismiss();
							}
						});
					}
				}.start();
	}

	private void doExport(final Export exporter, final OutputStream output) {
				final ProgressDialog progress = new ProgressDialog(this);
				progress.setIndeterminate(true);
				progress.setCancelable(false);
				progress.setMessage(getText(R.string.please_wait));
				progress.setTitle(R.string.exporting);
				progress.show();

				final Handler handler = new Handler();
				new Thread() {
					@Override
					public void run() {
						try {
							exporter.setOutput(output);
							exporter.exportData(db.getAllCwg());
						} catch (IOException ioe) {
							Toast.makeText(MainActivity.this, ioe.getClass().getName() +
									": " + ioe.getMessage(), Toast.LENGTH_LONG).show();
						}

						handler.post(new Runnable() {
							public void run() {
								listCursor.requery();
								listAdapter.notifyDataSetInvalidated();
								listAdapter.notifyDataSetChanged();
								progress.dismiss();
							}
						});
					}
				}.start();
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
				TextExport textExport = new TextExport();
				doExport(textExport, newFileOutput("cwg.txt"));
				return true;
			case R.id.menuImportText:
				TextImport textImport = new TextImport();
				doImport(textImport, newFileInput("cwg.txt"));
				return true;
			case R.id.menuExportCsv:
				CsvExport csvExport = new CsvExport();
				doExport(csvExport, newFileOutput("cwg.csv"));
				return true;
			case R.id.menuImportCsv:
				CsvImport csvImport = new CsvImport();
				doImport(csvImport, newFileInput("cwg.csv"));
				return true;
			case R.id.menuImportCatalog:
				try {
					SharedPreferences settings =
							PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					String catalogUrl = settings.getString("catalog_url",
							getText(R.string.pref_catalog_url).toString());

					CatalogImport catalogImport = new CatalogImport();
					URL url = new URL(catalogUrl);
					doImport(catalogImport, url.openStream());
				} catch (IOException ioe) {
					Toast.makeText(this, ioe.getClass().getName() + ": " + ioe.getMessage(),
							Toast.LENGTH_LONG).show();
				}
				return true;
			case R.id.menuStatistics:
				this.startActivity(new Intent(this, StatsActivity.class));
				return true;
			case R.id.menuPreference:
				this.startActivity(new Intent(this, PreferenceActivity.class));
				return true;
			case R.id.menuEraseDb:
				AlertDialog dialog = new AlertDialog.Builder(this).create();
				dialog.setTitle(R.string.erase_database);
				dialog.setMessage(getText(R.string.are_you_sure_clean_whole_db));
				dialog.setButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						db.eraseDb();
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
		cur.close();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cur;
		switch (item.getItemId()) {
			case R.id.menuShow:
				Intent myIntent = new Intent(this, ShowActivity.class);
				myIntent.putExtra("cwgId", info.id);
				this.startActivity(myIntent);
				return true;
			case R.id.menuAddSame:
				db.incCwg(info.id);
				listCursor.requery();
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			case R.id.menuAddOther:
				cur = db.getCwg(info.id);
				db.addCwg(
						cur.getString(cur.getColumnIndex("title")),
						null,
						null,
						null,
						1
				);
				cur.close();
				listCursor.requery();
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			case R.id.menuRename:
				cur = db.getCwg(info.id);
				final long id = info.id;
				final String title = cur.getString(cur.getColumnIndex("title"));
				final String catalogTitle = cur.getString(cur.getColumnIndex("catalog_title"));
				final String catalogId = cur.getString(cur.getColumnIndex("catalog_id"));
				final String jpg = cur.getString(cur.getColumnIndex("jpg"));
				final int count = cur.getInt(cur.getColumnIndex("count"));
				cur.close();

				LayoutInflater factory = LayoutInflater.from(this);
				final View textEntryView = factory.inflate(R.layout.dialog_rename, null);
				final EditText edit = (EditText) textEntryView.findViewById(R.id.rename_title);
				edit.setText(title);
				cur.close();
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle(catalogTitle);
				alertDialog.setView(textEntryView);
				alertDialog.setButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						db.updateCwg(id, edit.getText().toString(), catalogTitle, catalogId, jpg, count);
						listCursor.requery();
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
					}
				});
				alertDialog.setButton2(getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				alertDialog.show();
				return true;
			case R.id.menuCopy:
				cur = db.getCwg(info.id);
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboard.setText(cur.getString(cur.getColumnIndex("title")));
				cur.close();
				return true;
			case R.id.menuRemoveOne:
				db.decCwg(info.id);
				listCursor.requery();
				listAdapter.notifyDataSetInvalidated();
				listAdapter.notifyDataSetChanged();
				return true;
			case R.id.menuDelete:
				AlertDialog dialog = new AlertDialog.Builder(this).create();
				dialog.setTitle(R.string.delete_cwg);
				dialog.setMessage(getText(R.string.are_you_sure_delete_cwg));
				dialog.setButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						db.deleteCwg(info.id);
						listCursor.requery();
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
					}
				});
				dialog.setButton2(getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				dialog.show();
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
}
