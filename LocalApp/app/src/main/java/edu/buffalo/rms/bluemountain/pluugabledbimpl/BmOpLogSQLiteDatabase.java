package edu.buffalo.rms.bluemountain.pluugabledbimpl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import edu.buffalo.rms.bluemountain.databaseshim.BmSQLiteDatabase;

/**
 * @author Aniruddh Adkar
 *         <p>
 *         Created by raman on 11/20/16.
 *         <p>
 *         This class contain specific implementation of SQLiteDatabase class based on recording Operations Logs
 */
public class BmOpLogSQLiteDatabase extends BmSQLiteDatabase {
    private final String TAG = BmOpLogSQLiteDatabase.class.getSimpleName();

    private SQLiteDatabase sqLiteDB;
    private Context relatedContext;
    private String dbFileName;

    public BmOpLogSQLiteDatabase(SQLiteDatabase db, Context relatedContext, String dbFileName) {
        Log.v(TAG, "constructor() called with Database Name : " + dbFileName);

        this.sqLiteDB = db;
        this.relatedContext = relatedContext;
        this.dbFileName = dbFileName;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate() called");
    }

    @Override
    public BmOpLogSQLiteDatabase getWritableDatabase(SQLiteDatabase db) {
        // setting writable database
        this.sqLiteDB = db;

        return this;
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        Log.v(TAG, "insert() called with nullColumnHack : " + nullColumnHack + " , initialValues : " + values);
        long localEnd, writeEnd;
        long start = System.nanoTime();

        long result = sqLiteDB.insert(table, nullColumnHack, values);
        localEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_insert:local", String.valueOf(localEnd-start));
        //taken from insertWithOnConflict() in SQLiteDatabase.java
        //Android/sdk1/sources/android-23/android/database/sqlite/SQLiteDatabase.java

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT");
        //TODO assign Conflict algorithm
//        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');

        Object[] bindArgs = null;
        int size = (values != null && values.size() > 0)
                ? values.size() : 0;
        if (size > 0) {
            bindArgs = new Object[size];
            int i = 0;
            for (String colName : values.keySet()) {
                sql.append((i > 0) ? "," : "");
                sql.append(colName);
                bindArgs[i++] = values.get(colName);
            }
            sql.append(')');
            sql.append(" VALUES (");
            for (i = 0; i < size; i++) {
                sql.append((i > 0) ? ",?" : "?");
            }
        } else {
            sql.append(nullColumnHack + ") VALUES (NULL");
        }
        sql.append(')');

        //TODO add bindArgs for other object types
        String logStatement = Utils.formatSql(sql.toString()) + " bindArgs: " + Utils.getArgsString(bindArgs);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, start, "_insert");
        writeEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_insert:withLogging", String.valueOf(writeEnd-start));
        return result;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        Log.v(TAG, "delete() called with databaseTable : " + table + " , whereClause : " + whereClause + " , whereArgs : " + whereArgs);

        long localEnd,writeEnd;
        long start = System.nanoTime();

        int result = sqLiteDB.delete(table, whereClause, whereArgs);
        localEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_delete:local", String.valueOf(localEnd-start));
//        Log.d(TAG_STMT, "delete() sql:" + "DELETE FROM " + table +
//                (!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : "") + Utils.getArgsString(whereArgs));
        String logStatement = "sql:" + "DELETE FROM " + table +
                (!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : "") + Utils.getArgsString(whereArgs);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, start, "_delete");
        writeEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_delete:withLogging", String.valueOf(writeEnd-start));
        return result;
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        Log.v(TAG, "rawQuery() called with sql : " + sql + " , selectionArgs : " + selectionArgs);

        long localEnd,writeEnd;
        long start = System.nanoTime();
        Cursor cursor = sqLiteDB.rawQuery(sql, selectionArgs);
        localEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_rawQry:local", String.valueOf(localEnd-start));
//        Log.d(TAG_STMT, "rawQuery() sql:" + sql + " selectionArgs:" + Utils.getArgsString(selectionArgs));

        String logStatement = Utils.formatSql(sql.toString()) + " selectionArgs: " + Utils.getArgsString(selectionArgs);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, start, "_rawQuery");
        writeEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_rawQry:withLogging", String.valueOf(writeEnd-start));
        return cursor;
    }


    @Override
    public Cursor query(boolean distinct, String databaseTable, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        Log.v(TAG, "query() called with distinct : " + distinct + " , databaseTable : " + databaseTable
                + " , columns : " + columns + " , selection : " + selection + " , selectionArgs : " + selectionArgs
                + " , groupBy : " + groupBy + " , having : " + having + " , orderBy : " + orderBy + " , limit : " + limit);

        return sqLiteDB.query(distinct, databaseTable, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        Log.v(TAG, "update() called with databaseTable :" + table + " , newValues : " + values + " , whereClause : " + whereClause + " , whereArgs : " + whereArgs);

        long end,localEnd,writeEnd;
        long start = System.nanoTime();

        int result = sqLiteDB.update(table, values, whereClause, whereArgs);
        localEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_update:local", String.valueOf(localEnd-start));

        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        //sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(table);
        sql.append(" SET ");

        // move all bind args to one array
        int setValuesSize = values.size();
        int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
        Object[] bindArgs = new Object[bindArgsSize];
        int i = 0;
        for (String colName : values.keySet()) {
            sql.append((i > 0) ? "," : "");
            sql.append(colName);
            bindArgs[i++] = values.get(colName);
            sql.append("=?");
        }
        if (whereArgs != null) {
            for (i = setValuesSize; i < bindArgsSize; i++) {
                bindArgs[i] = whereArgs[i - setValuesSize];
            }
        }
        if (!TextUtils.isEmpty(whereClause)) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }
        //TODO add bindArgs for other object types
        String logStatement = Utils.formatSql(sql.toString()) + " bindArgs: " + Utils.getArgsString(bindArgs);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, start, "_update");
        writeEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_update:withLogging", String.valueOf(writeEnd-start));
        return result;
    }

    @Override
    public void execSQL(String sql) {
        Log.v(TAG, "execSQL() called with sql : " + sql);

        long localEnd,writeEnd;
        long start = System.nanoTime();

        sqLiteDB.execSQL(sql);
        localEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_exec:local", String.valueOf(localEnd-start));
//        Log.d(TAG_STMT, "execSQL() sql:" + sql);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(Utils.formatSql(sql), start, "_execSQL");
        writeEnd = System.nanoTime();
        Log.d(Globals.TAG_TIME+"_exec:withLogging", String.valueOf(writeEnd-start));
    }

    @Override
    public void onUpgrade(int oldVersion, int newVersion) {
        Log.v(TAG, "onUpgrade() called with oldVersion :" + oldVersion + " , newVersion : " + newVersion);
    }
}
