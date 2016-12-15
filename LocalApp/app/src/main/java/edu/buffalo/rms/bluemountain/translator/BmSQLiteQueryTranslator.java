package edu.buffalo.rms.bluemountain.translator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import edu.buffalo.rms.bluemountain.databaseshim.BmSQLiteDatabase;
import edu.buffalo.rms.bluemountain.pluugabledb.IBmPluggableSQLDB;
import edu.buffalo.rms.bluemountain.pluugabledbimpl.BmPluggableSQLDBImpl;

/**
 * @author Aniruddh Adkar
 *         <p>
 *         Created by raman on 11/20/16.
 *         <p>
 *         This class contain specific implementation of SQLiteDatabase class based on recording Operations Logs
 */
public class BmSQLiteQueryTranslator extends BmSQLiteDatabase {
    private final String TAG = BmSQLiteQueryTranslator.class.getSimpleName();

    private SQLiteDatabase sqLiteDB;
    private Context relatedContext;
    private String dbFileName;
    private IBmPluggableSQLDB pluggableSQLiteDatabaseImpl;

    public BmSQLiteQueryTranslator(SQLiteDatabase db, Context relatedContext, String dbFileName) {
        Log.v(TAG, "constructor() called with Database Name : " + dbFileName);
        pluggableSQLiteDatabaseImpl = new BmPluggableSQLDBImpl(db);
        this.dbFileName = dbFileName;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate() called");
    }

    @Override
    public BmSQLiteQueryTranslator getWritableDatabase(SQLiteDatabase db) {
        // setting writable database
        this.sqLiteDB = db;

        return this;
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        Log.v(TAG, "insert() called with nullColumnHack : " + nullColumnHack + " , initialValues : " + values);

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

//        //TODO add bindArgs for other object types
        String sqlString = Utils.formatSql(sql.toString()) + " bindArgs: " + Utils.getArgsString(bindArgs);
        long result = (long) pluggableSQLiteDatabaseImpl.execute(null,sqlString,Long.class.getSimpleName());
        return result;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        Log.v(TAG, "delete() called with databaseTable : " + table + " , whereClause : " + whereClause + " , whereArgs : " + whereArgs);

        Integer result = (Integer) pluggableSQLiteDatabaseImpl.execute(null,table,Integer.class.getSimpleName());
        return result;
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        Log.v(TAG, "rawQuery() called with sql : " + sql + " , selectionArgs : " + selectionArgs);

        Cursor c = pluggableSQLiteDatabaseImpl.executeCursor(null,sql,Cursor.class.getSimpleName());
        return c;
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
        String sqlString = Utils.formatSql(sql.toString()) + " bindArgs: " + Utils.getArgsString(bindArgs);

        Integer result = (Integer) pluggableSQLiteDatabaseImpl.execute(null,sqlString ,Long.class.getSimpleName());
        return result;
    }

    @Override
    public void execSQL(String sql) {
        Log.v(TAG, "execSQL() called with sql : " + sql);

        pluggableSQLiteDatabaseImpl.execute(null,sql,Long.class.getSimpleName());
    }

    @Override
    public void onUpgrade(int oldVersion, int newVersion) {
        Log.v(TAG, "onUpgrade() called with oldVersion :" + oldVersion + " , newVersion : " + newVersion);
    }
}
