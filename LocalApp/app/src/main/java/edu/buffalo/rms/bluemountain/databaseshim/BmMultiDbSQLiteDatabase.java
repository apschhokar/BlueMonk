package edu.buffalo.rms.bluemountain.databaseshim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Ajay Partap Singh Chhokar
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This class contain specific implementation of SQLiteDatabase class based on splitting tables within the sqLiteDB into multiple table files
 */
public class BmMultiDbSQLiteDatabase extends BmSQLiteDatabase {
    private final String TAG = BmUtilsSingleton.GLOBAL_TAG + BmMultiDbSQLiteDatabase.class.getSimpleName();

    private SQLiteDatabase sqLiteDB;
    private String dbFileName;
    private Context relatedContext;
    private BmSQLiteOpenHelper myDBHelper;
    private boolean isAttached = false;
    private final String FOREIGN_KEY = "foreignkey";
    private final String DATABASE_PATH = "/data/data/edu.buffalo.rms.bluemountain.localapp/databases/";
    private AtomicInteger atomicInteger;
    private ArrayBlockingQueue<BmUtilsSingleton.TestLatency> sharedQ;
    private UploadDbTableFileTask detectAndUploadDbTableFileTask;
    private boolean flag;

    public BmMultiDbSQLiteDatabase(SQLiteDatabase db, Context relatedContext, String dbFileName) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "constructor() called with Database Name : " + dbFileName);
        }

        this.sqLiteDB = db;
        this.relatedContext = relatedContext;
        this.dbFileName = dbFileName;
        this.flag = true;

        init();
    }

    @Override
    public void onCreate() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called");
        }
    }

    @Override
    public void init() {
        this.flag = true;

        if (null != atomicInteger) {
            atomicInteger.set(0);
        } else {
            atomicInteger = new AtomicInteger(0);
        }

        if (null == sharedQ) {
            sharedQ = new ArrayBlockingQueue<>(100, true);
        } else {
            sharedQ.clear();
        }

        if (null == detectAndUploadDbTableFileTask) {
            detectAndUploadDbTableFileTask = new UploadDbTableFileTask();
            detectAndUploadDbTableFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void clear() {
        // Avoiding Memory leaks and making objects eligible for Garbage Collection
        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyDummyObject();
        testObject.cancelTask = true;

        updateQueue(testObject);

        if (null != detectAndUploadDbTableFileTask) {
            detectAndUploadDbTableFileTask.cancel(false);
            detectAndUploadDbTableFileTask = null;
        }

        flag = false;

        sqLiteDB = null;
        myDBHelper = null;

        relatedContext = null;
        atomicInteger = null;
        sharedQ = null;
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
    public BmMultiDbSQLiteDatabase getWritableDatabase(SQLiteDatabase db) {
        // setting writable database
        this.sqLiteDB = db;
        return this;
    }

    //do some operation
    @Override
    public long insert(String databaseTable, String nullColumnCheck, ContentValues initialValues) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "insert() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.MULTI_DB, BmUtilsSingleton.Operation.INSERT, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        testObject.dbTableFileName = databaseTable;

        long result = sqLiteDB.insert(databaseTable, nullColumnCheck, initialValues);

        // Update the Queue
        // This will upload DB Table File to server asynchronously
        updateQueue(testObject);

        return result;
    }

    @Override
    public int delete(String databaseTable, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "delete() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.MULTI_DB, BmUtilsSingleton.Operation.DELETE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        testObject.dbTableFileName = databaseTable;

        int val = sqLiteDB.delete(databaseTable, whereClause, whereArgs);

        // Update the Queue
        // This will upload DB Table File to server asynchronously
        updateQueue(testObject);

        return val;
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "rawQuery() called with sql : " + sql);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.MULTI_DB, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        Cursor cur = sqLiteDB.rawQuery(sql, selectionArgs);

        //framework execution time
        testObject.recordWithFrameworkExecutionTime();

        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return cur;
    }

    @Override
    public Cursor query(boolean distinct, String databaseTable, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "query() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.MULTI_DB, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        Cursor cur = sqLiteDB.query(distinct, databaseTable, columns, selection, selectionArgs, groupBy, having, orderBy, limit);

        //framework execution time
        testObject.recordWithFrameworkExecutionTime();

        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        return cur;
    }

    @Override
    public int update(String databaseTable, ContentValues newValues, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "update() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.MULTI_DB, BmUtilsSingleton.Operation.UPDATE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        testObject.dbTableFileName = databaseTable;

        int val = sqLiteDB.update(databaseTable, newValues, whereClause, whereArgs);

        // Update the Queue
        // This will upload DB Table File to server asynchronously
        updateQueue(testObject);

        return val;
    }

    //after new database is created then attach it to previous one
    @Override
    public void execSQL(String sql) throws SQLException {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "execSQL() called with sql : " + sql);
        }

        String newSQL = "";

        if (null != sql && !sql.contains("create table")) {
            sqLiteDB.execSQL(sql);
            return;
        }

        //check for create table
        if (sql.contains("create table") || sql.contains("CREATE TABLE")) {
            newSQL = getNewDatabasePath(sql);
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "execSQL() newSQL : " + newSQL);
        }

        //execute the query
        sqLiteDB.execSQL(newSQL);
        attachAllDatabase();
    }

    @Override
    public void onUpgrade(int oldVersion, int newVersion) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onUpgrade() called with oldVersion : " + oldVersion + ", newVersion : " + newVersion);
        }
    }

    //////////////////////////// Multi DB Framework Implementation  ////////////////////////////////

    private void updateCounter() {
        final int operationCount = atomicInteger.incrementAndGet();

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "Produced : " + operationCount);
        }
    }

    private void updateQueue(BmUtilsSingleton.TestLatency testObject) {
        try {
            sharedQ.put(testObject);
            updateCounter();
        } catch (InterruptedException e) {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            //framework execution time
            testObject.recordWithFrameworkExecutionTime();
        }
    }

    public String getNewDatabasePath(String sql) {
        String newSQL = "";

        //check for foreign key
        if (sql.contains("foreign key") || sql.contains("FOREIGN KEY")) {
            openPreviousDatabaseInstance(sql);
            newSQL = getWithoutForeignKeyQuery(sql);

        } else if (sql.contains("create table") || sql.contains("CREATE TABLE")) {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "sql command is " + sqLiteDB.getPath() + " - " + sqLiteDB.toString());
            }

            changeDataBaseInstance(sql);
            return sql;
        }

        return newSQL;
    }

    // method basically closes the connection of previous Db and creates a new one
    public void changeDataBaseInstance(String sql) {
        //String[] databaseSplit = sqLiteDatabase.getPath().split("/");
        String temp = new String(sql);
        String[] databaseTableSplit = temp.split(" ");

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "SPLIT --" + databaseTableSplit[2]);
        }

        //close connection to previous database.
        if (myDBHelper != null) {
            myDBHelper.close();
        }

        sqLiteDB.close();

        //create new database instance
        myDBHelper = new BmSQLiteOpenHelper(relatedContext, databaseTableSplit[2] + ".db", null, 2);
        sqLiteDB = null;
        sqLiteDB = myDBHelper.getWritableDatabase();
    }

    // method basically opens the connection of previous Db
    public void openPreviousDatabaseInstance(String sql) {
        //String[] databaseSplit = sqLiteDatabase.getPath().split("/");
        String temp = new String(sql);
        String[] databaseTableSplit = temp.split(" ");

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "SPLIT --" + databaseTableSplit[2]);
        }

        //close connection to previous database.
        sqLiteDB.close();

        String path = sqLiteDB.getPath().substring(0, sqLiteDB.getPath().length() - 5);
        String newPath = path + getParentForeignKeyQuery(sql) + ".db";

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "path is --> " + path);
        }

        sqLiteDB = SQLiteDatabase.openDatabase(newPath, null, 0);
    }

    // returns query without foreign key
    public String getWithoutForeignKeyQuery(String query) {
        int index = query.indexOf("FOREIGN KEY");
        //storeRefTableForeignKey(query);
        String newQuery = query.substring(0, index - 2) + ");";
        return newQuery;
    }

    // returns parent table
    public String getParentForeignKeyQuery(String query) {
        String temp = new String(query);
        String[] databaseTableSplit = temp.split(" ");

        for (int i = 0; i < databaseTableSplit.length; i++) {
            Log.v(TAG, databaseTableSplit[i] + " " + i);
        }

        return databaseTableSplit[29];
    }

    // method used to attach all the databases
    public void attachAllDatabase() {
        File file = new File(DATABASE_PATH);
        String[] myFiles;
        myFiles = file.list();

        // return if only one database is present
        if (myFiles.length <= 2)
            return;

        // loop through all the database and attach except the current one
        for (int i = 0; i < myFiles.length - 2; i++) {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "files are " + myFiles[i]);
            }

            if (myFiles[i].toString().contains("journal")) {
                continue;
            }

            String DatabasePath = DATABASE_PATH + myFiles[i];
            String database = "random" + i;
            sqLiteDB.execSQL("ATTACH DATABASE '" + DatabasePath + "' AS " + database + "");

            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "attachAllDatabase: once done");
            }
        }
    }

    private class UploadDbTableFileTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "********** UploadDbTableFileTask() doInBackground **********");
            }

            BmUtilsSingleton.TestLatency testObject;

            try {
                while (flag) {
                    if (isCancelled()) {
                        break;
                    }

                    testObject = sharedQ.take();
                    if (testObject.cancelTask) {
                        break;
                    }

                    // Capturing Remote Start Time for Recording Actual Remote Execution Time
                    testObject.captureRemoteStartTime();

                    if (atomicInteger.get() >= BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE
                            && BmUtilsSingleton.INSTANCE.isNetworkAvailable(relatedContext)) {
                        atomicInteger.set(0);
                        uploadTableDbFileToServerSynchronously(testObject.dbTableFileName, testObject);
                    } else {
                        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);
                    }
                }
            } catch (InterruptedException e) {
                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                return 0;
            }
        }
    }

    public void uploadTableDbFileToServerSynchronously(final String dbFileName, final BmUtilsSingleton.TestLatency testObject) {
        Boolean isTableDbFileUploaded = false;

        //get the file
        String path = DATABASE_PATH + dbFileName + ".db";
        final File file = new File(path);

        // create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part dbTableFileBody =
                MultipartBody.Part.createFormData("dbTableFile", file.getName(), requestFile);

        // add another part within the multipart request
        String descriptionString = "This is DB Table File";
        RequestBody description =
                RequestBody.create(
                        MediaType.parse("multipart/form-data"), descriptionString);


        try {
            // finally, execute the request
            Call<Boolean> call = BmUtilsSingleton.INSTANCE.getNetworkAPI().uploadDbTableFileToServer(description, dbTableFileBody);
            isTableDbFileUploaded = call.execute().body();

            if (null == isTableDbFileUploaded) {
                isTableDbFileUploaded = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            isTableDbFileUploaded = false;
        } finally {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "uploadTableDbFileToServerSynchronously() : onResponse() isTableDbFileUploaded : " + isTableDbFileUploaded);
            }

            // Recording Native + Framework + Remote Execution Time with Waiting
            testObject.recordWithRemoteExecutionTime(isTableDbFileUploaded);

            // Recording Actual Remote Execution Time
            testObject.recordActualRemoteExecutionTime();

            BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);

        }
    }

