package edu.buffalo.rms.bluemountain.databaseshim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * @author Aniruddh Adkar
 * @author Ajay Partap Singh Chhokar
 * @author Ramanpreet Singh Khinda
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This is the parent class for all BlueMountain SQLite Database operations.
 *         Child classes of this class will be providing their specific implementation of the methods.
 */
public class BmSQLiteDatabase implements IBmSQLiteDatabase {
    private static final String TAG = BmUtilsSingleton.GLOBAL_TAG + BmSQLiteDatabase.class.getSimpleName();

    private SQLiteDatabase sqLiteDB;
    private Context relatedContext;
    private String dbFileName;

    public BmSQLiteDatabase() {

    }

    public BmSQLiteDatabase(SQLiteDatabase db, Context relatedContext, String dbFileName) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "constructor() called with Database Name : " + dbFileName);
        }

        this.sqLiteDB = db;
        this.relatedContext = relatedContext;
        this.dbFileName = dbFileName;
    }


    @Override
    public void onCreate() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called");
        }
    }

    @Override
    public void init() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "init() called");
        }
    }

    @Override
    public void clear() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "clear() called");
        }
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
    public BmSQLiteDatabase getWritableDatabase(SQLiteDatabase db) {
        // setting writable database
        this.sqLiteDB = db;

        return this;
    }

    @Override
    public long insert(String databaseTable, String nullColumnCheck, ContentValues initialValues) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "insert() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.NATIVE, BmUtilsSingleton.Operation.INSERT, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        long result = sqLiteDB.insert(databaseTable, nullColumnCheck, initialValues);

        // Recording Native Execution Time
        testObject.recordNativeExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return result;
    }

    @Override
    public int delete(String databaseTable, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "delete() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.NATIVE, BmUtilsSingleton.Operation.DELETE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        int result = sqLiteDB.delete(databaseTable, whereClause, whereArgs);

        // Recording Native Execution Time
        testObject.recordNativeExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return result;
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "rawQuery() called with sql : " + sql);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.NATIVE, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        Cursor result = sqLiteDB.rawQuery(sql, selectionArgs);

        // Recording Native Execution Time
        testObject.recordNativeExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return result;
    }


    @Override
    public Cursor query(boolean distinct, String databaseTable, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "query() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.NATIVE, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        Cursor result = sqLiteDB.query(distinct, databaseTable, columns, selection, selectionArgs, groupBy, having, orderBy, limit);

        // Recording Native Execution Time
        testObject.recordNativeExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return result;
    }

    @Override
    public int update(String databaseTable, ContentValues newValues, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "update() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.NATIVE, BmUtilsSingleton.Operation.UPDATE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        int result = sqLiteDB.update(databaseTable, newValues, whereClause, whereArgs);

        // Recording Native Execution Time
        testObject.recordNativeExecutionTime();
        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return result;
    }

    @Override
    public void execSQL(String sql) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "execSQL() called with sql : " + sql);
        }

        sqLiteDB.execSQL(sql);
    }

    @Override
    public void onUpgrade(int oldVersion, int newVersion) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onUpgrade() called with oldVersion : " + oldVersion + ", newVersion : " + newVersion);
        }
    }
}
