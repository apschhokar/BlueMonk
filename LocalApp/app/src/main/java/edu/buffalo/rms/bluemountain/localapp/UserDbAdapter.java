package edu.buffalo.rms.bluemountain.localapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import edu.buffalo.rms.bluemountain.databaseshim.BmSQLiteDatabase;
import edu.buffalo.rms.bluemountain.databaseshim.BmSQLiteOpenHelper;
import edu.buffalo.rms.bluemountain.databaseshim.BmUtilsSingleton;

/**
 * @author Aniruddh Adkar
 * @author Ajay Partap Singh Chhokar
 * @author Ramanpreet Singh Khinda
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This is a client or user class to test the BlueMountain SQLiteDatabase approaches
 *         <p/>
 *         The Pluggable BlueMountain system will replace 2 classes to provide access of BlueMountain specific SQLiteDatabase implementation.
 *         Specifically there will be only these 3 changes in the user's DbAdapter class
 *         <p/>
 *         BlueMountain Change 1: Replace SQLiteDatabase with BmSQLiteDatabase
 *         BlueMountain Change 2: Replace getWritableDatabase() with getBmWritableDatabase()
 *         BlueMountain Change 3: Replace SQLiteOpenHelper with BmSQLiteOpenHelper
 */
public class UserDbAdapter {
    private static final String TAG = BmUtilsSingleton.GLOBAL_TAG + UserDbAdapter.class.getSimpleName();

    public static final String KEY_ROW_ID = "_id";

    public static final String KEY_NAME = "name";
    public static final String KEY_AGE = "age";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_ABOUT_ME = "about";

    public static final String[] ROW_KEY = new String[]{KEY_ROW_ID};
    public static final String[] ALL_KEYS = new String[]{KEY_ROW_ID, KEY_NAME, KEY_AGE, KEY_HEIGHT, KEY_ABOUT_ME};

    public static final String DATABASE_NAME = "bm_droids_db_local";
    public static final String DATABASE_TABLE_NAME = "mainTable";

    // Track DB version if a new version of your app changes the format.
    public static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE_SQL =
            "create table " + DATABASE_TABLE_NAME + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_NAME + " text not null, "
                    + KEY_AGE + " integer not null, "
                    + KEY_HEIGHT + " double not null, "
                    + KEY_ABOUT_ME + " text not null "
                    + ");";

    private final Context context;
    private UserDbHelper userDbHelper;

    // Change 1: BlueMountain Pluggable System will replace SQLiteDatabase with BmSQLiteDatabase
    private BmSQLiteDatabase bmDB;

    public UserDbAdapter(Context context) {
        this.context = context;
        userDbHelper = new UserDbHelper(context);
    }

    /**
     * For any initialization task to be performed on different approaches
     */
    public void init() {
        bmDB.init();
    }

    /**
     * For Avoiding Memory leaks and making objects eligible for Garbage Collection
     */
    public void clear() {
        bmDB.clear();
    }

    /**
     * BlueMountain Change 2: Since SQLiteDatabase is a final class we need to define our own
     * getBmWritableDatabase() method in the BmSQLiteOpenHelper class which will provide user
     * with BmSQLiteDatabase class which acts as a wrapper for SQLiteDatabase class
     */
    public UserDbAdapter openUserDB() {
        bmDB = userDbHelper.getBmWritableDatabase();

        return this;
    }

    public void closeUserDB() {
        userDbHelper.close();
    }

