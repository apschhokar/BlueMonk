package edu.buffalo.rms.bluemountain.framework;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import edu.buffalo.rms.bluemountain.localapp.IBmRemoteService;

/**
 * Created by sharath on 4/7/16.
 */
public class BmRemoteService extends Service {
    public String TAG = "BmRemoteService";
    public static int getReadParam() {
        return mReadParam;
    }

    public static int mReadParam = 0;
    private String REMOTE_PACKAGE = "edu.buffalo.rms.bluemountain.pluggablefs";
    private String REMOTE_CLASS = "edu.buffalo.rms.bluemountain.pluggablefs.PSCloudFS";
    BmFileSystemFramework mFileProxy = null;
    @Override
    public void onCreate() {
        Log.v(TAG, "Creating Bind service, my PID: " + Process.myPid());
        //Todo: Prompt for permission()
        mFileProxy = BmFileSystemFramework.getInstance();
        mFileProxy.initRemoteFS(REMOTE_PACKAGE, REMOTE_CLASS);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Request to get binder");
        return mBinder;
    }

    private static String mFSApp = null;

    private final IBmRemoteService.Stub mBinder = new IBmRemoteService.Stub() {
        /* Add more parameter configuration functions here */
        public void setReadParam(int readParam) {
            Log.v(TAG, "Received remote call with " + readParam);
            Log.v(TAG, "Current value of readParam " + mReadParam);
            mReadParam = readParam;

        }
        public void setFSApp(String fsApp) {
            Log.v(TAG, "Received remote call with " + fsApp);
            mFSApp = fsApp;
        }

    };

    public static String getFSApp() {
        return mFSApp;
    }
}


