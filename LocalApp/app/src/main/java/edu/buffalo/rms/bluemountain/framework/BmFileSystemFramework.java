package edu.buffalo.rms.bluemountain.framework;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import dalvik.system.DexClassLoader;
import edu.buffalo.rms.bluemountain.pluggablefs.IBmFileSystem;

/**
 * Created by sharath on 3/19/16.
 *
 * This will be a singleton class
 * BmFileSystemFramework class initializes the framework and provides methods
 * to load the remote classes and methods. The FileSystem implements the full
 * set of the BlueMountain interface. All functions will be either default functions
 * or overridden functions. The framework also provides ways to invoke functions
 * through events. The framework also creates background tasks, Ex. When uploading.
 *
 * The framework deals with objects of fixed size only. And the least-count is
 * an object.
 */
public class BmFileSystemFramework implements IBmFramework {
    private Context mContext;
    private IBmFileSystem mRemoteInterface;
    private boolean mRemoteFS;
    private File mScratchDir;
    private static BmFileSystemFramework mInstance;
    boolean mTestMode = false;

    /**
     * Singleton class and constructors
     */
    public static BmFileSystemFramework getInstance() {
        if (mInstance == null) {
            mInstance = new BmFileSystemFramework();
        }
        return mInstance;
    }

    protected BmFileSystemFramework() {
        //initialize Framework
    }

    // Public methods

    public void setTestMode(boolean mode) {
        mTestMode = mode;
    }

    //TODO: get context statically
    public void setContext(Context context) {
        this.mContext = context;
        mScratchDir = new File(mContext.getFilesDir(), "scratch");
        if (!mScratchDir.exists()) {
            mScratchDir.mkdir();
        }
    }

    // onStart()
    public void initRemoteFS(String remotePackage, String remoteClass) {
        mRemoteFS = true;
        mRemoteInterface = (IBmFileSystem) loadClass(remotePackage, remoteClass);
        /* Other initializations to follow */
        mRemoteInterface.initFS();
    }

    public boolean hasRemoteFS() {
        return mRemoteFS;
    }

    /**
     * BlueMountain CRUD Interface:
     * <p>
     * Create  : fcreate
     * Read    : fread
     * Update  : fupdate
     * Delete  : fdelete
     * <p>
     * Additional interfaces
     * <p>
     * Lock    : acquireObjectLock
     * Flush   : flush
     * Close   : close
     */

    public void fcreate(String objId, byte[] buf, int len) {
        if (!mRemoteFS || mTestMode) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(new File(mScratchDir, objId));
                fos.write(buf, 0, len);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        mRemoteInterface.fcreate(objId, buf, len);
    }

    public void fupdate(String objId, byte[] buf, int off, int len) {
        // Update the object
        if (!mRemoteFS || mTestMode) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(mScratchDir, objId));
                fos.write(buf, off, buf.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        mRemoteInterface.fupdate(objId, buf, off, len);
    }

    /**
     * @param objId  :
     * @param objOff
     * @param buf
     * @param len
     */
    public void fread(String objId, int objOff, byte[] buf, int len) {
        if (!mRemoteFS || mTestMode) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(new File(mScratchDir, objId));
                fis.read(buf);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        mRemoteInterface.fread(objId, objOff, buf, len);
    }

    public void fdelete(String objId) {
        if (!mRemoteFS || mTestMode) {
            return;
        }
        mRemoteInterface.fdelete(objId);
    }

    public void flush(List<String> objIds) {
        if (!mRemoteFS || mTestMode) {
            return;
        }
        for (String id : objIds) {
            /* All chunks with this object ID are done */
            mRemoteInterface.flush(id);
        }
    }

    public void close(List<String> objIds) {
        if (!mRemoteFS || mTestMode) {
            return;
        }
        for (String id : objIds) {
            /* All chunks with this object ID are done */
            mRemoteInterface.onClose(id);
        }
    }

