package edu.buffalo.rms.bluemountain.shim;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sharath on 7/19/16.
 */
public class BmObject {
    //TODO: Metadata files should be uploaded to the cloud as well
    public  BmObject() {
        //TODO: Load from file and populate the has table
    }
    private List<String> objIds = new ArrayList<>();
    int sizeOfLastObj = 0;

    public List<String> getObjIds() {
        return objIds;
    }

    public void addObj(String id, int size) {
        objIds.add(id);
        sizeOfLastObj = size;
    }
    public void delLastObj() {
        objIds.remove(objIds.size());
    }

    public String getObjAtIndex(int index) {
        return objIds.get(index);
    }

    public void setSizeOfLastObj(int size) {
        sizeOfLastObj = size;
    }

    public int getNumberOfObjs() {
        return objIds.size();
    }

    public int getSize() {
        int size = (objIds.size() - 1) * BmFileOutputStream.BM_OBJECT_SIZE + sizeOfLastObj;
        return size;
    }
}
