package edu.buffalo.rms.bluemountain.oplog;


/**
 * Created by aniruddh on 10/15/2016.
 */

public class Utils {
    public static String getArgsString(String[] selectionArgs) {
        String result = "";
        if (null == selectionArgs || selectionArgs.length == 0 ) return result;
        for (int count = 0; count < selectionArgs.length; ++count) {
            if (count != selectionArgs.length - 1)
                result += selectionArgs[count] + ",";
            else
                result += selectionArgs[count];
        }
        return result;
    }

    public static String getArgsString(Object[] selectionArgs) {
        String result = "";
        if (null == selectionArgs) return "_null_";
        if (selectionArgs.length == 0) return result;
        for (int count = 0; count < selectionArgs.length; ++count) {
            if (count != selectionArgs.length - 1)
                result += String.valueOf(selectionArgs[count]) + ",";
            else
                result += String.valueOf(selectionArgs[count]);
        }
        return result;
    }

    public static String formatSql(String sql){
        return "sql :"+sql;
    }

    public static String formatMetadata(String metadata){
        return "metadata :" +metadata;
    }
}