    private Object loadClass(String remotePackage, String remoteClass) {
        Object instance = null;
        try {
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(remotePackage, 0);
            String fullPathToApk = ai.publicSourceDir;
            Log.v("CLASS-LOADER", "PATH: " + fullPathToApk);
            File dexOutputDir = mContext.getDir("dex", Context.MODE_PRIVATE);
            DexClassLoader dexLoader = new DexClassLoader(fullPathToApk,
                    dexOutputDir.getAbsolutePath(), null, mContext.getClassLoader());
            Class<?> hostAPIClass = Class.forName(remoteClass, true, dexLoader);
            Constructor<?> constructor = hostAPIClass.getConstructor(new Class[]{Context.class});
            instance = constructor.newInstance(new Object[]{mContext});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }
}

//    public void acquireObjectLock(String objId) {
//        //TODO: Framework lock?
//        //TODO: Change this function to initIO, lock can be implemented inside that.
//        //TODO: this function should return a context object
//        mRemoteInterface.acquireObjectLock(objId);
//    }
//
//    public void flushChunk(String chunkId) {
//        BmChunk chunk = getChunkFromId(chunkId);
//        String response = uploadDbTableFileToServer(chunk.getData(), chunk.getAccessTkn(), chunk.getServerUrl(),
//                chunk.getHeader(), chunk.getHeaderVal());
//        mRemoteInterface.onUpload(chunkId, response);
//    }
//
//
//    private byte[] readRemoteChunk(String chunkId) {
//        BmChunk chunk = mChunkCache.getChunk(chunkId);
//        if (chunk != null) {
//            //present in local cache
//            return chunk.getData();
//        }
//        byte [] buf = mRemoteInterface.read(chunkId);
//        return buf;
//    }
//
//    private byte[] preProcessObject(byte [] buf) {
//        //Todo: Have a default implementation
//        byte [] processedBuf = mRemoteInterface.preProcessObject(buf);
//        return processedBuf;
//    }
//
//    private void writeToFrameworkCache(String objId, byte [] bytes) {
//        //TODO: implement cache
//    }
//
//    private BmChunk[] getChunksFromObject(String objId, byte[] bytes) {
//        return getChunksFromObject(objId, bytes, 0);
//    }
//
//    private BmChunk[] getChunksFromObject(String objId, byte[] bytes, int off) {
//        /* too much memory copy going in here */
//        /* Instead of returning the byte, return only the start and len of each */
//        return mRemoteInterface.getChunksFromObject(objId, bytes, off);
//    }
//
//    /**
//     * uploadDbTableFileToServer
//     *
//     * This method uploads a file to a sent URL
//     * The HTTP requires some headers. This includes standards headers like
//     * "Content-type", "Authorization" and specific headers like "Dropbox-API-arg"
//     * "Authorization" can be "Bearer" or "Basic"
//     *
//     * Example Dropbox request is:
//     *
//     *  HTTP POST https://content.dropboxapi.com/2/files/upload
//                --header "Authorization: Bearer ACCESS-CODE"
//                --header "Dropbox-API-Arg:
//                    {
//                        "path": "/test.txt",
//                        "mode": "add",
//                        "autorename": true,
//                        "mute": false,
//                     }"
//                --header "Content-Type: application/octet-stream"
//                --data-binary @local_file.txt
//     *
//     * The custom headers have to be sent as a string whereas the BlueMountain can deduce the
//     * standard headers.
//     * Dropbox also allows uploadDbTableFileToServer "session-start", "append", "finish" to uploadDbTableFileToServer large files
//     *
//     * @return
//     */
//    private String uploadDbTableFileToServer(byte[] buf, String accessTkn, String remoteUrl,
//                      String customHdr, String customVal) {
//        // Asynchronously called by the framework for each chunk.
//        // Can also be called by the fs through flushChunk
//        String response = null;
//        try {
//            URL urlObj = new URL(remoteUrl);
//            HttpURLConnection httpCon = (HttpURLConnection) urlObj.openConnection();
//            httpCon.setDoOutput(true);
//            httpCon.setRequestProperty("Authorization", "Bearer " + accessTkn);
//            if (customHdr != null) {
//                httpCon.setRequestProperty(customHdr, customVal);
//            }
//            httpCon.setRequestProperty("Content-Type", "application/octet-stream");
//            httpCon.setRequestMethod("POST");
//            httpCon.getOutputStream().write(buf);
//            httpCon.getOutputStream().close();
//
//            String httpResp = httpCon.getResponseMessage();
//            byte[] retBuf = new byte [HTTP_RESPONSE_BUF_SZ];
//            int off = 0;
//            while (httpCon.getInputStream().available() > 0) {
//                int bytesRead = httpCon.getInputStream().read(retBuf, off, retBuf.length - off);
//                off += bytesRead;
//            }
//            response = new String(retBuf, httpCon.getContentEncoding());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return response;
//    }
//
//    private BmChunk getChunkFromId(String chunkId) {
//        return null;
//    }
