package cz.nomi.cwg;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "cwg";
    private static final int DATABASE_VERSION = 1;

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE title (_id INTEGER PRIMARY KEY," +
                "title VARCHAR NOT NULL COLLATE LOCALIZED);");
        db.execSQL("CREATE TABLE cwg (_id INTEGER PRIMARY KEY," +
                "title_id INT NOT NULL," +
                "version INT NOT NULL," +
                "count INT NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
