package edu.buffalo.rms.bluemountain.pluugabledb;

import android.database.Cursor;

/**
 * Created by aniruddh on 12/3/16.
 */

public interface IBmPluggableSQLDB {
    public Object execute(String args[], String sql, String returnType);
    public Cursor executeCursor(String args[], String sql, String returnType);
}
