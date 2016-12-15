package edu.buffalo.rms.bluemountain.databaseshim;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author Aniruddh Adkar
 * @author Ajay Partap Singh Chhokar
 * @author Ramanpreet Singh Khinda
 *         <p>
 *         Created by raman on 11/20/16.
 *         <p>
 *         This class provides an interface that defines the methods the implementing class need to support
 *         and for which the implementing class wants to define their custom implementation
 */
public interface IBmSQLiteDatabase {

    void onCreate();

    void init();

    void clear();

    void close();

    BmSQLiteDatabase getWritableDatabase(SQLiteDatabase db);

    void onUpgrade(int oldVersion, int newVersion);

    void execSQL(String sql);

    Cursor rawQuery(String sql, String[] selectionArgs);

    Cursor query(boolean distinct, String databaseTable, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit);

    long insert(String databaseTable, String nullColumnCheck, ContentValues initialValues);

    int update(String databaseTable, ContentValues newValues, String whereClause, String whereArgs[]);

    int delete(String databaseTable, String whereClause, String[] whereArgs);

}
