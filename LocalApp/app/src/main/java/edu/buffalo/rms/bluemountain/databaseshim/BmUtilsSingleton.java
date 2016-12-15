package edu.buffalo.rms.bluemountain.databaseshim;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Aniruddh Adkar
 * @author Ajay Partap Singh Chhokar
 * @author Ramanpreet Singh Khinda
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This class acts as providing access to common fields and functions
 */
public enum BmUtilsSingleton {
    INSTANCE;

    public static final String GLOBAL_TAG = "DROIDS_";
    public static final String TAG = GLOBAL_TAG + BmUtilsSingleton.class.getSimpleName();

    public static final boolean DEBUG = true;

    // Local Server URL for Genymotion : "http://10.0.3.2:8000"
    public static final boolean isEmulatorGenymotion = false;
    public static final String GENYMOTION_EMULATOR_LOCAL_IP = "http://10.0.3.2:8000";
    public static final String EMULATOR_LOCAL_IP = "http://localhost:8000";

    // Local System IP
    public static final boolean directLocalServerHit = true;
    public static final String SYSTEM_LOCAL_IP = "http://192.168.0.12:8000";

    // For now we don't have production server so both urls are same
    public static final String PRODUCTION_URL = directLocalServerHit ? SYSTEM_LOCAL_IP : isEmulatorGenymotion ? GENYMOTION_EMULATOR_LOCAL_IP : EMULATOR_LOCAL_IP;
    public static final String LOCAL_URL = directLocalServerHit ? SYSTEM_LOCAL_IP : isEmulatorGenymotion ? GENYMOTION_EMULATOR_LOCAL_IP : EMULATOR_LOCAL_IP;
    public static final String BASE_URL = (DEBUG == true) ? LOCAL_URL : PRODUCTION_URL;

    public enum SYNC_TYPE {
        NATIVE, OP_LOG, FILE_CHUNK, MULTI_DB;
    }

    public enum Operation {
        UPDATE(4), QUERY(5), INSERT(6), DELETE(7), DELETE_ALL(8), EXEC_SQL(7),;
        private final int value;

        private Operation(int value) {
            this.value = value;
        }

        public int getIntValue() {
            return value;
        }

    }

    // Default SYNC_TYPE is NATIVE
    public SYNC_TYPE sync_type = SYNC_TYPE.NATIVE;

    // Default Chunk size is 4 KB
    public static final int FILE_CHUNK_SIZE = 1024 * 4;

