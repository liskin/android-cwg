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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
	class CustomViewBinder implements SimpleCursorAdapter.ViewBinder {
		ColorStateList oldColors = null;

		public CustomViewBinder() {
		}

		public boolean setViewValue(View view, Cursor cursor, int index) {
			if (index == cursor.getColumnIndex("title")) {
				TextView textView = (TextView) view;
				if (cursor.isNull(cursor.getColumnIndex("catalog_id"))) {
					if (cursor.getInt(cursor.getColumnIndex("count")) > 0) {
						textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
					} else {
						textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					}
				} else {
					if (cursor.getInt(cursor.getColumnIndex("count")) > 0) {
						textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
					} else {
						textView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
					}
				}

				if (MainActivity.this.mergeId == cursor.getInt(cursor.getColumnIndex("_id"))) {
					oldColors = textView.getTextColors();
					textView.setTextColor(Color.GREEN);
				} else {
					if (oldColors != null) {
						textView.setTextColor(oldColors);
					}
				}
			}
			return false;
		}
	}

	private DatabaseAdapter db;
	private boolean duplicity = false;
	private SimpleCursorAdapter listAdapter;
	private Cursor listCursor;
	private long mergeId = 0;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.main);

		db = new DatabaseAdapter(this);
		db.open();

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
				if (MainActivity.this.duplicity) {
					listCursor = db.getDuplicityFilteredCwg(arg0.toString());
				} else {
					listCursor = db.getFilteredCwg(arg0.toString());
				}
				return listCursor;
			}
		});
		listAdapter.setViewBinder(new CustomViewBinder());
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
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id) {
				if (MainActivity.this.mergeId == 0) {
					MainActivity.this.openContextMenu(arg1);
				} else {
					db.mergeCwg(MainActivity.this.mergeId, id);
					listCursor.requery();
					listAdapter.notifyDataSetInvalidated();
					listAdapter.notifyDataSetChanged();
					MainActivity.this.mergeId = 0;
				}
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

	private ProgressInputStream newFileInput(String fileName) {
		File root = Environment.getExternalStorageDirectory();
		if (root.canWrite()) {
			File file = new File(root, fileName);
			try {
				return new ProgressInputStream(
						this,
						new FileInputStream(file),
						file.length());
			} catch (FileNotFoundException fnfe) {
				return null;
			}
		} else {
			return null;
		}
	}

	private void doImport(final Import importer, final ProgressInputStream input) {
		final Handler handler = new Handler();
		new Thread() {
			@Override
			public void run() {
				try {
					importer.setInput(input);
					db.beginTransaction();
					importer.importData(db);
					db.endTransaction();
					input.close();
				} catch (IOException ioe) {
					Toast.makeText(MainActivity.this, ioe.getClass().getName() +
						": " + ioe.getMessage(), Toast.LENGTH_LONG).show();
				}

				handler.post(new Runnable() {
					public void run() {
						listCursor.requery();
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
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
				ProgressInputStream textIn = newFileInput("cwg.txt");
				doImport(textImport, textIn);
				return true;
			case R.id.menuExportCsv:
				CsvExport csvExport = new CsvExport();
				doExport(csvExport, newFileOutput("cwg.csv"));
				return true;
			case R.id.menuImportCsv:
				CsvImport csvImport = new CsvImport();
				ProgressInputStream csvIn = newFileInput("cwg.csv");
				doImport(csvImport, csvIn);
				return true;
			case R.id.menuImportCatalog:
				try {
					SharedPreferences settings =
							PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					String catalogUrl = settings.getString("catalog_url",
							getText(R.string.pref_catalog_url).toString());

					CatalogImport catalogImport = new CatalogImport();
					URL url = new URL(catalogUrl);
					HttpURLConnection http = (HttpURLConnection) url.openConnection();
					doImport(catalogImport, new ProgressInputStream(this,
							http.getInputStream(), http.getContentLength()));
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
			case R.id.menuAutoMerge:
				final ProgressDialog progress = new ProgressDialog(this);
				progress.setIndeterminate(true);
				progress.setCancelable(false);
				progress.setMessage(getText(R.string.please_wait));
				progress.setTitle(R.string.auto_merging);
				progress.show();

				final Handler handler = new Handler();
				new Thread() {
					@Override
					public void run() {
						db.autoMergeCwg();

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
			case R.id.menuMerge:
				if (this.mergeId == 0) {
					this.mergeId = info.id;
				} else {
					this.mergeId = 0;
				}
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
