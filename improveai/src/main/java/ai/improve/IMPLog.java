package ai.improve;

public class IMPLog {
    public static final String Prefix = "IMPROVE_AI.";

    private static Logger sLogger;

    private static boolean sLogEnabled = false;

    public static void setLogger(Logger logger) {
        sLogger = logger;
    }

    public static void enableLogging(boolean enabled) {
        sLogEnabled = enabled;
    }

    public static void d(String tag, String message){
        if(sLogger != null && sLogEnabled) {
            sLogger.d(Prefix+tag, message);
        }
    }

    public static void w(String tag, String message){
        if(sLogger != null && sLogEnabled) {
            sLogger.w(Prefix+tag, message);
        }
    }

    public static void e(String tag, String message){
        if(sLogger != null && sLogEnabled) {
            sLogger.e(Prefix+tag, message);
        }
    }

    public interface Logger {
        void d(String tag, String message);
        void w(String tag, String message);
        void e(String tag, String message);
    }
}
