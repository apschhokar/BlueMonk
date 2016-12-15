package edu.buffalo.rms.bluemountain.databaseshim;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Ramanpreet Singh Khinda
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This class contain specific implementation of SQLiteDatabase class based on file chunking approach
 */
public class BmFileChunkSQLiteDatabase extends BmSQLiteDatabase {
    private final String TAG = BmUtilsSingleton.GLOBAL_TAG + BmFileChunkSQLiteDatabase.class.getSimpleName();

    private SQLiteDatabase sqLiteDB;
    private Context relatedContext;
    private String dbFileName;

    private MappedByteBuffer buffer;

    private static final String FILE_CHUNK_PREF_NAME = "file_chunk_pref";
    private AtomicInteger atomicInteger;
    private ArrayBlockingQueue<BmUtilsSingleton.TestLatency> sharedQ;
    private DetectAndUploadChunkTask detectAndUploadChunkTask;
    private boolean flag;

    public BmFileChunkSQLiteDatabase(SQLiteDatabase db, Context relatedContext, String dbFileName) {
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
        // make sure we don't have any previous chunk hash
        reset();

        mapDbToMemory();
    }

    /**
     * Avoiding Memory leaks and making objects eligible for Garbage Collection
     */
    @Override
    public void clear() {
        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyDummyObject();
        testObject.cancelTask = true;
        updateQueue(testObject);

        if (null != detectAndUploadChunkTask) {
            detectAndUploadChunkTask.cancel(false);
            detectAndUploadChunkTask = null;
        }

        flag = false;
        sqLiteDB = null;

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
    public BmFileChunkSQLiteDatabase getWritableDatabase(SQLiteDatabase db) {
        // setting writable database
        this.sqLiteDB = db;

        return this;
    }

    @Override
    public long insert(String databaseTable, String nullColumnCheck, ContentValues initialValues) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "insert() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK, BmUtilsSingleton.Operation.INSERT, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);
        long result = sqLiteDB.insert(databaseTable, nullColumnCheck, initialValues);

        // Update the Queue and detect chunks in the background.
        // If a chunk is changed, than uploading will be done asynchronously
        updateQueue(testObject);

        return result;
    }

    @Override
    public int delete(String databaseTable, String whereClause, String[] whereArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "delete() called on databaseTable : " + databaseTable);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK, BmUtilsSingleton.Operation.DELETE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        int result = sqLiteDB.delete(databaseTable, whereClause, whereArgs);

        // Update the Queue and detect chunks in the background.
        // If a chunk is changed, than uploading will be done asynchronously
        updateQueue(testObject);

