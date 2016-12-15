package edu.buffalo.rms.bluemountain.shim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by sharath on 7/19/16.
 */
//TODO: Implement persistent metadata store

public class BmFileMetadata {
    private static BmFileMetadata mInstance;
    private Map<String, BmObject> mFileToObj = new HashMap<String, BmObject>();

    protected BmFileMetadata() {
        return;
    }

    public static BmFileMetadata getInstance() {
        if (mInstance == null) {
            mInstance = new BmFileMetadata();
        }
        return mInstance;
    }
    /*
    Retrieve the object from the hashmap if present,
    if not, return null.
    If null is returned, a new object should be created, by
    passing the filename and the number of objects to create
     */
    public BmObject getObjFromFilename(String filename) {
        return mFileToObj.get(filename);
    }

    public void createNewObj(String filename, BmObject obj) {
        mFileToObj.put(filename, obj);
    }

    static String generateObjectID() {
        Long id = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        return id.toString();
    }
    /*

    String filename;
    int objBlockSz;
    int lastObjsz;
    BmObject [] bmObj;
    String metaDataPath;

    public BmFileMetadata(String metaDataPath) {
        this.metaDataPath = metaDataPath;
    }

    public void pushToFile() {
        try {
            FileOutputStream fos = new FileOutputStream(metaDataPath + fileToMetadaFile());
            fos.write(filename.getBytes());
            fos.write(ByteBuffer.allocate(4).putInt(objBlockSz).array());
            fos.write(ByteBuffer.allocate(4).putInt(lastObjsz).array());
            fos.write(bmObj[0].getId().getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String fileToMetadaFile() {
        return filename + ".meta";
    }
    */
}
