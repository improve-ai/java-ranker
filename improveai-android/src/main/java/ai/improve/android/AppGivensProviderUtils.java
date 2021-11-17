package ai.improve.android;

import static ai.improve.android.AppGivensProvider.SP_Key_Born_Time;
import static ai.improve.android.AppGivensProvider.SP_Key_Decision_Count;
import static ai.improve.android.AppGivensProvider.SP_Key_Session_Count;
import static ai.improve.android.AppGivensProvider.SP_Key_Session_Start_Time;
import static ai.improve.android.Constants.Improve_SP_File_Name;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import ai.improve.log.IMPLog;

public class AppGivensProviderUtils {
    public static final String Tag = "AppGivensProviderUtils";

    /**
     * Session start time is the moment when the first AppGivensProvider instance is created.
     * */
    private static long sLastSessionStartTime = 0;

    public static void setLastSessionStartTime(long lastSessionStartTime) {
        sLastSessionStartTime = lastSessionStartTime;
    }

    public static int getSessionCount(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        int count = sp.getInt(SP_Key_Session_Count, 0);
        return count - 1 >= 0 ? count - 1 : 0;
    }

    public static String getDecisionCountKeyOfModel(String modelName) {
        return String.format(SP_Key_Decision_Count, modelName);
    }

    public static int getDecisionCount(Context context, String modelName) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        String decisionCountKey = getDecisionCountKeyOfModel(modelName);
        return sp.getInt(decisionCountKey, 0);
    }

    public static double getSinceSessionStart(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        long sessionStartTime = sp.getLong(SP_Key_Session_Start_Time, 0);
        return (System.currentTimeMillis() - sessionStartTime) / 1000.0;
    }

    /**
     * @return 0, if there's no last session
     * */
    public static double getSinceLastSessionStart() {
        if(sLastSessionStartTime == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - sLastSessionStartTime) / 1000.0;
    }

    public static double getSinceBorn(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        long bornTime = sp.getLong(SP_Key_Born_Time, 0);
        return (System.currentTimeMillis() - bornTime) / 1000.0;
    }

    /**
     * @param model Build.MODEL
     * @return 0, if Build.MODEL is null or empty
     * */
    public static int parseDeviceVersion(String model) {
        if (TextUtils.isEmpty(model)) {
            return 0;
        }
        try {
            // find index of the first number
            int firstNumberIndex = -1;
            for (int i = 0; i < model.length(); ++i) {
                char c = model.charAt(i);
                if (c >= '0' && c <= '9') {
                    firstNumberIndex = i;
                    break;
                }
            }
            if (firstNumberIndex == -1) {
                return 0;
            }

            boolean hasFoundDot = false;
            int lastNumberIndex = firstNumberIndex;
            for (int i = firstNumberIndex + 1; i < model.length(); ++i) {
                char c = model.charAt(i);
                if (c == '.') {
                    if(hasFoundDot) {
                        // Multiple dots found
                        break;
                    } else {
                        hasFoundDot = true;
                        lastNumberIndex = i;
                        continue;
                    }
                } else if (c >= '0' && c <= '9') {
                    lastNumberIndex = i;
                    continue;
                }
                break;
            }

            String versionString = model.substring(firstNumberIndex, lastNumberIndex + 1);

            String[] versionArray = versionString.split("\\.");
            if (versionArray.length == 1) {
                int major = Integer.parseInt(versionArray[0]);
                return major * 1000;
            } else if (versionArray.length == 2) {
                int major = Integer.parseInt(versionArray[0]);
                int minor = Integer.parseInt(versionArray[1]);
                return major * 1000 + minor;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    /**
     * @return 0, if version string is null or empty
     * */
    public static double versionToInt(String versionString) {
        if(TextUtils.isEmpty(versionString)) {
            return 0;
        }

        String[] versionArray = versionString.split("\\.");
        if(versionArray.length == 1) {
            try {
                int major = Integer.parseInt(versionArray[0]);
                return major * 1000;
            } catch (Throwable t){
                IMPLog.e(Tag, versionString + ", versionToInt error, " + t.getLocalizedMessage());
            }
        } else if(versionArray.length == 2) {
            try {
                int major = Integer.parseInt(versionArray[0]);
                int minor = Integer.parseInt(versionArray[1]);
                return major * 1000 + minor;
            } catch (Throwable t) {
                IMPLog.e(Tag, versionString + ", versionToInt error, " + t.getLocalizedMessage());
            }
        } else if(versionArray.length >= 3) {
            try {
                int major = Integer.parseInt(versionArray[0]);
                int minor = Integer.parseInt(versionArray[1]);
                int build = Integer.parseInt(versionArray[2]);
                return major * 1000 + minor + build/1000.0;
            } catch (Throwable t) {
                IMPLog.e(Tag, versionString + ", versionToInt error, " + t.getLocalizedMessage());
            }
        }
        return 0;
    }
}
