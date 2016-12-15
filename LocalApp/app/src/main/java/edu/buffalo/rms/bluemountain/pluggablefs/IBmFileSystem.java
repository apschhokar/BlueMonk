package edu.buffalo.rms.bluemountain.pluggablefs;

/**
 * Created by sharath on 5/6/16.
 */
public interface IBmFileSystem {
    void initFS();

    //CREATE
    void fcreate(String objId, byte[] bytes, int len);
    //QUERY
    void fread(String objId, int objOff, byte[] buf, int bufPos);
    //UPDATE
    void fupdate(String objId, byte[] buf, int off, int len);
    //DELETE
    void fdelete(String objId);

    void flush(String id);

    void onClose(String id);

//    byte[] preProcessObject(byte[] buf);
//    BmChunk[] getChunksFromObject(String objId, byte[] bytes, int off);
//    void acquireObjectLock(String objId);
//    void onUpload(String chunkId, String status);
//    void uploadDbTableFileToServer();
//    void flushChunk(String chunkId);
//    byte[] delta(byte[] oldBytes, byte[] newBytes);
//    void delete(String objId);
//    byte[] read(String chunkId);
//    void onClose(String ObjId);
//    void flush(String ObjId);
//    boolean implementsCache();
}
