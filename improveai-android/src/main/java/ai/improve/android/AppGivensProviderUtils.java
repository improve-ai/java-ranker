package ai.improve.android;

import static ai.improve.android.AppGivensProvider.SP_Key_Born_Time;
import static ai.improve.android.AppGivensProvider.SP_Key_Decision_Count;
import static ai.improve.android.AppGivensProvider.SP_Key_Model_Reward;
import static ai.improve.android.Constants.Improve_SP_File_Name;

import android.app.Presentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import ai.improve.ImproveContentProvider;
import ai.improve.log.IMPLog;

public class AppGivensProviderUtils {
    public static final String Tag = "AppGivensProviderUtils";

    private static final double MillisSecondsPerDay = 86400000.0;

    public static final double SecondsPerDay = 86400.0;

    public static String getDecisionCountKeyOfModel(String modelName) {
        return String.format(SP_Key_Decision_Count, modelName);
    }

    public static int getDecisionCount(Context context, String modelName) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        String decisionCountKey = getDecisionCountKeyOfModel(modelName);
        return sp.getInt(decisionCountKey, 0);
    }

    public static double getSinceSessionStart() {
        long sessionStartTime = ImproveContentProvider.getSessionStartTime();
        return (System.currentTimeMillis() - sessionStartTime) / MillisSecondsPerDay;
    }

    public static double getSinceBorn(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        long bornTime = sp.getLong(SP_Key_Born_Time, 0);
        return (System.currentTimeMillis() - bornTime) / MillisSecondsPerDay;
    }

    /**
     * @param model Build.MODEL
     * @return 0, if Build.MODEL is null or empty
     * */
    public static double parseDeviceVersion(String model) {
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
                return major;
            } else if (versionArray.length == 2) {
                int major = Integer.parseInt(versionArray[0]);
                int minor = Integer.parseInt(versionArray[1]);
                return major + minor / 1000.0;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    /**
     * @return 0, if version string is null or empty
     * */
    public static double versionToNumber(String versionString) {
        if(TextUtils.isEmpty(versionString)) {
            return 0;
        }

        double version = 0;
        String[] versionArray = versionString.split("\\.");
        if(versionArray.length == 1) {
            try {
                int major = Integer.parseInt(versionArray[0]);
                version = major;
            } catch (Throwable t){
                IMPLog.e(Tag, versionString + ", versionToInt error, " + t.getLocalizedMessage());
            }
        } else if(versionArray.length == 2) {
            try {
                int major = Integer.parseInt(versionArray[0]);
                int minor = Integer.parseInt(versionArray[1]);
                version = major + minor/1000.0;
            } catch (Throwable t) {
                IMPLog.e(Tag, versionString + ", versionToInt error, " + t.getLocalizedMessage());
            }
        } else if(versionArray.length >= 3) {
            try {
                int major = Integer.parseInt(versionArray[0]);
                int minor = Integer.parseInt(versionArray[1]);
                int build = Integer.parseInt(versionArray[2]);
                version = major + minor/1000.0 + build/1000000.0;
            } catch (Throwable t) {
                IMPLog.e(Tag, versionString + ", versionToInt error, " + t.getLocalizedMessage());
            }
        }
        return Math.round(version * 1000000) / 1000000.0;
    }

    public static void addRewardForModel(Context context, String modelName, double reward) {
        String key = String.format(SP_Key_Model_Reward, modelName);
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        double curReward = Double.longBitsToDouble(sp.getLong(key, 0));
        sp.edit().putLong(key, Double.doubleToLongBits(curReward + reward)).apply();
    }

    public static double rewardOfModel(Context context, String modelName) {
        String key = String.format(SP_Key_Model_Reward, modelName);
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        return Double.longBitsToDouble(sp.getLong(key, 0));
    }

    public static double roundedRewardOfModel(Context context, String modelName) {
        double reward = rewardOfModel(context, modelName);
        return Math.round(reward * 1000000) / 1000000.0;
    }

    public static double rewardsPerDecision(Context context, String modelName) {
        int decisionCount = getDecisionCount(context, modelName);
        double rewards = rewardOfModel(context, modelName);
        double result = decisionCount == 0 ? 0 : (rewards / decisionCount);
        return Math.round(result * 1000000) / 1000000.0;
    }

    public static double decisionsPerDay(Context context, String modelName) {
        int decisionCount = getDecisionCount(context, modelName);
        double days = getSinceBorn(context);
        double result =  decisionCount / days;
        return Math.round(result * 1000000) / 1000000.0;
    }
}
