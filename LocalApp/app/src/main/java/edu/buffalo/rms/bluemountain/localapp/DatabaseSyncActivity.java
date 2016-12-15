package edu.buffalo.rms.bluemountain.localapp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.buffalo.rms.bluemountain.databaseshim.BmUtilsSingleton;
import edu.buffalo.rms.bluemountain.oplog.OpLogsSyncAdapter;

/**
 * @author Ramanpreet Singh Khinda
 *         <p/>
 *         Created by raman on 11/20/16.
 *         <p/>
 *         This class is for testing File Chunking Approach for SQLite database
 */
public class DatabaseSyncActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = BmUtilsSingleton.GLOBAL_TAG + DatabaseSyncActivity.class.getSimpleName();

    @Bind(R.id.etxt_num_tables)
    EditText eTextNumTables;

    @Bind(R.id.etxt_num_entries)
    EditText eTextNumEntries;

    @Bind(R.id.native_radio_btn)
    RadioButton nativeRadioBtn;

    @Bind(R.id.op_log_radio_btn)
    RadioButton opLogRadioBtn;

    @Bind(R.id.multi_db_radio_btn)
    RadioButton multiDbRadioBtn;

    @Bind(R.id.file_chunk_radio_btn)
    RadioButton fileChunkRadioBtn;

    @Bind(R.id.single_opr_radio_btn)
    RadioButton singleOprRadioBtn;

    @Bind(R.id.multi_opr_radio_btn)
    RadioButton multiOprRadioBtn;

    @Bind(R.id.random_opr_radio_btn)
    RadioButton randomOprRadioBtn;

    @Bind(R.id.txt_num_of_tables)
    TextView txtNumOfTables;

    @Bind(R.id.etxt_num_of_tables)
    EditText eTxtNumOfTables;

    @Bind(R.id.txt_num_of_opr)
    TextView txtNumOfOpr;

    @Bind(R.id.etxt_num_of_opr)
    EditText eTxtNumOfOpr;

    @Bind(R.id.insert_radio_btn)
    RadioButton insertRadioBtn;

    @Bind(R.id.update_radio_btn)
    RadioButton updateRadioBtn;

    @Bind(R.id.delete_radio_btn)
    RadioButton deleteRadioBtn;

    @Bind(R.id.delete_all_radio_btn)
    RadioButton deleteAllRadioBtn;

    @Bind(R.id.create_fresh_tables_btn)
    Button createFreshTablesBtn;

    @Bind(R.id.execute_btn)
    Button executeBtn;

    @Bind(R.id.latency_test_btn)
    Button latencyTestBtn;

    @Bind(R.id.throughput_test_btn)
    Button throughputTestBtn;

    private UserDbAdapter userDbAdapter;

    private static final int MAX_RANDOM_VALUE = 100;
    private boolean isSyncTypeChanged = false;
    private Context mContext;

    enum OperationType {
        SINGLE_OPERATION(1), MULTI_OPERATIONS(2), RANDOM_OPERATIONS(3);

        private final int value;

        private OperationType(int value) {
            this.value = value;
        }

        public int getIntValue() {
            return value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called");
        }

        setContentView(R.layout.activity_database_sync);
        ButterKnife.bind(this);

        mContext = this;

        userDbAdapter = new UserDbAdapter(this);
        userDbAdapter.openUserDB();

        createFreshTablesBtn.setOnClickListener(this);
        executeBtn.setOnClickListener(this);
        latencyTestBtn.setOnClickListener(this);
        throughputTestBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        BmUtilsSingleton.INSTANCE.isFullSystemLatencyTesting = false;
        BmUtilsSingleton.INSTANCE.isFullSystemThroughputTesting = false;

        switch (v.getId()) {
            case R.id.create_fresh_tables_btn:
                try {
                    int numOfTables = Integer.parseInt(eTextNumTables.getText().toString().trim());
                    int numOfEntriesPerTable = Integer.parseInt(eTextNumEntries.getText().toString().trim());

                    // For testing purpose fresh tables creations will be done using native database
                    updateCurrentDatabase(BmUtilsSingleton.SYNC_TYPE.NATIVE);
                    createFreshTables(numOfTables, numOfEntriesPerTable);

                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();

                    showToast("Enter no. of Tables to create and no. of Entries per Table");
                }
                break;

            case R.id.execute_btn:
                /**
                 * Setting the Sync Type which will decide the sync strategy
                 * To see its effects check BmSQLiteOpenHelper class in the database shim
                 */
                BmUtilsSingleton.INSTANCE.sync_type = getSyncType();

                BmUtilsSingleton.Operation operation = getOperation();
                OperationType oprType = getOperationType();

                int numOfTables = getNumOfTables(oprType);
                int numOfOperations = getNumOfOperations(oprType);

                executeOperations(operation, numOfTables, numOfOperations);
                break;

            case R.id.latency_test_btn:
                BmUtilsSingleton.INSTANCE.isFullSystemLatencyTesting = true;
                runFullSystemLatencyTest();
                break;

            case R.id.throughput_test_btn:
                BmUtilsSingleton.INSTANCE.isFullSystemThroughputTesting = true;
                runFullSystemThroughputTest();
                break;
        }
    }

    private void runFullSystemLatencyTest() {
        new AsyncTask<Integer, String, Integer>() {
            @Override
            protected void onPreExecute() {
                BmUtilsSingleton.INSTANCE.launchRingDialog(mContext, "Running Full System Latency Test...", "Initializing");
                BmUtilsSingleton.INSTANCE.initTestingFramework();
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                int result = 0;

                try {
                    // Below is the step by step instructions on how testing logic is implemented
                    // STEP 1: Select a Sync Type {NATIVE, OP_LOG, MULTI_DB, FILE_CHUNK}
                    for (BmUtilsSingleton.SYNC_TYPE testSyncType : BmUtilsSingleton.SYNC_TYPE.values()) {

                        /******** Create Fresh Database Start *************/
                        BmUtilsSingleton.INSTANCE.canLog = false;

                        // Load the NATIVE database
                        updateCurrentDatabase(BmUtilsSingleton.SYNC_TYPE.NATIVE);
                        int numOfTables = Integer.parseInt(BmUtilsSingleton.INSTANCE.getTableEntryList().get(0).split(":")[0].trim());
                        int numOfEntriesPerTable = Integer.parseInt(BmUtilsSingleton.INSTANCE.getTableEntryList().get(0).split(":")[1].trim());

                        createFreshTablesForFullSystemTesting(numOfTables, numOfEntriesPerTable);

                        BmUtilsSingleton.INSTANCE.canLog = true;
                        /******** Create Fresh Database End *************/


                        // Load the current sync type database
                        updateCurrentDatabase(testSyncType);
                        userDbAdapter.init();

                        // IMP: For MULTI_DB we have to create fresh tables with MULTI_DB approach and not with NATIVE approach
                        if (testSyncType == BmUtilsSingleton.SYNC_TYPE.MULTI_DB) {
                            createFreshTablesForFullSystemTesting(numOfTables, numOfEntriesPerTable);
                            TimeUnit.SECONDS.sleep(2);
                        }

                        // STEP 2: Select the number of operations to be performed
                        // Note: Actual Number of Performed Operations will be (testNumOfOperations X testInitialTableEntrySize)
                        for (Integer testNumOfOperations : BmUtilsSingleton.INSTANCE.getOperationCountList()) {

                            String progressMsg = "Currently Executing -> Sync Type : " + testSyncType + ", Operation : ALL"
                                    + ", NumOfTables : " + numOfTables + ", NumOfOperations per table : "
                                    + testNumOfOperations;

                            publishProgress(progressMsg);

                            // STEP 3: Perform multiple operations in the order {UPDATE, QUERY, INSERT, DELETE}
                            userDbAdapter.performMultiOperationsOnTableFiles(numOfTables, testNumOfOperations);
                            TimeUnit.SECONDS.sleep(10);
                        }

                        // STEP 4: For Avoiding Memory leaks and making objects eligible for Garbage Collection
                        userDbAdapter.clear();

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    result = -1;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    result = -1;
                } catch (Exception e) {
                    e.printStackTrace();
                    result = -1;
                } finally {
                    try {
                        // Giving enough time so that all the background operations should get performed before we finish
                        TimeUnit.SECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return result;
            }

            @Override
            protected void onProgressUpdate(String... message) {
                BmUtilsSingleton.INSTANCE.updateProgressDialogMessage(message[0]);
            }

            @Override
            protected void onPostExecute(Integer msg) {
                BmUtilsSingleton.INSTANCE.closeDumpFiles();
                BmUtilsSingleton.INSTANCE.dismissProgressDialog();

                String message;

                if (msg == 0) {
                    message = "Full system latency testing completed successfully. But some tasks might be running in background";
                } else {
                    message = "We detected some errors while executing full latency system testing. Kindly debug the code";
                }

                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    Log.v(TAG, message);
                }

                showToast(message);
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

    }

    private void runFullSystemThroughputTest() {
        new AsyncTask<Integer, String, Integer>() {
            @Override
            protected void onPreExecute() {
                BmUtilsSingleton.INSTANCE.launchRingDialog(mContext, "Running Full System Throughput Test...", "Initializing");
                BmUtilsSingleton.INSTANCE.initTestingFramework();
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                int result = 0;

                try {
                    // STEP 1: Select the number of operations to be performed
                    // Note: Actual Number of Performed Operations will be (testNumOfOperations X testInitialTableEntrySize)
                    for (Integer testNumOfOperations : BmUtilsSingleton.INSTANCE.getOperationCountList()) {

                        // STEP 2: Select a Sync Type {NATIVE, OP_LOG, MULTI_DB, FILE_CHUNK}
                        for (BmUtilsSingleton.SYNC_TYPE testSyncType : BmUtilsSingleton.SYNC_TYPE.values()) {

                            /******** Create Fresh Database Start *************/
                            BmUtilsSingleton.INSTANCE.canLog = false;

                            // Load the NATIVE database
                            updateCurrentDatabase(BmUtilsSingleton.SYNC_TYPE.NATIVE);
                            int numOfTables = Integer.parseInt(BmUtilsSingleton.INSTANCE.getTableEntryList().get(0).split(":")[0].trim());
                            int numOfEntriesPerTable = Integer.parseInt(BmUtilsSingleton.INSTANCE.getTableEntryList().get(0).split(":")[1].trim());

                            createFreshTablesForFullSystemTesting(numOfTables, numOfEntriesPerTable);

                            BmUtilsSingleton.INSTANCE.canLog = true;
                            /******** Create Fresh Database End *************/


                            // Load the current sync type database
                            updateCurrentDatabase(testSyncType);
                            userDbAdapter.init();


                            // IMP: For MULTI_DB we have to create fresh tables with MULTI_DB approach and not with NATIVE approach
                            if (testSyncType == BmUtilsSingleton.SYNC_TYPE.MULTI_DB) {
                                createFreshTablesForFullSystemTesting(numOfTables, numOfEntriesPerTable);
                                TimeUnit.SECONDS.sleep(2);
                            }


                            // STEP 3: Select the test operation {INSERT, QUERY, UPDATE, DELETE, EXEC_SQL}
                            for (BmUtilsSingleton.Operation testOperation : BmUtilsSingleton.Operation.values()) {

                                // For testing purpose we will test not support DELETE_ALL and EXEC_SQL
                                if (testOperation == BmUtilsSingleton.Operation.DELETE_ALL
                                        || testOperation == BmUtilsSingleton.Operation.EXEC_SQL) {
                                    continue;
                                }


                                String progressMsg = "Currently Executing -> Sync Type : " + testSyncType + ", Operation : " + testOperation
                                        + ", NumOfTables : " + numOfTables + ", NumOfOperations per table : "
                                        + testNumOfOperations;

                                publishProgress(progressMsg);

                                // STEP 4: Execute the selected operations
                                executeOperationsForFullSystemThroughputTesting(testSyncType, testOperation, BmUtilsSingleton.INSTANCE.OPERATION_QUEUE_SIZE, numOfTables, testNumOfOperations);

                                // Giving enough time so that all the background operations should get performed before we move to next sync type
                                TimeUnit.SECONDS.sleep(50);
                            }

                            // STEP 5: For Avoiding Memory leaks and making objects eligible for Garbage Collection
                            userDbAdapter.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    result = -1;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    result = -1;
                } catch (Exception e) {
                    e.printStackTrace();
                    result = -1;
                } finally {
                    try {
                        TimeUnit.SECONDS.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return result;
            }

            @Override
            protected void onProgressUpdate(String... message) {
                BmUtilsSingleton.INSTANCE.updateProgressDialogMessage(message[0]);
            }

            @Override
            protected void onPostExecute(Integer msg) {
                BmUtilsSingleton.INSTANCE.closeDumpFiles();
                BmUtilsSingleton.INSTANCE.dismissProgressDialog();

                String message;

                if (msg == 0) {
                    message = "Full system throughput testing completed successfully. But some tasks might be running in background";
                } else {
                    message = "We detected some errors while executing full system testing. Kindly debug the code";
                }

                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    Log.v(TAG, message);
                }

                showToast(message);
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

    }

    private void updateCurrentDatabase(BmUtilsSingleton.SYNC_TYPE syncType) {
        BmUtilsSingleton.INSTANCE.sync_type = syncType;

        userDbAdapter.closeUserDB();
        userDbAdapter.openUserDB();
    }

    private BmUtilsSingleton.SYNC_TYPE getSyncType() {

        if (nativeRadioBtn.isChecked()) {
            return BmUtilsSingleton.SYNC_TYPE.NATIVE;

        } else if (opLogRadioBtn.isChecked()) {
            return BmUtilsSingleton.SYNC_TYPE.OP_LOG;

        } else if (multiDbRadioBtn.isChecked()) {
            return BmUtilsSingleton.SYNC_TYPE.MULTI_DB;

        } else if (fileChunkRadioBtn.isChecked()) {
            return BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK;
        } else {
            return BmUtilsSingleton.SYNC_TYPE.NATIVE;
        }
    }

    public void onSyncTypeRadioButtonClick(View view) {
        isSyncTypeChanged = false;

        switch (view.getId()) {
            case R.id.native_radio_btn:
                if (BmUtilsSingleton.INSTANCE.sync_type != BmUtilsSingleton.SYNC_TYPE.NATIVE) {
                    isSyncTypeChanged = true;
                }

                BmUtilsSingleton.INSTANCE.sync_type = BmUtilsSingleton.SYNC_TYPE.NATIVE;
                break;

            case R.id.op_log_radio_btn:
                if (BmUtilsSingleton.INSTANCE.sync_type != BmUtilsSingleton.SYNC_TYPE.OP_LOG) {
                    isSyncTypeChanged = true;
                }

                BmUtilsSingleton.INSTANCE.sync_type = BmUtilsSingleton.SYNC_TYPE.OP_LOG;
                break;

            case R.id.multi_db_radio_btn:
                if (BmUtilsSingleton.INSTANCE.sync_type != BmUtilsSingleton.SYNC_TYPE.MULTI_DB) {
                    isSyncTypeChanged = true;
                }

                BmUtilsSingleton.INSTANCE.sync_type = BmUtilsSingleton.SYNC_TYPE.MULTI_DB;
                break;

            case R.id.file_chunk_radio_btn:
                if (BmUtilsSingleton.INSTANCE.sync_type != BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK) {
                    isSyncTypeChanged = true;
                }

                BmUtilsSingleton.INSTANCE.sync_type = BmUtilsSingleton.SYNC_TYPE.FILE_CHUNK;
                break;
        }

        if (isSyncTypeChanged) {
            // For testing purpose execute every operation for different sync type on a fresh instance of DB
            userDbAdapter.closeUserDB();
            userDbAdapter.openUserDB();
        }
    }

    private OperationType getOperationType() {
        OperationType oprType;

        if (randomOprRadioBtn.isChecked()) {
            oprType = OperationType.RANDOM_OPERATIONS;

        } else if (multiOprRadioBtn.isChecked()) {
            oprType = OperationType.MULTI_OPERATIONS;

        } else {
            oprType = OperationType.SINGLE_OPERATION;
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "Selected OperationType : " + oprType);
        }

        return oprType;
    }

    private int getNumOfTables(OperationType oprType) {
        Random ran = new Random();
        int numOfTables = 0;

        switch (oprType) {
            case SINGLE_OPERATION:
                numOfTables = 1;
                break;

            case MULTI_OPERATIONS:
                try {
                    numOfTables = Integer.parseInt(eTxtNumOfTables.getText().toString());
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    numOfTables = 1;
                }
                break;

            case RANDOM_OPERATIONS:
                numOfTables = ran.nextInt(MAX_RANDOM_VALUE);
                break;
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "Selected numOfTables : " + numOfTables);
        }

        return numOfTables;
    }


    private int getNumOfOperations(OperationType oprType) {
        Random ran = new Random();
        int numOfOperations = 0;

        switch (oprType) {
            case SINGLE_OPERATION:
                numOfOperations = 1;
                break;

            case MULTI_OPERATIONS:
                try {
                    numOfOperations = Integer.parseInt(eTxtNumOfOpr.getText().toString());
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    numOfOperations = 1;
                }
                break;

            case RANDOM_OPERATIONS:
                numOfOperations = ran.nextInt(MAX_RANDOM_VALUE);
                break;
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "Selected numOfOperations : " + numOfOperations);
        }

        return numOfOperations;
    }

    private BmUtilsSingleton.Operation getOperation() {
        BmUtilsSingleton.Operation operation;

        if (updateRadioBtn.isChecked()) {
            operation = BmUtilsSingleton.Operation.UPDATE;

        } else if (deleteRadioBtn.isChecked()) {
            operation = BmUtilsSingleton.Operation.DELETE;

        } else if (deleteAllRadioBtn.isChecked()) {
            operation = BmUtilsSingleton.Operation.DELETE_ALL;

        } else {
            operation = BmUtilsSingleton.Operation.INSERT;
        }

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "Selected Operation : " + operation);
        }

        return operation;
    }

    public void onOprTypeRadioButtonClicked(View v) {
        switch (v.getId()) {
            case R.id.single_opr_radio_btn:
            case R.id.random_opr_radio_btn:
                txtNumOfTables.setVisibility(View.GONE);
                eTxtNumOfTables.setVisibility(View.GONE);
                txtNumOfOpr.setVisibility(View.GONE);
                eTxtNumOfOpr.setVisibility(View.GONE);
                break;

            case R.id.multi_opr_radio_btn:
                txtNumOfTables.setVisibility(View.VISIBLE);
                eTxtNumOfTables.setVisibility(View.VISIBLE);
                txtNumOfOpr.setVisibility(View.VISIBLE);
                eTxtNumOfOpr.setVisibility(View.VISIBLE);
                break;

        }
    }

    private int createFreshTablesForFullSystemTesting(final int numOfTables, final int numOfEntriesPerTable) {
        int prevTableCount = userDbAdapter.deleteAllTablesAndFiles();

        userDbAdapter.createMultiTableFiles(numOfTables);
        userDbAdapter.insertMultiEntriesIntoTableFiles(numOfTables, numOfEntriesPerTable);

        return prevTableCount;
    }

    private void createFreshTables(final int numOfTables, final int numOfEntriesPerTable) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "createFreshTables called with numOfTables : " + numOfTables + " , numOfEntriesPerTable : " + numOfEntriesPerTable);
        }

        new AsyncTask<Integer, String, Integer>() {
            @Override
            protected void onPreExecute() {
                BmUtilsSingleton.INSTANCE.launchRingDialog(mContext, "Please wait ...", "Creating fresh Tables");
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                try {
                    int prevTableCount = userDbAdapter.deleteAllTablesAndFiles();

                    publishProgress("Deleted previous " + prevTableCount + " Tables");

                    userDbAdapter.createMultiTableFiles(numOfTables);
                    userDbAdapter.insertMultiEntriesIntoTableFiles(numOfTables, numOfEntriesPerTable);

                } catch (Exception ex) {
                    return -1;
                }

                return 0;
            }

            @Override
            protected void onProgressUpdate(String... message) {
                showToast(message[0]);
            }

            @Override
            protected void onPostExecute(Integer msg) {
                BmUtilsSingleton.INSTANCE.dismissProgressDialog();
                String message;

                if (msg == 0) {
                    message = "Created fresh " + numOfTables + " Tables with " + numOfEntriesPerTable + " Entries in each Table";
                } else {
                    message = "Error creating fresh Tables";
                }

                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    Log.v(TAG, message);
                }

                showToast(message);

            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void executeOperationsForFullSystemThroughputTesting(final BmUtilsSingleton.SYNC_TYPE syncType, final BmUtilsSingleton.Operation operation, final int operationQueueSize, final int numOfTables, final int numOfOperations) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "executeOperationsForFullSystemThroughputTesting called with Operation : " + operation + " , numOfTables : " + numOfTables + " , numOfOperations : " + numOfOperations);
        }

        try {
            switch (operation) {
                case INSERT:
                    BmUtilsSingleton.TestThroughput insertTestObject = BmUtilsSingleton.INSTANCE.getTestThroughputObject(syncType, operation, operationQueueSize, numOfTables, numOfOperations);
                    userDbAdapter.insertMultiEntriesIntoTableFiles(numOfTables, numOfOperations);
                    insertTestObject.recordOperationsExecutionTime();
                    BmUtilsSingleton.INSTANCE.endThroughputTestWithSingleFileDump(insertTestObject);
                    break;

                case QUERY:
                    BmUtilsSingleton.TestThroughput queryTestObject = BmUtilsSingleton.INSTANCE.getTestThroughputObject(syncType, operation, operationQueueSize, numOfTables, numOfOperations);
                    userDbAdapter.queryMultiEntriesFromTableFiles(numOfTables, numOfOperations);
                    queryTestObject.recordOperationsExecutionTime();
                    BmUtilsSingleton.INSTANCE.endThroughputTestWithSingleFileDump(queryTestObject);
                    break;

                case UPDATE:
                    BmUtilsSingleton.TestThroughput updateTestObject = BmUtilsSingleton.INSTANCE.getTestThroughputObject(syncType, operation, operationQueueSize, numOfTables, numOfOperations);
                    userDbAdapter.updateMultiEntriesOfTableFiles(numOfTables, numOfOperations);
                    updateTestObject.recordOperationsExecutionTime();
                    BmUtilsSingleton.INSTANCE.endThroughputTestWithSingleFileDump(updateTestObject);
                    break;

                case DELETE:
                    BmUtilsSingleton.TestThroughput deleteTestObject = BmUtilsSingleton.INSTANCE.getTestThroughputObject(syncType, operation, operationQueueSize, numOfTables, numOfOperations);
                    userDbAdapter.deleteMultiEntriesFromTableFiles(numOfTables, numOfOperations);
                    deleteTestObject.recordOperationsExecutionTime();
                    BmUtilsSingleton.INSTANCE.endThroughputTestWithSingleFileDump(deleteTestObject);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void executeOperations(final BmUtilsSingleton.Operation operation, final int numOfTables, final int numOfOperations) {
        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "executeOperations called with Operation : " + operation + " , numOfTables : " + numOfTables + " , numOfOperations : " + numOfOperations);
        }


        new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected void onPreExecute() {
                BmUtilsSingleton.INSTANCE.launchRingDialog(mContext, "Please wait ...", "Executing " + operation + " operation");
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                try {
                    switch (operation) {
                        case INSERT:
                            userDbAdapter.insertMultiEntriesIntoTableFiles(params[1], params[2]);
                            break;

                        case UPDATE:
                            userDbAdapter.updateMultiEntriesOfTableFiles(params[1], params[2]);
                            break;

                        case DELETE:
                            userDbAdapter.deleteMultiEntriesFromTableFiles(params[1], params[2]);

                            break;

                        case DELETE_ALL:
                            userDbAdapter.deleteAllEntriesFromTableFiles();
                            break;

                    }
                } catch (Exception ex) {
                    return -1;
                }

                return 0;
            }

            @Override
            protected void onProgressUpdate(Integer... prevTableCount) {
            }

            @Override
            protected void onPostExecute(Integer msg) {
                BmUtilsSingleton.INSTANCE.dismissProgressDialog();
                String message;

                if (msg == 0) {
                    message = "Executed " + operation + " operation and performed " + numOfOperations + " operations";
                } else {
                    message = "Error executing " + operation + " operation";
                }

                if (BmUtilsSingleton.INSTANCE.DEBUG) {
                    Log.v(TAG, message);
                }

                showToast(message);
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, operation.getIntValue(), numOfTables, numOfOperations);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != userDbAdapter) {
            userDbAdapter.closeUserDB();
        }
    }
}

