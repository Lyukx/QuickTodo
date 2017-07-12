package jp.ac.titech.itpro.sdl.powertodo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by lyukaixie on 2017/07/12.
 */

public class QuickTodoDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "QuickTodo.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + TodoEntry.TABLE_NAME +
            " (" + TodoEntry._ID + " INTEGER PRIMARY KEY," +
            TodoEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
            TodoEntry.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
            TodoEntry.COLUMN_NAME_TIME + TEXT_TYPE + COMMA_SEP +
            TodoEntry.COLUMN_NAME_DONE + INT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXIST " + TodoEntry.TABLE_NAME;

    public QuickTodoDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    /* Inner class that defines the table contents */

}