        return result;
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "rawQuery() called with sql : " + sql);
        }

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

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

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK, BmUtilsSingleton.Operation.QUERY, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

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

        BmUtilsSingleton.TestLatency testObject = BmUtilsSingleton.INSTANCE.getTestLatencyObject(BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK, BmUtilsSingleton.Operation.UPDATE, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE);

        int result = sqLiteDB.update(databaseTable, newValues, whereClause, whereArgs);

        // Update the Queue and detect chunks in the background.
        // If a chunk is changed, than uploading will be done asynchronously
        updateQueue(testObject);

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

        reset();
    }

    //////////////////////////// File Chunking Framework Implementation  ////////////////////////////////

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
            // Recording Native Execution Time
            // This will work only during latency test
            testObject.recordNativeExecutionTime();
        }
    }

    public void reset() {
        this.flag = true;

        if (null != buffer) {
            buffer.reset();
        }

        if (null == sharedQ) {
            sharedQ = new ArrayBlockingQueue<>(200, true);
        } else {
            sharedQ.clear();
        }

        if (null != atomicInteger) {
            atomicInteger.set(0);
        } else {
            atomicInteger = new AtomicInteger(0);
        }

        if (null != relatedContext) {
            final SharedPreferences prefs = getFileChunkSharedPreference(relatedContext);
            prefs.edit().clear().commit();
        }

        if (null == detectAndUploadChunkTask) {
            detectAndUploadChunkTask = new DetectAndUploadChunkTask();
            detectAndUploadChunkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * This method will clear the shared preference (to delete any previous chunk hash)
     * and map the db file to memory
     */
    private void mapDbToMemory() {
        try {
            //Create file object
            File file = new File(getDbPath());

            //Get file channel in readonly mode
            FileChannel fileChannel = null;
            fileChannel = new RandomAccessFile(file, "r").getChannel();

            //Get direct byte buffer access using channel.map() operation
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            // set the mark at the beginning of the buffer
            buffer.mark();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDbPath() {
        return relatedContext.getDatabasePath(dbFileName).toString();
    }

    private class DetectAndUploadChunkTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "********** DetectAndUploadChunkTask() doInBackground **********");
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

                    // Capturing Framework Start Time for Recording Actual Framework Execution Time
                    testObject.captureFrameworkStartTime();

                    if (atomicInteger.get() >= BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE
                            && BmUtilsSingleton.INSTANCE.isNetworkAvailable(relatedContext)) {
                        atomicInteger.set(0);
                        detectAndUploadChunks(testObject);
                    } else {
                        BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);
                    }
                }
            } finally {
                return 0;
            }
        }
    }

    private void detectAndUploadChunks(BmUtilsSingleton.TestLatency testObject) {
        final SharedPreferences prefs = getFileChunkSharedPreference(relatedContext);
        boolean isDirty = false;

        try {
            if (null == buffer) {
                mapDbToMemory();
            }

            buffer.reset();

            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                // the buffer now reads the file as if it were loaded in memory.
                Log.v(TAG, "buffer.isLoaded() : " + buffer.isLoaded());  //Attempts to load every page of this buffer into RAM
                Log.v(TAG, "buffer.capacity() : " + buffer.capacity());  //Get the size based on content size of file
                Log.v(TAG, "buffer.limit() : " + buffer.limit()); //Return the limit of this buffer
            }


            int capacity = buffer.capacity();
            float float_parts = (capacity * 1.0f) / BmUtilsSingleton.INSTANCE.FILE_CHUNK_SIZE;
            int num_parts = capacity / BmUtilsSingleton.INSTANCE.FILE_CHUNK_SIZE;

            if (float_parts - num_parts > 0) {
                ++num_parts;
            }

            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "Total Parts : " + num_parts);
            }

            byte[][] chunk = new byte[num_parts][];

            for (int index = 0; index < num_parts; index++) {
                chunk[index] = new byte[BmUtilsSingleton.INSTANCE.FILE_CHUNK_SIZE];
                // if its the last chunk than capture only remaining buffer
                if (index + 1 == num_parts) {
                    buffer.get(chunk[index], 0, buffer.remaining());
                } else {
                    buffer.get(chunk[index], 0, BmUtilsSingleton.INSTANCE.FILE_CHUNK_SIZE);
                }

                String prevFastHash = prefs.getString("fast:" + index, "");
                String newFastHash = String.valueOf(getFastHash(chunk[index]));

                isDirty = false;

                if ("".equals(prevFastHash) || !prevFastHash.equals(newFastHash)) {
                    isDirty = true;
                    if (BmUtilsSingleton.INSTANCE.DEBUG) {
                        Log.v(TAG, "Chunk " + (index + 1) + " -> Prev Fast Hash : " + prevFastHash + ", New Fast Hash : " + newFastHash);
                    }
                }

                // Recording Native + Framework Execution Time with Waiting
                testObject.recordWithFrameworkExecutionTime();

                // Recording Actual Framework Execution Time
                testObject.recordActualFrameworkExecutionTime();

                // Capturing Remote Start Time for Recording Actual Remote Execution Time
                testObject.captureRemoteStartTime();

                if (isDirty) {
                    int offset = index * BmUtilsSingleton.INSTANCE.FILE_CHUNK_SIZE;

                    uploadFileChunkToServerSynchronously(chunk[index], offset, index, newFastHash, testObject);
                }
            }
        } catch (Exception e) {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            // If there are no changes detected than we can end the test here
            // Otherwise "uploadFileChunkToServerAsynchronously" method will take of ending the test
            if (!isDirty) {
                BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);
            }

            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "********** updateQueueAndDetectChunksInBackground() END **********");
            }
        }
    }

    public void uploadFileChunkToServerSynchronously(final byte[] chunkByteData, final int offset, final int index, final String newFastHash, final BmUtilsSingleton.TestLatency testObject) {
        Boolean isChunkUploaded = false;

        // create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), chunkByteData);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part chunkByteDataBody =
                MultipartBody.Part.createFormData("chunkByteData", dbFileName, requestFile);

        // add another part within the multipart request
        String descriptionString = "This is Chunk Byte Data";
        RequestBody description =
                RequestBody.create(
                        MediaType.parse("multipart/form-data"), descriptionString);

        try {
            Call<Boolean> call = BmUtilsSingleton.INSTANCE.getNetworkAPI().uploadFileChunkToServer(description, chunkByteDataBody, offset);
            isChunkUploaded = call.execute().body();

            if (null == isChunkUploaded) {
                isChunkUploaded = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            isChunkUploaded = false;
        } finally {
            if (BmUtilsSingleton.INSTANCE.DEBUG) {
                Log.v(TAG, "uploadFileChunkToServerSynchronously() : onResponse() isChunkUploaded : " + isChunkUploaded);
            }

            if (isChunkUploaded) {
                updateLocalHashValues(index, newFastHash);
            }

//            // Recording Native + Framework + Remote Execution Time with Waiting
            testObject.recordWithRemoteExecutionTime(isChunkUploaded);
//
//            // Recording Actual Remote Execution Time
            testObject.recordActualRemoteExecutionTime();
//
            BmUtilsSingleton.INSTANCE.endLatencyTestWithSingleFileDump(testObject);
        }
    }

    private void updateLocalHashValues(final int index, final String newFastHash) {
        final SharedPreferences prefs = getFileChunkSharedPreference(relatedContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("fast:" + index, "" + newFastHash);

        /**
         * commit() writes its preferences out to persistent storage synchronously.
         *
         * apply() commits its changes to the in-memory SharedPreferences immediately
         * but starts an asynchronous commit to disk and you won't be notified of any failures.
         */
        editor.commit();
    }

    private SharedPreferences getFileChunkSharedPreference(Context context) {
        return context.getSharedPreferences(FILE_CHUNK_PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * https://github.com/jpountz/lz4-java/tree/master/src/xxhash
     * <p/>
     * 20x times faster than MD5 Hash
     */
    public int getFastHash(final byte[] input) throws IOException {
        XXHashFactory factory = XXHashFactory.fastestInstance();

        ByteArrayInputStream in = new ByteArrayInputStream(input);

        int seed = 0x9747b28c; // used to initialize the hash value, use whatever
        // value you want, but always the same
        StreamingXXHash32 hash32 = factory.newStreamingHash32(seed);
        byte[] buf = new byte[100]; // for real-world usage, use a larger buffer, like 8192 bytes
        for (; ; ) {
            int read = in.read(buf);
            if (read == -1) {
                break;
            }

            hash32.update(buf, 0, read);
        }

        return hash32.getValue();
    }

//    public String getMD5Hash(final byte[] hash) throws NoSuchAlgorithmException {
//        MessageDigest md = MessageDigest.getInstance("MD5");
//        md.update(hash);
//
//        byte[] digest = md.digest();
//        StringBuffer sb = new StringBuffer();
//        for (byte b : digest) {
//            sb.append(String.format("%02x", b & 0xff));
//        }
//
//        return sb.toString();
//    }
}