package edu.buffalo.rms.bluemountain.shim;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import edu.buffalo.rms.bluemountain.framework.BmFileSystemFramework;

/**
 * Created by sharath on 8/17/16.
 */
public class BmFileInputStream extends InputStream {

    private static final String TAG = "BM_Fis";

    private final Context mContext;

    BmFileSystemFramework mBmFramework = null;
    private BmFileMetadata mFileMetaStore;
    private BmObject mBmObj;
    public int BM_OBJECT_SIZE = BmFileOutputStream.BM_OBJECT_SIZE;
    /*
     * if the read is for a file that has not been created using BlueMountain
     * outputStream, treat is like a local file
     */
    private FileInputStream mBmLocalFileStream = null;

    public BmFileInputStream(Context ctx) {
        mContext = ctx;
        mBmFramework = BmFileSystemFramework.getInstance();
        mFileMetaStore = BmFileMetadata.getInstance();
    }

    public BmFileInputStream(String filename, Context ctx) {
        this(ctx);
        try {
            openFileInput(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int read() throws IOException {
        //TODO: Implement single byte read
        return 0;
    }

    /**
     * The stream of bytes is converted to a set of Objects, with a default size
     * of 16Mb per object. acquireObjectLock is called on all objects associated
     * with this write. This is a blocking call and has to be implemented by
     * the pluggable FS. An Object key is generated and the corresponding
     * metadata is recorded. In a File metadata Objects have a flat file structure
     * Metadata stored in the object BmFileMetadata is updated locally. Finally
     * the write method of the framework is called
     *
     * @param buffer
     * @param off
     * @throws IOException
     */

    @Override
    public int read(byte []buffer, int off, int len) throws IOException {
        /* if remote file system is not registered, just do a local write */
        if (!mBmFramework.hasRemoteFS()) {
            /* if remote file system is not registered, just do a local read */
        }
        if (mBmLocalFileStream != null) {
            //local file reads
            return mBmLocalFileStream.read(buffer, off, len);
        }

        //TODO: Check top most level cache

        // Split the filestream to 16Mb objects.
        int startObj = off / BM_OBJECT_SIZE;
        int endObj = (off + len) / BM_OBJECT_SIZE;
        int lastObjSz = (off + len) % BM_OBJECT_SIZE;

        int firstObjSz;
        int firstObjOffset = off % BM_OBJECT_SIZE;

        if (firstObjOffset == 0) {
            //read starts at object boundary
            firstObjSz = (len < BM_OBJECT_SIZE) ? len : BM_OBJECT_SIZE;
        } else {
            // check if the object ends at boundary
            firstObjSz = BM_OBJECT_SIZE - (off  % BM_OBJECT_SIZE);
        }

        byte [] buf = new byte[BM_OBJECT_SIZE];
        int buffer_pos = 0;

        //first object
        readObject(mBmObj.getObjAtIndex(startObj), firstObjOffset, buf, firstObjSz);
        System.arraycopy(buf, firstObjOffset, buffer, buffer_pos, firstObjSz);
        buffer_pos += firstObjSz;

        /* for every object in the middle */
        for (int i = startObj + 1; i < endObj; i++) {
            readObject(mBmObj.getObjAtIndex(i), buf);
            System.arraycopy(buf, 0, buffer, buffer_pos, BM_OBJECT_SIZE);
            buffer_pos += BM_OBJECT_SIZE;
        }

        readObject(mBmObj.getObjAtIndex(endObj), buf);
        System.arraycopy(buf, 0, buffer, buffer_pos, lastObjSz);
        return len;
    }


    public void openFileInput(String filename) throws FileNotFoundException {
        //Todo: filename should be looked up from the hashmap
        mBmObj = mFileMetaStore.getObjFromFilename(filename);
        if (mBmObj == null) {
            // check if this is a local file
            File localFile = new File(filename);
            if (!localFile.exists()) {
                throw new FileNotFoundException();
            }
            mBmLocalFileStream = new FileInputStream(localFile);
        }
    }

    private int readObject(String objId, byte[] buffer) {
        return readObject(objId, 0, buffer);
    }

    private int readObject(String objId, int objOff, byte[] buffer) {
        return readObject(objId, objOff, buffer, buffer.length);
    }

    private int readObject(String objId, int objOff, byte[] buffer, int len) {
        //TODO: Background task
        Log.v(TAG, "Using remote file-system");

        long startTime = System.nanoTime();
        int ret = 0;
        mBmFramework.fread(objId, objOff, buffer, len);
        long difference = System.nanoTime() - startTime;
        Toast.makeText(mContext, "Remote Write took: " + difference / 1000000 + "ms",
                Toast.LENGTH_LONG).show();
        Log.v(TAG, "Remote Write took: " + difference + "ns");
        return ret;
    }
}
