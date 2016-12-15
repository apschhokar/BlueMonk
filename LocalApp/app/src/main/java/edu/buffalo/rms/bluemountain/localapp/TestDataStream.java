package edu.buffalo.rms.bluemountain.localapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by sharath on 5/2/16.
 */
public class TestDataStream {
    static public final int ONE_KB = 1024;
    static public final int TEN_KB = (10 * ONE_KB);
    static public final int ONE_MB = (ONE_KB * ONE_KB);
    static public final int TEN_MB = (10 * ONE_MB);
    static public final int HUNDRED_MB = (100 * ONE_MB);

    Context mCtx;
    AssetManager mAsst;

    TestDataStream(Context ctx) {
        mCtx = ctx;
        mAsst = mCtx.getAssets();
    }

    public InputStream getTestInputStream(int size) {
        InputStream is = null;
        try {
            is = mAsst.open(getFilebySize(size));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    static public String getFilebySize(int size) {
        switch (size) {
            case ONE_KB:
               return "1K_random.dat";
            case TEN_KB:
                return "10K_random.dat";
           case ONE_MB:
               return "1M_random.dat";
           case TEN_MB:
               return "10M_random.dat";
           case HUNDRED_MB:
               return "100M_random.dat";
        }
        return null;
    }
}