/*
    //TODO - need to define this method properly
    public void storeRefTableForeignKey(String query){
        String temp = new String(query);
        String[] databaseTableSplit = temp.split(" ");
        for(int i=0;i<databaseTableSplit.length;i++){
            Log.v(TAG,databaseTableSplit[i]+" "+i);
        }

        //parent and child table
        String parentTable = databaseTableSplit[29];
        String childTable = databaseTableSplit[2];

        //parent and child column
        String parentColumn = databaseTableSplit[31];
        String childColumn = databaseTableSplit[26];

        SharedPreferences.Editor editor = relatedContext.getSharedPreferences(MY_PREF, relatedContext.MODE_PRIVATE).edit();
        editor.putString(childTable,parentTable+"-"+childTable+":"+parentColumn+"-"+childColumn);
        editor.putString(parentTable, parentTable + "-" + childTable + ":" + parentColumn + "-" + childColumn);
        editor.commit();
    }

    //TODO - need to define this method properly
    public String checkForForeignKey(String table){
        SharedPreferences prefs = relatedContext.getSharedPreferences(MY_PREF, relatedContext.MODE_PRIVATE);
        String restoredText = prefs.getString(FOREIGN_KEY, null);
        if (restoredText != null) {
           return  restoredText;
        }
        return "";
    }

   // not completed due to time constraint
    public boolean checkIfValueExistsInParentTable(String foreign, String ParentTable, String ChildTable){
        Cursor cursor = sqLiteDatabase.rawQuery("select * from "+ ForeignKeyUtility.geChildTable(foreign) + " where name='"+ ForeignKeyUtility.geChildColumn(foreign) + "'",  null);
        return false;
    }

    */

}