    /**
     * helper method to get the total number of tables within a database
     */
    private int getNumOfTables() {
        Cursor cursor = bmDB.rawQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name != 'android_metadata' AND name != 'sqlite_sequence';", null);
        int numOfTables = 0;

        if (null != cursor) {
            cursor.moveToFirst();
            numOfTables = cursor.getInt(0);
        }

        cursor.close();

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "numOfTables : " + numOfTables);
        }

        return numOfTables;
    }

    /**
     * helper method to collect the required row ids in an array and return to the calling function.
     * Even though our row ids are incremental integers we still can't rely on
     * directly accessing integer rows using integer row ids for modification.
     * <p/>
     * This is because our random delete operation can delete a row from any position
     * and this may result in deletion of some of the row ids in the order
     * <p/>
     * For e.g: Before Random Deletion Row ids may look like - 1, 2, 3, 4, 5 , 7.
     * and After Random Deletion Row ids may look like - 1, 2, 3, 5 , 7.
     */
    public long[] getRowIds(String dbTableName, int requiredRows) {
        Cursor cursor = getAllRowsIds(dbTableName);
        long numOfEntries[] = new long[1];

        try {
            if (null != cursor) {
                cursor.moveToFirst();

                int size = requiredRows < cursor.getCount() ? requiredRows : cursor.getCount();
                numOfEntries = new long[size];
                int curIndex = 0;

                do {
                    long rowId = cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID));
                    numOfEntries[curIndex++] = rowId;

                    cursor.moveToNext();
                } while (curIndex < size);

            } else {
                numOfEntries[0] = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        return numOfEntries;
    }

    /**
     * helper method to create user defined number of tables within the database
     */
    public boolean createMultiTableFiles(final int numOfTables) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** createMultiTableFiles() Start. numOfTables : " + numOfTables);
        }

        for (int num = 0; num < numOfTables; num++) {
            bmDB.execSQL("create table " + DATABASE_TABLE_NAME + num + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_NAME + " text not null, "
                    + KEY_AGE + " integer not null, "
                    + KEY_HEIGHT + " double not null, "
                    + KEY_ABOUT_ME + " text not null "
                    + ");");
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** createMultiTableFiles() End *****");
        }

        return true;
    }

    /**
     * helper method to perform multiple operations in the order {UPDATE, QUERY, INSERT, DELETE}
     * on the database with size <'numOfTables','numOfEntriesPerTable'>
     */
    public boolean performMultiOperationsOnTableFiles(final int numOfTables, final int numOfEntriesPerTable) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** performMultiOperationsOnTableFiles() Start. numOfTables : " + numOfTables + " , numOfEntriesPerTable : " + numOfEntriesPerTable + " *****");
        }

        Random ranAge = new Random();
        Random ranHeight = new Random();

        for (int num = 0; num < numOfTables; num++) {
            String currentDbTableName = DATABASE_TABLE_NAME + num;
            int entriesCounter = numOfEntriesPerTable;

            for (long rowId : getRowIds(currentDbTableName, numOfEntriesPerTable)) {
                try {
                    // UPDATE
                    updateRow(currentDbTableName, rowId, getSaltString(), ranAge.nextInt(50), ranHeight.nextInt(20), getSaltString());
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // QUERY
                    getRow(currentDbTableName, rowId);
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // INSERT
                    insertRow(currentDbTableName, getSaltString(), ranAge.nextInt(50), ranHeight.nextInt(20), getSaltString());
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // DELETE
                    deleteRow(currentDbTableName, rowId);
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (--entriesCounter <= 0) {
                    break;
                }
            }
        }


        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** insertMultiEntriesIntoTableFiles() End *****");
        }

        return true;
    }


    /**
     * helper method to insert 'numOfEntriesPerTable' entries with random data in the requested 'numOfTables' tables
     */
    public boolean insertMultiEntriesIntoTableFiles(final int numOfTables, final int numOfEntriesPerTable) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** insertMultiEntriesIntoTableFiles() Start. numOfTables : " + numOfTables + " , numOfEntriesPerTable : " + numOfEntriesPerTable + " *****");
        }


        Random ranAge = new Random();
        Random ranHeight = new Random();

        for (int num = 0; num < numOfTables; num++) {
            String currentDbTableName = DATABASE_TABLE_NAME + num;

            for (int entry = 0; entry < numOfEntriesPerTable; entry++) {
                insertRow(currentDbTableName, getSaltString(), ranAge.nextInt(50), ranHeight.nextInt(20), getSaltString());
            }
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** insertMultiEntriesIntoTableFiles() End *****");
        }

        return true;
    }

    /**
     * helper method to insert 'numOfEntriesPerTable' entries with random data in the requested 'numOfTables' tables
     */
    public boolean queryMultiEntriesFromTableFiles(final int numOfTables, final int numOfEntriesPerTable) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** queryMultiEntriesFromTableFiles() Start. numOfTables : " + numOfTables + " , numOfEntriesPerTable : " + numOfEntriesPerTable + " *****");
        }

        for (int num = 0; num < numOfTables; num++) {
            String currentDbTableName = DATABASE_TABLE_NAME + num;
            int entriesCounter = numOfEntriesPerTable;

            for (long rowId : getRowIds(currentDbTableName, numOfEntriesPerTable)) {
                getRow(currentDbTableName, rowId);

                if (--entriesCounter <= 0) {
                    break;
                }
            }
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** queryMultiEntriesFromTableFiles() End *****");
        }

        return true;
    }


    /**
     * helper method to update 'numOfEntriesPerTable' entries with random data in the requested 'numOfTables' tables
     */
    public boolean updateMultiEntriesOfTableFiles(final int numOfTables, final int numOfEntriesPerTable) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** updateMultiEntriesOfTableFiles() Start. numOfTables : " + numOfTables + " , numOfEntriesPerTable : " + numOfEntriesPerTable + " *****");
        }

        Random ranAge = new Random();
        Random ranHeight = new Random();

        for (int num = 0; num < numOfTables; num++) {
            String currentDbTableName = DATABASE_TABLE_NAME + num;
            int entriesCounter = numOfEntriesPerTable;

            for (long rowId : getRowIds(currentDbTableName, numOfEntriesPerTable)) {
                updateRow(currentDbTableName, rowId, getSaltString(), ranAge.nextInt(50), ranHeight.nextInt(20), getSaltString());

                if (--entriesCounter <= 0) {
                    break;
                }
            }
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** updateMultiEntriesOfTableFiles() End *****");
        }

        return true;
    }

    /**
     * helper method to delete 'numOfEntriesPerTable' entries from the requested 'numOfTables' tables
     */
    public boolean deleteMultiEntriesFromTableFiles(final int numOfTables, final int numOfEntriesPerTable) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** deleteMultiEntriesFromTableFiles() Start. numOfTables : " + numOfTables + " , numOfEntriesPerTable : " + numOfEntriesPerTable + " *****");
        }

        for (int num = 0; num < numOfTables; num++) {
            String currentDbTableName = DATABASE_TABLE_NAME + num;
            int entriesCounter = numOfEntriesPerTable;

            for (long rowId : getRowIds(currentDbTableName, numOfEntriesPerTable)) {
                deleteRow(currentDbTableName, rowId);

                if (--entriesCounter <= 0) {
                    break;
                }
            }
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** updateMultiEntriesOfTableFiles() End *****");
        }

        return true;
    }


    /**
     * helper method to delete all record from all the tables
     */
    public boolean deleteAllEntriesFromTableFiles() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** deleteAllEntriesFromTableFiles() Start. *****");
        }

        int numOfTables = getNumOfTables();

        for (int num = 0; num < numOfTables; num++) {
            deleteAllRows(DATABASE_TABLE_NAME + num);
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** deleteAllEntriesFromTableFiles() End *****");
        }

        return true;
    }

    /**
     * helper method to drop all tables
     */
    public int deleteAllTablesAndFiles() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** deleteAllTablesAndFiles() Start *****");
        }

        int numOfTables = getNumOfTables();

        for (int num = 0; num < numOfTables; num++) {
            bmDB.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_NAME + num);
        }

        deleteTableDbFiles();

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "***** deleteAllTablesAndFiles End *****");
        }

        return numOfTables;
    }

    public void deleteTableDbFiles() {
        File file = new File("/data/data/edu.buffalo.rms.bluemountain.localapp/databases/");
        String[] myFiles;

        myFiles = file.list();
        if (myFiles == null) return;

        for (int i = 0; i < myFiles.length; i++) {
            File myFile = new File(file, myFiles[i]);
            if (myFile.getName().contains(DATABASE_NAME)) {
                continue;
            }

            myFile.delete();
        }
    }

    /**
     * helper method to generate random salt string
     */
    private String getSaltString() {
        String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();

        while (salt.length() < 18) {
            int index = (int) (rnd.nextFloat() * SALT_CHARS.length());
            salt.append(SALT_CHARS.charAt(index));
        }

        return salt.toString();
    }

    public long insertRow(String dbTableName, String name, int age, double height, String aboutMe) {
        // Create row's data:
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_AGE, age);
        initialValues.put(KEY_HEIGHT, height);
        initialValues.put(KEY_ABOUT_ME, aboutMe);

        // Insert it into the BlueMountain database.
        return bmDB.insert(dbTableName, null, initialValues);
    }

    /**
     * Delete a row from the local database, by rowId (primary key)
     */
    public boolean deleteRow(String dbTableName, long rowId) {
        String where = KEY_ROW_ID + "=" + rowId;
        return bmDB.delete(dbTableName, where, null) != 0;
    }

    /**
     * Delete all rows from the local database
     */
    public void deleteAllRows(String dbTableName) {
        Cursor c = getAllRowsIds(dbTableName);
        if (c.moveToFirst()) {
            do {
                long rowId = c.getColumnIndexOrThrow(KEY_ROW_ID);
                deleteRow(dbTableName, c.getLong((int) rowId));
            } while (c.moveToNext());
        }

        c.close();
    }

    /**
     * Get a specific row (by rowId) from the local database
     */
    public Cursor getRow(String dbTableName, long rowId) {
        String where = KEY_ROW_ID + "=" + rowId;
        Cursor c = bmDB.query(true, dbTableName, ALL_KEYS,
                where, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }

        //todo use this cursor to get result
        c.close();

        return c;
    }

    /**
     * Return all rows from the local database
     */
    public Cursor getAllRowsIds(String dbTableName) {
        String where = null;
        Cursor c = bmDB.query(true, dbTableName, ROW_KEY,
                where, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /**
     * Change an existing row to be equal to new data in the local database
     */
    public boolean updateRow(String dbTableName, long rowId, String name, int age, double height, String aboutMe) {
        String where = KEY_ROW_ID + "=" + rowId;

        // Create row's data:
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_NAME, name);
        newValues.put(KEY_AGE, age);
        newValues.put(KEY_HEIGHT, height);
        newValues.put(KEY_ABOUT_ME, aboutMe);

        // Insert it into the database.
        return bmDB.update(dbTableName, newValues, where, null) != 0;
    }

    public String rawQuery(String query) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "Inside rawQuery");
        }

        Cursor c = bmDB.rawQuery(query, null);
        String result = "";

        int columnCount = c.getColumnCount();
        int totalRows = c.getCount();
        for (int count = 0; count < columnCount; ++count) {
            String colName = c.getColumnName(count);
            result += colName + ", ";
        }
        result += "\n";

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            for (int count = 0; count < columnCount; ++count) {
                String val = c.getString(count);
                result += val + ",";
            }
            result += "\n";

        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "RawQuery result" + result);
        }

        c.close();
        if (columnCount == 0 && totalRows == 0) {
            result = "No data returned";
        }

        return result;
    }


    /**
     * Private class which handles database creation and upgrading.
     * Used to handle low-level database access.
     * <p/>
     * BlueMountain Change 3: BlueMountain Pluggable System will replace SQLiteOpenHelper with BmSQLiteOpenHelper
     */
    private static class UserDbHelper extends BmSQLiteOpenHelper {
        UserDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(BmSQLiteDatabase bmDB) {
            super.onCreate(bmDB);

            /**
             * Below line is commented just for the sake of creating multiple tables
             * as defined in the createMultiTableFiles() method above.
             *
             */
            //    db.execSQL(DATABASE_CREATE_SQL);
        }

        @Override
        public void onUpgrade(BmSQLiteDatabase bmDB, int oldVersion, int newVersion) {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "Upgrading application's database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data!");
            }

            // Destroy old database:
            bmDB.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);

            // Recreate new database:
            onCreate(bmDB);
        }
    }
}
