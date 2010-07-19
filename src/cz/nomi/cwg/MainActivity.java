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
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class MainActivity extends Activity {
	class CustomViewBinder implements SimpleCursorAdapter.ViewBinder {
		private ColorStateList oldColors = null;

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
					if (oldColors == null) {
						oldColors = textView.getTextColors();
					}
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

	private enum Mode {
		NORMAL, DUPLICITY, WITHOUT_CATALOG;
	}
	private DatabaseAdapter db;
	private SimpleCursorAdapter listAdapter;
	private Cursor listCursor;
	private long mergeId = 0;
	private Mode mode = Mode.NORMAL;
	private Menu optionsMenu;
	private EditText search;
	private Thread filterThread;
	private SharedPreferences settings;

	private void addCwgBySearch() {
		String title = search.getText().toString().trim();
		if (title.length() > 0) {
			db.addCwg(title, null, null, null, 1);
			listCursor.requery();
			listAdapter.notifyDataSetInvalidated();
			listAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.main);

		db = new DatabaseAdapter(this);
		db.open();

		final ListView list = (ListView) findViewById(R.id.list);
		search = (EditText) findViewById(R.id.search);
		final ImageView button = (ImageView) findViewById(R.id.button);

		settings =
				PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// Focus to search box
		search.requestFocus();

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
		listAdapter.setViewBinder(new CustomViewBinder());
		list.setAdapter(listAdapter);
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
				reloadFilter(true);
			}
		});
		search.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN &&
						keyCode == KeyEvent.KEYCODE_ENTER) {
					addCwgBySearch();
					return true;
				} else {
					return false;
				}
			}
		});

		// Add title
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				addCwgBySearch();
			}
		});

		// List click
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id) {
				if (MainActivity.this.mergeId == 0) {
					SharedPreferences settings =
							PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					boolean autoOpen = settings.getBoolean("auto_show_cwg",
							getText(R.string.pref_auto_show_cwg).equals("true"));

					if (autoOpen) {
						Intent myIntent = new Intent(MainActivity.this, ShowActivity.class);
						myIntent.putExtra("cwgId", id);
						MainActivity.this.startActivity(myIntent);
					} else {
						MainActivity.this.openContextMenu(arg1);
					}
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

	private OutputStream newFileOutput(String fileName) {
		File file = Storage.getFile(fileName);
		if (file == null) {
			Toast.makeText(MainActivity.this, getText(R.string.cant_open_external_storage),
					Toast.LENGTH_LONG).show();
			return null;
		} else {
			try {
				return new FileOutputStream(file);
			} catch (FileNotFoundException fnfe) {
				Toast.makeText(MainActivity.this, getText(R.string.cant_open_file) +
					" " + fnfe.getMessage(), Toast.LENGTH_LONG).show();
				return null;
			}
		}
	}

	private ProgressInputStream newFileInput(String fileName) {
		File file = Storage.getFile(fileName);
		if (file == null) {
			Toast.makeText(MainActivity.this, getText(R.string.cant_open_external_storage),
					Toast.LENGTH_LONG).show();
			return null;
		} else {
			try {
				return new ProgressInputStream(
						this,
						new FileInputStream(file),
						file.length());
			} catch (FileNotFoundException fnfe) {
				Toast.makeText(MainActivity.this, getText(R.string.cant_open_file) +
					" " + fnfe.getMessage(), Toast.LENGTH_LONG).show();
				return null;
			}
		}
	}

	private void doImport(final Import importer, final ProgressInputStream input) {
		if (input == null) {
			return;
		}
		final Handler handler = new Handler();
		new Thread() {
			@Override
			public void run() {
				db.beginTransaction();
				importer.setInput(input);
				
				try {
					importer.importData(db);
					db.endTransaction();
				} catch (final SQLiteConstraintException se) {
					db.rollback();
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this,
									getText(R.string.cant_import_duplicity),
									Toast.LENGTH_LONG).show();
						}
					});
				} catch (final ImportException ie) {
					db.rollback();
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, ie.getMessage(),
								Toast.LENGTH_LONG).show();
						}
					});
				}

				try {
					input.close();
				} catch (final IOException ioe) {
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, ioe.getClass().getName() +
								": " + ioe.getMessage(),	Toast.LENGTH_LONG).show();
						}
					});
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

	private Thread doExport(final Export exporter, final OutputStream output) {
		if (output == null) {
			return null;
		}
		final ProgressDialog progress = new ProgressDialog(this);
		progress.setIndeterminate(true);
		progress.setCancelable(false);
		progress.setMessage(getText(R.string.please_wait));
		progress.setTitle(R.string.exporting);
		progress.show();

		final Handler handler = new Handler();
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					exporter.setOutput(output);
					exporter.exportData(db.getAllCwg());
				} catch (final ExportException ioe) {
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, ioe.getMessage(),
								Toast.LENGTH_LONG).show();
						}
					});
				}

				try {
					output.close();
				} catch (final IOException ioe) {
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, ioe.getClass().getName() +
								": " + ioe.getMessage(),	Toast.LENGTH_LONG).show();
						}
					});
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
		};
		thread.start();
		return thread;
	}

	private void reloadFilter(final boolean sleep) {
		final Handler handler = new Handler();
		if (filterThread != null) {
			filterThread.interrupt();
			try {
				filterThread.join();
			} catch (InterruptedException ie) {
				return;
			}
		}
		filterThread = new Thread() {
			@Override
			public void run() {
				try {
					if (sleep) {
						Thread.sleep(200);
					}
				} catch (InterruptedException ie) {
					return;
				}
				String text = search.getText().toString().trim();
				if (text.length() == 0) {
					switch (MainActivity.this.mode) {
						case NORMAL:
							listCursor = db.getAllCwg();
							break;
						case DUPLICITY:
							listCursor = db.getDuplicityCwg();
							break;
						case WITHOUT_CATALOG:
							listCursor = db.getWoCatalogCwg();
							break;
					}
				} else {
					switch (MainActivity.this.mode) {
						case NORMAL:
							listCursor = db.getFilteredCwg(text);
							break;
						case DUPLICITY:
							listCursor = db.getDuplicityFilteredCwg(text);
							break;
						case WITHOUT_CATALOG:
							listCursor = db.getWoCatalogFilteredCwg(text);
							break;
					}
				}
				handler.post(new Runnable() {
					public void run() {
						listAdapter.changeCursor(listCursor);
						listAdapter.notifyDataSetInvalidated();
						listAdapter.notifyDataSetChanged();
					}
				});
			}
		};
		filterThread.start();
	}

	private void reloadMode() {
		MenuItem all = optionsMenu.findItem(R.id.menuAll);
		MenuItem duplicity = optionsMenu.findItem(R.id.menuDuplicity);
		MenuItem woCatalog = optionsMenu.findItem(R.id.menuWoCatalog);
		switch (this.mode) {
			case NORMAL:
				all.setVisible(false);
				duplicity.setVisible(true);
				woCatalog.setVisible(true);
				this.setTitle(R.string.main_activity);
				break;
			case DUPLICITY:
				all.setVisible(true);
				duplicity.setVisible(false);
				woCatalog.setVisible(true);
				this.setTitle(R.string.main_activity_duplicity);
				break;
			case WITHOUT_CATALOG:
				all.setVisible(true);
				duplicity.setVisible(true);
				woCatalog.setVisible(false);
				this.setTitle(R.string.main_activity_without_catalog);
				break;
		}
		reloadFilter(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		optionsMenu = menu;

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuAll:
				this.mode = Mode.NORMAL;
				reloadMode();
				return true;
			case R.id.menuDuplicity:
				this.mode = Mode.DUPLICITY;
				reloadMode();
				return true;
			case R.id.menuWoCatalog:
				this.mode = Mode.WITHOUT_CATALOG;
				reloadMode();
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
			case R.id.menuExportWeb: {
				final String url = settings.getString("web_export_url", "");
				final String token = settings.getString("web_export_token", "");

				if (url.length() == 0) {
					Toast.makeText(this, getText(R.string.url_is_not_set),
							Toast.LENGTH_LONG).show();
					return true;
				}

				if (token.length() == 0) {
					Toast.makeText(this, getText(R.string.token_is_not_set),
							Toast.LENGTH_LONG).show();
					return true;
				}

				final ByteArrayOutputStream writer = new ByteArrayOutputStream();
				CsvExport webExport = new CsvExport();
				final Thread exportThread = doExport(webExport, writer);

				final Handler handler = new Handler();
				final ProgressDialog progress = new ProgressDialog(this);
				progress.setIndeterminate(true);
				progress.setCancelable(false);
				progress.setMessage(getText(R.string.please_wait));
				progress.setTitle(R.string.uploading);
				new Thread() {
					@Override
					public void run() {
						try {
							synchronized (exportThread) {
								while (exportThread.isAlive()) {
									try {
										exportThread.join();
									} catch (InterruptedException ie) {}
								}
							}

							handler.post(new Runnable() {
								public void run() {
									progress.show();
								}
							});

							HttpClient httpClient = new DefaultHttpClient();
							HttpPost httpPost = new HttpPost(url);

							List<NameValuePair> data = new ArrayList<NameValuePair>(2);
							data.add(new BasicNameValuePair("token", token));
							data.add(new BasicNameValuePair("csv", writer.toString("UTF-8")));
							httpPost.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));

							final HttpResponse response = httpClient.execute(httpPost);
							if (response.getStatusLine().getStatusCode() != 200) {
								final Header[] headers = response.getHeaders("Error");
								if (headers.length > 0) {
									for (final Header header : response.getHeaders("Error")) {
										handler.post(new Runnable() {
											public void run() {
												Toast.makeText(MainActivity.this, header.getValue(), Toast.LENGTH_LONG).show();
											}
										});
									}
								} else {
									handler.post(new Runnable() {
										public void run() {
											Toast.makeText(MainActivity.this,
												Integer.toString(response.getStatusLine().getStatusCode()) + " " +
												response.getStatusLine().getReasonPhrase(),
												Toast.LENGTH_LONG).show();
										}
									});
								}
							}

							handler.post(new Runnable() {
								public void run() {
									progress.dismiss();
								}
							});
						} catch (final ClientProtocolException cpe) {
							handler.post(new Runnable() {
								public void run() {
									Toast.makeText(MainActivity.this, cpe.getClass().getName() + ": " + cpe.getMessage(),
											Toast.LENGTH_LONG).show();
								}
							});
						} catch (final IOException ioe) {
							handler.post(new Runnable() {
								public void run() {
									Toast.makeText(MainActivity.this, ioe.getClass().getName() + ": " + ioe.getMessage(),
										Toast.LENGTH_LONG).show();
								}
							});
						}
					}
				}.start();

				return true;
			}
			case R.id.menuImportCsv:
				CsvImport csvImport = new CsvImport();
				ProgressInputStream csvIn = newFileInput("cwg.csv");
				doImport(csvImport, csvIn);
				return true;
			case R.id.menuImportCatalog:
				try {
					boolean catalogUseZip = settings.getBoolean("catalog_use_zip",
							getText(R.string.pref_catalog_use_zip).equals("true"));
					InputStream input;
					long length;
					if (catalogUseZip) {
						String catalogZipUrl = settings.getString("catalog_zip_url",
							getText(R.string.pref_catalog_zip_url).toString());
						URL url = new URL(catalogZipUrl);
						HttpURLConnection http = (HttpURLConnection) url.openConnection();
						ZipInputStream zip = new ZipInputStream(http.getInputStream());
						input = zip;
						length = zip.getNextEntry().getSize();
					} else {
						String catalogUrl = settings.getString("catalog_url",
							getText(R.string.pref_catalog_url).toString());

						URL url = new URL(catalogUrl);
						HttpURLConnection http = (HttpURLConnection) url.openConnection();
						input = http.getInputStream();
						length = http.getContentLength();
					}
					CatalogImport catalogImport = new CatalogImport();
					doImport(catalogImport, new ProgressInputStream(this,
						input, length));
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

		boolean autoOpen = settings.getBoolean("auto_show_cwg",
				getText(R.string.pref_auto_show_cwg).equals("true"));
		menu.findItem(R.id.menuShow).setVisible(!autoOpen);

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
				if (catalogTitle == null) {
					alertDialog.setTitle(title);
				} else {
					alertDialog.setTitle(catalogTitle);
				}
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (this.mode != Mode.NORMAL) {
				this.mode = Mode.NORMAL;
				reloadMode();
				return true;
			} else {
				return super.onKeyDown(keyCode, event);
			}
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		reloadFilter(false);
	}
}
