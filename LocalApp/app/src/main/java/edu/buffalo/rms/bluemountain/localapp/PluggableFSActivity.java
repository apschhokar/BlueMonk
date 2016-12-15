package edu.buffalo.rms.bluemountain.localapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import edu.buffalo.rms.bluemountain.framework.BmFileSystemFramework;
import edu.buffalo.rms.bluemountain.framework.FilesizeUnits;
import edu.buffalo.rms.bluemountain.shim.BmFileOutputStream;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static edu.buffalo.rms.bluemountain.localapp.PluggableFSActivity.TestMode.*;

public class PluggableFSActivity extends AppCompatActivity {
    private static final int NUMBER_OF_RUNS = 100;
    public static int READ_BLOCK_SZ = (4 * FilesizeUnits.ONE_MB);

    static String TAG = "LOCAL_APP";
    static String TESTFILE = "blueMountain-test";

    private static final int MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE = 1;
    BmFileSystemFramework mFileProxy = null;

    public enum TestMode {NATIVE_IO, FRAMEWORK_IO, PLUGGABLE_IO, FULL_TESTS}

    private TextView mTv;

    public static TestMode mTestMode = FULL_TESTS;
    private int mTestSize = TestDataStream.TEN_MB; //default

    public void getStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE);
        } else {
            filesWriteTest();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission was granted");
                    //continue with writeTest
                    filesWriteTest();
                } else {
                    Log.v(TAG, "Permission denied");
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFileProxy.hasRemoteFS()) {
            mTv.setText("ElkHorn: Remote-FS");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pluggable_fs);

        /* Initialize the framework */
        mFileProxy = BmFileSystemFramework.getInstance();
        mFileProxy.setContext(getApplicationContext());

        //Toast.makeText(this, data, Toast.LENGTH_LONG).show();
        Log.v(TAG, "LocalApp started with PID: " + Process.myPid());
        mTv = (TextView) findViewById(R.id.textview);
        mTv.setText("ElkHorn: Local-FS");
    }

    public void fileWriteOnClick(View v) {
        getStoragePermissions();
    }

    public void filesWriteTest() {
        Log.v(TAG, "Returned from getting permissions");
        long time = 0;
        switch (mTestMode) {
            case FULL_TESTS:
                time = filesWriteFullTest(mTestSize);
                break;
            case NATIVE_IO:
                time = singleWriteTest("OneMB", mTestSize, true);
                break;
            case FRAMEWORK_IO:
                mFileProxy.setTestMode(true);
            case PLUGGABLE_IO:
                time = singleWriteTest("OneMB", mTestSize, false);
                break;
        }

        String toastTxt = "Done. Tests took: " + time / 1000000 + "ms";
        Toast.makeText(this, toastTxt, Toast.LENGTH_LONG).show();
    }

    /**
     * Data set size: 1Kb, 10Kb, 1Mb, 10Mb, 100Mb
     *
     * Number of tests: 100
     *
     * 1. Native IO
     *      a. Without flush()
     *      b. With flush()
     * 2. Native IO with translation layer
     *      a. Without flush()
     *      b. With flush()
     * 3. Remote IO cache
     *      a. Without flush()
     *      b. With flush to cache ()
     * 4. Remote IO:
     *      a. Cloud (Sync to Dropbox)
     *      b. Desktop (Sync to local system)
     *      c. Wifi-Direct
     */
    private long filesWriteFullTest(int size) {

        mTv.setText("Starting Tests...");
        long totalTime = 0;

        File path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        File logFile = new File(path, "BlueMountain_log_file.txt");
        BufferedWriter logWriter = null;

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                logWriter = new BufferedWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String prefix = TestDataStream.getFilebySize(size) + "_native_";
        try {
            logWriter.write("Native-IO test, no flush. Size + " + size);
            logWriter.newLine();
            for (int i = 0; i < NUMBER_OF_RUNS; i++) {
                long time = singleWriteTest(prefix + i , size, true);
                totalTime += time;
                logWriter.write(i + ". " + time/(1000) + " us");
                logWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mFileProxy.setTestMode(true);
        prefix = TestDataStream.getFilebySize(size) + "_framework_";
        try {
            logWriter.write("Framework-IO test, no flush");
            logWriter.newLine();
            for (int i = 0; i < NUMBER_OF_RUNS; i++) {
                long time = singleWriteTest(prefix + i, size, false);
                totalTime += time;
                logWriter.write(i + ". " + time/(1000) + " us");
                logWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!mFileProxy.hasRemoteFS()) {
            Log.v(TAG, "No remoteFS, exiting");
            return totalTime;
        }
        mFileProxy.setTestMode(false);
        prefix = TestDataStream.getFilebySize(size) + "_pluggable_";
        try {
            logWriter.write("Remote-IO cached test, no flush");
            logWriter.newLine();
            for (int i = 0; i < NUMBER_OF_RUNS; i++) {
                long time = singleWriteTest(prefix + i, size, false);
                totalTime += time;
                logWriter.write(i + ". " + time/(1000) + " us");
                logWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            logWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mTv.setText("All tests done...");
        return totalTime;
    }

    public long singleWriteTest(String fileSuffix, int size, boolean nativeMode) {
        Log.v(TAG, "Size of test data: " + size);

        TestDataStream testStream = new TestDataStream(getApplicationContext());
        InputStream testFis = testStream.getTestInputStream(size);
        String filename = TESTFILE + fileSuffix;

        byte[] inData = new byte[READ_BLOCK_SZ];
        long startTime = System.nanoTime();
        try {
            int bytesRead = testFis.read(inData);

            if (nativeMode) {
                FileOutputStream nativeFos = openFileOutput(filename, Context.MODE_PRIVATE);
                nativeFos.write(inData, 0, bytesRead);
                long difference = System.nanoTime() - startTime;
                nativeFos.close();
                Log.v(TAG, "Native Write took: " + difference + "ns");
                return difference;
            }

            BmFileOutputStream bmos = new BmFileOutputStream(getApplicationContext());
            bmos.openFileOutput(filename, Context.MODE_PRIVATE);

            while (bytesRead != -1) {
                Log.v(TAG, "Read " + bytesRead + " Bytes from the input stream");
                bmos.write(inData, 0, bytesRead);
                bytesRead = testFis.read(inData);
            }
            bmos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to write file: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        long difference = System.nanoTime() - startTime;
        Log.v(TAG, "Local Write took: " + difference + "ns");
        return difference;
    }

    public void filesReadTest(View v) {

    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.nativeIo:
                if (checked)
                    mTestMode = NATIVE_IO;
                    break;
            case R.id.framework:
                if (checked)
                    mTestMode = FRAMEWORK_IO;
                    break;
            case R.id.pluggable:
                if (checked)
                    mTestMode = PLUGGABLE_IO;
                    break;
            case R.id.fullTests:
                if (checked)
                    mTestMode = FULL_TESTS;
                break;
        }
    }
}
