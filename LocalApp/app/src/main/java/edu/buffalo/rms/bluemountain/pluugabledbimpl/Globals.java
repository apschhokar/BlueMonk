package edu.buffalo.rms.bluemountain.pluugabledbimpl;

/**
 * Created by aniruddh on 12/1/16.
 */

public enum Globals {
    INSTANCE;
    public String NATIVE_LOGGING_MODE = "native";
    public static String FRAMEWORK_WITH_REMOTE_SQL = "frameworkWithRemoteSQL";
    public static String FRAMEWORK_WITH_BLOCKING_FILE_LOGGING = "frameworkWithBlockingFileLogging";
    public static String CURRENT_LOGGING_MODE = FRAMEWORK_WITH_BLOCKING_FILE_LOGGING;
    public static String TAG_TIME = "Time";
}
