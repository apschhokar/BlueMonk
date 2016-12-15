package edu.buffalo.rms.bluemountain.framework;

/**
 * Created by sharath on 9/7/16.
 */
public interface IBmFramework {
    //CREATE
    void fcreate(String objId, byte[] bytes, int len);
    //QUERY
    void fread(String objId, int objOff, byte[] buf, int bufPos);
    //UPDATE
    void fupdate(String objId, byte[] buf, int off, int len);
    //DELETE
    void fdelete(String objId);
}
