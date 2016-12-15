package edu.buffalo.rms.bluemountain.databaseshim;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import edu.buffalo.rms.bluemountain.oplog.BmOpLogSQLiteDatabase;

/**
 * @author Aniruddh Adkar
 * @author Ajay Partap Singh Chhokar
 * @author Ramanpreet Singh Khinda
 *         <p>
 *         Created by raman on 11/20/16.
 *         <p>
 *         This class acts as an abstraction layer of SQLiteOpenHelper class.
 *         The user's SQLiteOpenHelper will be replaced by BmSQLiteOpenHelper to provide BlueMountain specific SQLite implementation
 */
public class BmSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = BmUtilsSingleton.GLOBAL_TAG + BmSQLiteOpenHelper.class.getSimpleName();

    private BmSQLiteDatabase bmSQLiteDatabase = null;

    private String dbFileName;
    private Context relatedContext = null;

    public BmSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "constructor() called with Database Name : " + name + " , Version : " + version);
        }

        this.relatedContext = context;
        this.dbFileName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called with SQLiteDatabase : " + db);
        }

        bmSQLiteDatabase = getBmSQLiteDatabaseWithSyncType(BmUtilsSingleton.INSTANCE.sync_type, db, relatedContext, dbFileName);
        onCreate(bmSQLiteDatabase);
    }

    @Override
    public void close() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "close() called");
        }

        onCreate(bmSQLiteDatabase);
    }

    public void onCreate(BmSQLiteDatabase bmDB) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called with BmSQLiteDatabase : " + bmDB);
        }

        bmSQLiteDatabase.onCreate();
    }

    public BmSQLiteDatabase getBmWritableDatabase() {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "getBmWritableDatabase() called");
        }

        SQLiteDatabase sqLiteDB = getWritableDatabase();
        bmSQLiteDatabase = getBmSQLiteDatabaseWithSyncType(BmUtilsSingleton.INSTANCE.sync_type, sqLiteDB, relatedContext, dbFileName);

        return bmSQLiteDatabase.getWritableDatabase(sqLiteDB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onUpgrade() called with oldVersion : " + oldVersion + ", newVersion : " + newVersion);
        }

        if (null == bmSQLiteDatabase) {
            bmSQLiteDatabase = getBmSQLiteDatabaseWithSyncType(BmUtilsSingleton.INSTANCE.sync_type, db, relatedContext, dbFileName);
        }

        onUpgrade(bmSQLiteDatabase, oldVersion, newVersion);
    }

    public void onUpgrade(BmSQLiteDatabase bmDB, int oldVersion, int newVersion) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onUpgrade() called with oldVersion : " + oldVersion + ", newVersion : " + newVersion);
        }

        bmSQLiteDatabase.onUpgrade(oldVersion, newVersion);
    }

    private BmSQLiteDatabase getBmSQLiteDatabaseWithSyncType(BmUtilsSingleton.SYNC_TYPE sync_strategy, SQLiteDatabase db, Context relatedContext, String dbFileName) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "SYNC_TYPE : " + sync_strategy);
        }

        switch (sync_strategy) {
            case NATIVE:
                return new BmSQLiteDatabase(db, relatedContext, dbFileName);

            case OP_LOG:
                return new BmOpLogSQLiteDatabase(db, relatedContext, dbFileName);
//                return new BmSQLiteQueryTranslator(db,relatedContext,dbFileName);

            case MULTI_DB:
                return new BmMultiDbSQLiteDatabase(db, relatedContext, dbFileName);

            case FILE_CHUNK:
                return new BmFileChunkSQLiteDatabase(db, relatedContext, dbFileName);

            default:
                return new BmOpLogSQLiteDatabase(db, relatedContext, dbFileName);
        }
    }
}

