package edu.buffalo.rms.bluemountain.pluugabledbimpl;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.buffalo.rms.bluemountain.pluugabledb.IBmPluggableSQLDB;

/**
 * Created by aniruddh on 12/3/16.
 */

public class BmPluggableSQLDBImpl implements IBmPluggableSQLDB {
    private SQLiteDatabase sqLiteDB;
    private String TAG = BmPluggableSQLDBImpl.class.getSimpleName();
    public BmPluggableSQLDBImpl(){

    }

    public BmPluggableSQLDBImpl(SQLiteDatabase db){
        this.sqLiteDB = db;
    }

    @Override
    public Object execute(String[] args, String sql, String returnType) {
        Log.d(TAG,"inside execute " + sql);
        if(sql.startsWith("sql")){
            sql = getExecutableSQLStatement(sql);
        }
        Log.d(TAG,"SQL to execute " + sql);
        Cursor cursor = sqLiteDB.rawQuery(sql,null);
        int count = cursor.getCount();
        if(returnType.equals(Long.class.getSimpleName()))
            return new Long(count);
        return count;
    }

    private String getExecutableSQLStatement(String sql){
        sql = sql.replaceAll("\\n","").trim();
        String [] splitArray = sql.split(":");
        String type = splitArray[0].trim();
        switch (type){
            case "metadata" : return metadataHandler(sql);
            case "sql" : return sqlHandler(sql, splitArray );
            default:
                System.out.println("Unknown type");
                break;
        }
        return null;
    }

    private String metadataHandler(String sql){
        return  null;
    }

    private String insertHandler(String sql){
        Log.d(TAG,"inside InsertHandler");
        String[] splitArray = sql.split(":");
        String insertString = splitArray[1].replace("bindArgs","").trim();
        int toReplace = insertString.split("\\?").length -1;
        if(toReplace == splitArray[2].split(",").length){
            String[] bindArgsArray = splitArray[2].trim().split(",");
            int anotherCount = 0;
            for(int count =0; count < toReplace; ++count){
                String replace = "";
                insertString = insertString.replaceFirst("\\?",isNumeric(bindArgsArray[anotherCount]) ? bindArgsArray[anotherCount] : "\"" +  bindArgsArray[anotherCount] + "\""  ) ;
                anotherCount++;
            }
        }
        return insertString.trim();
    }

    private boolean isNumeric(String str){
        try{
            Double no = Double.parseDouble(str);
            return true;
        }catch (NumberFormatException ex){
            return false;
        }
    }
    private String sqlHandler(String sql, String[] splitArray ){
        String command = splitArray[1];
        String insertMatcher = new String("INSERT").toLowerCase();
        if(command.toLowerCase().startsWith(insertMatcher)){
            return insertHandler(sql);
        }
        return  null;
    }

    @Override
    public Cursor executeCursor(String[] args, String sql, String returnType) {
        Log.d(TAG,"inside executeCursor");
        Cursor cursor = sqLiteDB.rawQuery(sql,null);
        return cursor;
    }
}
