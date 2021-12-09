package ai.improve.log;

public class IMPLog {
    public static final String Prefix = "IMPROVE_AI.";

    public static final int LOG_LEVEL_ALL = 0;
    public static final int LOG_LEVEL_DEBUG = 3;
    public static final int LOG_LEVEL_WARN = 5;
    public static final int LOG_LEVEL_ERROR = 6;
    public static final int LOG_LEVEL_OFF = 7;

    /**
     * Logging is disabled by default.
     * Call setLogLevel() to turn it on.
     * */
    public static int sLogLevel = LOG_LEVEL_OFF;

    private static Logger sLogger;

    public static void setLogLevel(int level) {
        sLogLevel = level;
    }

    public static void d(String tag, String message) {
        if(LOG_LEVEL_DEBUG >= sLogLevel) {
            if(sLogger != null) {
                sLogger.d(Prefix+tag, message);
            } else {
                System.out.println(Prefix + tag + ", " + message);
            }
        }
    }

    public static void w(String tag, String message) {
        if(LOG_LEVEL_WARN >= sLogLevel) {
            if(sLogger != null) {
                sLogger.w(Prefix+tag, message);
            } else {
                System.out.println(Prefix + tag + ", " + message);
            }
        }
    }

    public static void e(String tag, String message) {
        if(LOG_LEVEL_ERROR >= sLogLevel) {
            if(sLogger != null) {
                sLogger.e(Prefix+tag, message);
            } else {
                System.out.println(Prefix + tag + ", " + message);
            }
        }
    }

    public static void setLogger(Logger logger) {
        sLogger = logger;
    }

    public interface Logger {
        void d(String tag, String message);
        void w(String tag, String message);
        void e(String tag, String message);
    }
}
