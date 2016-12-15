package edu.buffalo.rms.bluemountain.oplog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Pattern;

import edu.buffalo.rms.bluemountain.databaseshim.BmSQLiteDatabase;
import edu.buffalo.rms.bluemountain.databaseshim.BmUtilsSingleton;

/**
 * @author Aniruddh Adkar
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This class contain specific implementation of SQLiteDatabase class based on recording Operations Logs
 */
public class BmOpLogSQLiteDatabase extends BmSQLiteDatabase {
    private final String TAG = BmOpLogSQLiteDatabase.class.getSimpleName();

    private SQLiteDatabase sqLiteDB;
    private Context relatedContext;
    private String dbFileName;

    public BmOpLogSQLiteDatabase(SQLiteDatabase db, Context relatedContext, String dbFileName) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "constructor() called with Database Name : " + dbFileName);
        }

        this.sqLiteDB = db;
        this.relatedContext = relatedContext;
        this.dbFileName = dbFileName;
    }

    @Override
    public void init() {
        //create database task here
    }

    @Override
    public void clear() {
        OpLogsSyncAdapter.clear();

    }

    @Override
    public void close() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "close() called");
        }

        if (null != sqLiteDB) {
            sqLiteDB.close();
        }
    }

    @Override
    public void onCreate() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called");
        }
    }

    @Override
    public BmOpLogSQLiteDatabase getWritableDatabase(SQLiteDatabase db) {
        // setting writable database
        this.sqLiteDB = db;

        return this;
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "insert() called on databaseTable : " + table);
        }

//        long localEnd, writeEnd;
//        long start = System.nanoTime();

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.OP_LOG, BmUtilsSingleton.Operation.INSERT, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        long result = sqLiteDB.insert(table, nullColumnHack, values);
        testObject.recordNativeExecutionTime();

//        localEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_insert:local", String.valueOf(localEnd-start));

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
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, "_insert");
        testObject.recordWithFrameworkExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

//        writeEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_insert:withLogging", String.valueOf(writeEnd-start));

        return result;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "delete() called on databaseTable : " + table);
        }

//        long localEnd,writeEnd;
//        long start = System.nanoTime();

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.OP_LOG, BmUtilsSingleton.Operation.DELETE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        int result = sqLiteDB.delete(table, whereClause, whereArgs);
        testObject.recordNativeExecutionTime();

//        localEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_delete:local", String.valueOf(localEnd-start));
//        Log.d(TAG_STMT, "delete() sql:" + "DELETE FROM " + table +
//                (!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : "") + Utils.getArgsString(whereArgs));

        String logStatement = "sql:" + "DELETE FROM " + table +
                (!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : "") + Utils.getArgsString(whereArgs);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, "_delete");

        testObject.recordWithFrameworkExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

//        writeEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_delete:withLogging", String.valueOf(writeEnd-start));

        return result;
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "rawQuery() called with sql : " + sql);
        }

//        long localEnd, writeEnd;
//        long start = System.nanoTime();

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.OP_LOG, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        Cursor cursor = sqLiteDB.rawQuery(sql, selectionArgs);

//        Log.d(Globals.TAG_TIME+"_rawQry:local", String.valueOf(localEnd-start));
//        Log.d(TAG_STMT, "rawQuery() sql:" + sql + " selectionArgs:" + Utils.getArgsString(selectionArgs));

        String logStatement = Utils.formatSql(sql.toString()) + " selectionArgs: " + Utils.getArgsString(selectionArgs);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, "_rawQuery");

        testObject.recordWithFrameworkExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

//        writeEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME + "_rawQry:withLogging", String.valueOf(writeEnd - start));

        return cursor;
    }


    @Override
    public Cursor query(boolean distinct, String tables, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "query() called on databaseTable : " + tables);
        }

//        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.OP_LOG, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        Cursor cursor = sqLiteDB.query(distinct, tables, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
//        testObject.recordNativeExecutionTime();
//        logQuery(distinct, tables, columns, selection, selectionArgs, groupBy, having, orderBy, limit, testObject);
        return cursor;
    }

    public void logQuery(boolean distinct, String tables, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, BmUtilsSingleton.TestLatency testObject){

        final Pattern sLimitPattern =
                Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?");
        if (TextUtils.isEmpty(groupBy) && !TextUtils.isEmpty(having)) {
            throw new IllegalArgumentException(
                    "HAVING clauses are only permitted when using a groupBy clause");
        }
        if (!TextUtils.isEmpty(limit) && !sLimitPattern.matcher(limit).matches()) {
            throw new IllegalArgumentException("invalid LIMIT clauses:" + limit);
        }

        StringBuilder query = new StringBuilder(120);

        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.length != 0) {
            appendColumns(query, columns);
        } else {
            query.append("* ");
        }
        query.append("FROM ");
        query.append(tables);
        appendClause(query, " WHERE ", selection);
        appendClause(query, " GROUP BY ", groupBy);
        appendClause(query, " HAVING ", having);
        appendClause(query, " ORDER BY ", orderBy);
        appendClause(query, " LIMIT ", limit);
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(Utils.formatSql(query.toString()), "_query");

        testObject.recordWithFrameworkExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);
    }

    /**
     * Add the names that are non-null in columns to s, separating
     * them with commas.
     */
    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;

        for (int i = 0; i < n; i++) {
            String column = columns[i];

            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (!TextUtils.isEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "update() called on databaseTable : " + table);
        }

//        long end,localEnd,writeEnd;
//        long start = System.nanoTime();

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.OP_LOG, BmUtilsSingleton.Operation.UPDATE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        int result = sqLiteDB.update(table, values, whereClause, whereArgs);
        testObject.recordNativeExecutionTime();

//        localEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_update:local", String.valueOf(localEnd-start));

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
        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(logStatement, "_update");

        testObject.recordWithFrameworkExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

//        writeEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_update:withLogging", String.valueOf(writeEnd-start));

        return result;
    }

    @Override
    public void execSQL(String sql) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "execSQL() called with sql : " + sql);
        }
        BmUtilsSingleton.TestLatency testObject = null;
        String sqlLower = sql.toLowerCase();
        if (sqlLower.startsWith("create")) {
            testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.OP_LOG, BmUtilsSingleton.Operation.EXEC_SQL, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        }

//        long localEnd,writeEnd;
//        long start = System.nanoTime();

        sqLiteDB.execSQL(sql);
        if (null != testObject) {
            testObject.recordNativeExecutionTime();
        }

//        localEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_exec:local", String.valueOf(localEnd-start));
//        Log.d(TAG_STMT, "execSQL() sql:" + sql);

        OpLogsSyncAdapter.getAdapter(relatedContext, sqLiteDB.getPath()).log(Utils.formatSql(sql), "_execSQL");
        if (null != testObject) {
            testObject.recordWithFrameworkExecutionTime();
            BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);
        }
//        writeEnd = System.nanoTime();
//        Log.d(Globals.TAG_TIME+"_exec:withLogging", String.valueOf(writeEnd-start));
    }

    @Override
    public void onUpgrade(int oldVersion, int newVersion) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onUpgrade() called with oldVersion : " + oldVersion + " , newVersion : " + newVersion);
        }
    }
}
