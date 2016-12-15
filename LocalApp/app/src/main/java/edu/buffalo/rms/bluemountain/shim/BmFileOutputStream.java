package edu.buffalo.rms.bluemountain.shim;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import edu.buffalo.rms.bluemountain.framework.BmFileSystemFramework;

/**
 * Created by sharath on 7/17/16.
 */
public class BmFileOutputStream extends OutputStream {

    private static final String TAG = "BM_Fos";
	public static int BM_OBJECT_SIZE = (512 * 1024); //512 Kb
    // (16 * 1024 * 1024); /* 16 Mb Objects */

	private final Context mContext;

    private BmFileSystemFramework mBmFramework = null;
    private BmFileMetadata mFileMetaStore;
    private BmObject mBmObj;

    private BmFileChannel mFileChannel;

    public BmFileChannel getChannel() {
        return mFileChannel;
    }

    public long position() throws IOException {
        return mFileChannel.position();
    }

    public void position(long pos) throws IOException {
        mFileChannel.position(pos);
    }

    private void incrPosition(long pos) throws IOException {
        position(position() + pos);
    }

    public BmFileOutputStream(Context ctx) {
        mContext = ctx;
        mBmFramework = BmFileSystemFramework.getInstance();
        mFileMetaStore = BmFileMetadata.getInstance();
        mFileChannel = new BmFileChannel();
    }

    public BmFileOutputStream(Context ctx, String filename, int mode) {
        this(ctx);
        try {
            openFileOutput(filename, mode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(int oneByte) throws IOException {
        write(new byte[] {(byte) oneByte}, 0, 1);
    }

    /**
     * The stream of bytes is converted to a set of Objects, with a default size
     * of 16Mb per object. acquireObjectLock is called on all objects associated
     * with this write. This is a blocking call and has to be implemented by
     * the pluggable fs. An Object key is generated and the corresponding
     * metadata is recorded in a Filemeta data Objects have a flat file structure
     * Metadata stored in the object BmFileMetadata is updated locally. Finally
     * the write method of the framework is called
     *
     * @param buffer
     * @param byteOffset
     * @param byteCount
     * @throws IOException
     */

    @Override
    public void write(byte []buffer, int byteOffset, int byteCount) throws IOException {

        long filePos = position();

        int startObj = (int) (filePos / BM_OBJECT_SIZE);
        int endObj = (int) ((filePos + byteCount - 1) / BM_OBJECT_SIZE);
        int lastObjSz = (int) (((filePos + byteCount - 1) % BM_OBJECT_SIZE) + 1);
        Log.v(TAG, "start: " + startObj + " end " + endObj + " lastObjSz " + lastObjSz);

        if (endObj >= mBmObj.getNumberOfObjs()) {
            //new objects have to be added
            for (int i = mBmObj.getNumberOfObjs(); i < endObj; i++) {
                /* size of last object will be different */
                mBmObj.addObj(BmFileMetadata.generateObjectID(), BM_OBJECT_SIZE);
            }
            mBmObj.addObj(BmFileMetadata.generateObjectID(), lastObjSz);
        }

        /* First object */
        int firstObjOffset = (int) (filePos % BM_OBJECT_SIZE);

        int firstObjSz = BM_OBJECT_SIZE - firstObjOffset;
        if ((firstObjOffset + byteCount) < BM_OBJECT_SIZE) {
            firstObjSz = byteCount;
        }

        Log.v(TAG, "byteOffset = " + byteOffset + " first object offset: " + firstObjOffset + " first object size " + firstObjSz);

        int offset = byteOffset;
        //start: inclusive, end:exclusive
        byte [] buf = Arrays.copyOfRange(buffer, offset, offset + firstObjSz);
        offset += firstObjSz;
        if (firstObjSz < BM_OBJECT_SIZE) {
            // first Object is being updated
            updateObject(mBmObj.getObjAtIndex(startObj), buf, firstObjOffset, firstObjSz);
        } else {
            createObject(mBmObj.getObjAtIndex(startObj), buf, BM_OBJECT_SIZE);
        }

        /* for every object in the middle */
        for (int i = startObj + 1; i < endObj; i++) {
            Log.v(TAG, "Updating middle Objects");
            buf = Arrays.copyOfRange(buffer, offset, offset + BM_OBJECT_SIZE);
            createObject(mBmObj.getObjAtIndex(i), buf, BM_OBJECT_SIZE);
            offset += BM_OBJECT_SIZE;
        }

        /* Last object */
        Log.v(TAG, "Last object, from " + offset + " to: " + (offset + lastObjSz));
        buf = Arrays.copyOfRange(buffer, offset, offset + lastObjSz);
        if (lastObjSz < BM_OBJECT_SIZE) {
            updateObject(mBmObj.getObjAtIndex(endObj), buf, 0, lastObjSz);
        } else {
            createObject(mBmObj.getObjAtIndex(endObj), buf, BM_OBJECT_SIZE);
        }

        incrPosition(byteCount);
    }

    @Override
    public void flush() {
        //TODO: flush only objects affected by this instance
        mBmFramework.flush(mBmObj.getObjIds());
    }

    @Override
    public void close() {
        //TODO: close only objects affected by this instance
        flush();
        mBmFramework.close(mBmObj.getObjIds());
    }

    //TODO: getFD(), getChannel() and finalize()

    //FileOutputStream Context.openFileOutput (String name, int mode)
    //Use 0 or MODE_PRIVATE for the default operation. Use MODE_APPEND to append to an existing file
    public void openFileOutput(String filename, int mode) throws IOException {
        //TODO: respect mode
        long pos = 0;
        mBmObj = mFileMetaStore.getObjFromFilename(filename);
        if (mBmObj == null) {
            /* new file, create an empty file of size 0 */
            Log.v(TAG, "Creating new object");
            mBmObj = new BmObject();
            mBmObj.addObj(BmFileMetadata.generateObjectID(), 0);
            mFileMetaStore.createNewObj(filename, mBmObj);
        } else if (mode == Context.MODE_PRIVATE) {
            //TODO: All existing objects have to be pruned
        } else if (mode == Context.MODE_APPEND) {
            pos = mBmObj.getSize();
        } else {
            throw new IOException();
        }
        position(pos);
    }

    /* BlueMountain Framework interface functions */
    private void updateObject(String objId, byte[] buf, int off, int len) {
        Log.v(TAG, "Updating object on remote file-system, id: " + objId);
        long startTime = System.nanoTime();
        mBmFramework.fupdate(objId, buf, off, len);
        long difference = System.nanoTime() - startTime;
        Log.v(TAG, "Remote Write took: " + difference + "ns");
    }

    private void createObject(String objId, byte[] buffer, int len) {
        Log.v(TAG, "Creating object on remote file-system, id: " + objId);
        long startTime = System.nanoTime();
        mBmFramework.fcreate(objId, buffer, len);
        long difference = System.nanoTime() - startTime;
        Log.v(TAG, "Remote Write took: " + difference + "ns");
    }
}