    public static boolean isNetworkAvailable(final Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Declaring this in Singleton class will keep the activities from holding references to the network api avoiding memory leaks
     */
    public static IBmNetworkAPI getNetworkAPI() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

        // set your desired log level
        if (DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        // add your other interceptors …

        // add logging as last interceptor
        httpClient.addInterceptor(logging);  // <-- this is the important line!

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();

        // Create an instance of our IBmNetworkAPI interface.
        return retrofit.create(IBmNetworkAPI.class);
    }

    ////////////// Benchmarking and Testing Framework for Multithreading Testing  //////////////////
    public class TestThroughput {
        SYNC_TYPE testSyncType;
        Operation testOperation;
        int testOperationQueueSize, testNumOfTables, testNumOfOperations;
        long startTime, operationsTotalExecutionTime;

        public TestThroughput(SYNC_TYPE testSyncType, Operation testOperation, int testOperationQueueSize, int testNumOfTables, int testNumOfOperations) {
            this.testSyncType = testSyncType;
            this.testOperation = testOperation;
            this.testOperationQueueSize = testOperationQueueSize;
            this.testNumOfTables = testNumOfTables;
            this.testNumOfOperations = testNumOfOperations;

            // Record start time
            this.startTime = System.nanoTime();
        }

        // Record total Execution Time for running the specified number of operations
        public void recordOperationsExecutionTime() {
            this.operationsTotalExecutionTime = System.nanoTime() - startTime;
        }
    }

    public class TestLatency {
        SYNC_TYPE testSyncType;
        Operation testOperation;
        String dbTableFileName;
        int testOperationQueueSize;
        long startTime, nativeExecutionTimeWithWaiting, frameworkExecutionTimeWithWaiting, remoteExecutionTimeWithWaiting;
        long frameworkOverheadWithWaiting, remoteOverheadWithWaiting;
        long startFrameworkTime, startRemoteTime, actualFrameworkExecutionTime, actualRemoteExecutionTime;

        boolean isRemoteUploadSuccess;
        boolean cancelTask;

        public TestLatency() {

        }

        public TestLatency(SYNC_TYPE testSyncType, Operation testOperation, int testOperationQueueSize) {
            this.testSyncType = testSyncType;
            this.testOperation = testOperation;
            this.testOperationQueueSize = testOperationQueueSize;

            // Record start time
            this.startTime = System.nanoTime();
        }

        public void recordNativeExecutionTime() {
            // Just Native Execution Time with Waiting
            this.nativeExecutionTimeWithWaiting = System.nanoTime() - startTime;
        }

        public void recordWithFrameworkExecutionTime() {
            // Native + Framework Execution Time with Waiting
            this.frameworkExecutionTimeWithWaiting = System.nanoTime() - startTime;

            // Framework Overhead Time with Waiting
            this.frameworkOverheadWithWaiting = frameworkExecutionTimeWithWaiting - nativeExecutionTimeWithWaiting;
        }

        public void recordWithRemoteExecutionTime(boolean isRemoteUploadSuccess) {
            this.isRemoteUploadSuccess = isRemoteUploadSuccess;

            // Native + Framework + Server Upload Execution Time with Waiting
            this.remoteExecutionTimeWithWaiting = System.nanoTime() - startTime;

            // Remote Overhead Time with Waiting
            this.remoteOverheadWithWaiting = remoteExecutionTimeWithWaiting - frameworkExecutionTimeWithWaiting;
        }

        public void captureFrameworkStartTime() {
            this.startFrameworkTime = System.nanoTime();
        }

        public void recordActualFrameworkExecutionTime() {
            this.actualFrameworkExecutionTime = System.nanoTime() - startFrameworkTime;
        }

        public void captureRemoteStartTime() {
            this.startRemoteTime = System.nanoTime();
        }

        public void recordActualRemoteExecutionTime() {
            this.actualRemoteExecutionTime = System.nanoTime() - startRemoteTime;
        }
    }

    public boolean isFullSystemLatencyTesting;
    public boolean isFullSystemThroughputTesting;

    private List<Integer> operationQueueSizeList;
    private List<Integer> operationCountList;
    private List<String> tableEntryList;
    private ProgressDialog ringProgressDialog;

    private FileOutputStream latencyDumpFos, throughputDumpFos;

    // After every 'OPERATION_QUEUE_SIZE' number of operations we uploadDbTableFileToServer to remote database
    // Default OPERATION_QUEUE_SIZE is 3
    public int OPERATION_QUEUE_SIZE = 10;
    public boolean canLog = true;

    public TestThroughput getTestThroughputObject(SYNC_TYPE testSyncType, Operation testOperation, int testOperationQueueSize, int testNumOfTables, int testNumOfOperations) {
        return new TestThroughput(testSyncType, testOperation, testOperationQueueSize, testNumOfTables, testNumOfOperations);
    }

    public TestLatency getTestLatencyDummyObject() {
        return new TestLatency();
    }

    public TestLatency getTestLatencyObject(SYNC_TYPE testSyncType, Operation testOperation, int testOperationQueueSize) {
        return new TestLatency(testSyncType, testOperation, testOperationQueueSize);
    }

    public void initTestingFramework() {
        this.latencyDumpFos = initDumpFile("droids_db_latency_testing.txt");
        this.throughputDumpFos = initDumpFile("droids_db_throughput_testing.txt");

        operationQueueSizeList = new ArrayList<>();
        operationQueueSizeList.add(10);
//        operationQueueSizeList.add(10);
//        operationQueueSizeList.add(50);

        operationCountList = new ArrayList<>();
//        operationCountList.add(13); //100 Oprs
//        operationCountList.add(32); //250 Oprs
//        operationCountList.add(63); //500 Oprs
        operationCountList.add(125); // 1000 Oprs


        // Format is "<Num_of_Tables> : <Num_of_Entries_in_each_table>"

        tableEntryList = new ArrayList<>();
        tableEntryList.add("8 : 50");
//        tableEntryList.add("15 : 15");
//        tableEntryList.add("1000 : 1000");
    }

    public void closeDumpFiles() {
        try {
            if (null != latencyDumpFos) {
                latencyDumpFos.close();
            }

            if (null != throughputDumpFos) {
                throughputDumpFos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * After some fixed number of operations specified by 'operationQueueSizeList' we are doing remote uploadDbTableFileToServer
     */
    public List<Integer> getOperationQueueSizeList(SYNC_TYPE syncType) {
        if (SYNC_TYPE.NATIVE == syncType) {
            // for native sync type, operation queue is not required
            List<Integer> dummyList = new ArrayList<>(1);
            dummyList.add(1);
            return dummyList;
        } else {
            return operationQueueSizeList;
        }
    }

    public List<Integer> getOperationCountList() {
        return operationCountList;
    }

    public List<String> getTableEntryList() {
        return tableEntryList;
    }

    private FileOutputStream initDumpFile(String fileName) {
        FileOutputStream fos = null;

        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/BlueMountain");
            dir.mkdirs();

            File file = new File(dir, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(file, true);
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } finally {
            return fos;
        }
    }

    /**
     * This is to dump latency timings
     */
    public void endLatencyTestWithSingleFileDump(TestLatency testObject) {
        try {
            if (isFullSystemLatencyTesting && canLog) {
                String result = "testSyncType: " + testObject.testSyncType
                        + ", testOperation: " + testObject.testOperation
                        + ", testOperationQueueSize: " + testObject.testOperationQueueSize
                        + ", nativeExecutionTimeWithWaiting: " + testObject.nativeExecutionTimeWithWaiting
                        + ", frameworkExecutionTimeWithWaiting: " + testObject.frameworkExecutionTimeWithWaiting
                        + ", remoteExecutionTimeWithWaiting: " + testObject.remoteExecutionTimeWithWaiting
                        + ", frameworkOverheadWithWaiting: " + testObject.frameworkOverheadWithWaiting
                        + ", remoteOverheadWithWaiting: " + testObject.remoteOverheadWithWaiting
                        + ", actualFrameworkExecutionTime: " + testObject.actualFrameworkExecutionTime
                        + ", actualRemoteExecutionTime: " + testObject.actualRemoteExecutionTime
                        + ", isRemoteUploadSuccess: " + testObject.isRemoteUploadSuccess;

                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    Log.v(TAG, result);
                }

                latencyDumpFos.write(result.getBytes("UTF-8"));
                latencyDumpFos.write(System.getProperty("line.separator").getBytes());
            }
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } finally {
            // De-referencing object to avoid memory leak issues
            testObject = null;
        }
    }


    /**
     * This is to dump throughput timings
     */
    public void endThroughputTestWithSingleFileDump(TestThroughput testObject) {
        try {
            if (isFullSystemLatencyTesting && canLog) {
                String result = "testSyncType: " + testObject.testSyncType
                        + ", testOperation: " + testObject.testOperation
                        + ", testOperationQueueSize: " + testObject.testOperationQueueSize
                        + ", testNumOfTables: " + testObject.testNumOfTables
                        + ", testNumOfOperationsPerTable: " + testObject.testNumOfOperations
                        + ", testNumOfOperationsTotal: " + (testObject.testNumOfTables * testObject.testNumOfOperations)
                        + ", operationsTotalExecutionTime: " + testObject.operationsTotalExecutionTime;

                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    Log.v(TAG, result);
                }


                throughputDumpFos.write(result.getBytes("UTF-8"));
                throughputDumpFos.write(System.getProperty("line.separator").getBytes());
            }
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } finally {
            // De-referencing object to avoid memory leak issues
            testObject = null;
        }
    }

    public void launchRingDialog(Context context, String msgTitle, String msgSubtitle) {
        ringProgressDialog = ProgressDialog.show(context, msgTitle, msgSubtitle, true);
        ringProgressDialog.setCancelable(false);
    }

    public void updateProgressDialogMessage(String updateMessage) {
        if (ringProgressDialog != null && ringProgressDialog.isShowing()) {
            ringProgressDialog.setMessage(updateMessage);
        }
    }

    public void dismissProgressDialog() {
        if (ringProgressDialog != null && ringProgressDialog.isShowing()) {
            ringProgressDialog.dismiss();
        }

        ringProgressDialog = null;
    }
}
