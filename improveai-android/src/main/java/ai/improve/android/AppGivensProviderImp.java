package ai.improve.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import ai.improve.util.GivensProvider;
import ai.improve.log.IMPLog;
import ai.improve.improveai_android.BuildConfig;

import static ai.improve.android.Constants.SP_File_Name;

/**
 * country - two letter code            // done
 * language - two letter code           // done
 * timezone - numeric GMT offset        // done
 * carrier                              // done
 * os - lower case                      // done
 * os_version                           // done
 * device                               // done
 * device_version
 * app                                  // done
 * app_version                          // done
 * build_version                        // done
 * improve_version == 6000              // done
 * screen_width                         // done
 * screen_height                        // done
 * screen_pixels == screen_width x screen_height                                // done
 * weekday (ISO 8601, monday==1.0, sunday==7.0) plus fractional part of day     // done
 * since_midnight                       // done
 * since_session_start                  // done
 * since_last_session_start             // done
 * since_born                           // done
 * session_count
 * decision_count
 *
 * Versions are major x 1000 + minor + build / 1000
 * Times are fractional seconds
 * Persist necessary values in NSUserDefaults/Android Context with “ai.improve.” key prefixes
 * */

public class AppGivensProviderImp implements GivensProvider {
    private static final String Tag = "AppGivensProviderImp";

    private static final String APP_Given_Key_Country = "country";
    private static final String APP_Given_Key_Language = "language";
    private static final String APP_Given_Key_Timezone_Offset = "timezone";
    private static final String APP_Given_Key_Carrier = "carrier";
    private static final String APP_Given_Key_OS = "os";
    private static final String APP_Given_Key_OS_Version = "os_version";
    private static final String APP_Given_Key_Device = "device";
    private static final String APP_Given_Key_Device_Version = "device_version";
    private static final String APP_Given_Key_App = "app";
    private static final String APP_Given_Key_App_Version = "app_version";
    private static final String APP_Given_Key_Build_Version = "build_version";
    private static final String APP_Given_Key_Improve_Version = "improve_version";

    private static final String APP_Given_Key_Screen_Width = "screen_width";
    private static final String APP_Given_Key_Screen_Height = "screen_height";
    private static final String APP_Given_Key_Screen_Pixels = "screen_pixels";
    private static final String APP_Given_Key_Week_Day = "week_day";
    private static final String APP_Given_Key_Since_Midnight = "since_midnight";
    private static final String APP_Given_Key_Since_Session_Start = "since_session_start";
    private static final String APP_Given_Key_Since_Last_Session_Start = "since_last_session_start";
    private static final String APP_Given_Key_Since_Born = "since_born";
    private static final String APP_Given_Key_Session_Count = "session_count";
    private static final String APP_Given_Key_Decision_Count = "decision_count";

    // SharedPreference key
    public static final String SP_Key_Born_Time = "born_time";
    public static final String SP_Key_Session_Start_Time = "session_start_time";
    public static final String SP_Key_Session_Count = "session_count";
    public static final String SP_Key_Decision_Count = "decision_count";

    private Context appContext;

//    /**
//     * Session start time is the moment when the first AppGivensProvider instance is created.
//     * */
//    private static long lastSessionStartTime = 0;

    private static boolean runOnce = false;

    public AppGivensProviderImp(Context context) {
        appContext = context.getApplicationContext();
        runOnceInSession(context);
    }

    private static synchronized void runOnceInSession(Context context) {
        if(!runOnce) {
            runOnce = true;

            SharedPreferences sp = context.getSharedPreferences("ai.improve", Context.MODE_PRIVATE);

            // Cache last session start time before it is covered by current session start time.
            AppGivensProviderUtils.setLastSessionStartTime(sp.getLong(SP_Key_Session_Start_Time, 0));

            // set born time
            long bornTime = sp.getLong(SP_Key_Born_Time, 0);
            if(bornTime == 0) {
                sp.edit().putLong(SP_Key_Born_Time, System.currentTimeMillis()).apply();
            }

            // save current session start time
            sp.edit().putLong(SP_Key_Session_Start_Time, System.currentTimeMillis()).apply();

            // increment session count value by 1
            int curSessionCount = sp.getInt(SP_Key_Session_Count, 0);
            sp.edit().putInt(SP_Key_Session_Count, curSessionCount+1).apply();
        }
    }

    @Override
    public Map<String, Object> getGivens() {
        Map<String, Object> appGivens = new HashMap();

        appGivens.put(APP_Given_Key_Country, getCountry());
        appGivens.put(APP_Given_Key_Language, getLanguage());
        appGivens.put(APP_Given_Key_Timezone_Offset, getGMTTimezoneOffset());
        appGivens.put(APP_Given_Key_Carrier, getCarrier());
        appGivens.put(APP_Given_Key_OS, getOS());
        appGivens.put(APP_Given_Key_OS_Version, getOsVersion());
        appGivens.put(APP_Given_Key_Device, getDevice());
        appGivens.put(APP_Given_Key_Device_Version, getDeviceVersion());
        appGivens.put(APP_Given_Key_App, getApp());
        appGivens.put(APP_Given_Key_App_Version, getAppVersion(appContext));
        appGivens.put(APP_Given_Key_Build_Version, getBuildVersion(appContext));
        appGivens.put(APP_Given_Key_Improve_Version, getImproveVersion());

        Point point = getScreenDimension(appContext);
        appGivens.put(APP_Given_Key_Screen_Width, point.x);
        appGivens.put(APP_Given_Key_Screen_Height, point.y);
        appGivens.put(APP_Given_Key_Screen_Pixels, point.x * point.y);

        appGivens.put(APP_Given_Key_Week_Day, getDayOfWeek());
        appGivens.put(APP_Given_Key_Since_Midnight, getSinceMidnight());
        appGivens.put(APP_Given_Key_Since_Session_Start, getSinceSessionStart());
        appGivens.put(APP_Given_Key_Since_Last_Session_Start, getSinceLastSessionStart());
        appGivens.put(APP_Given_Key_Since_Born, getSinceBorn());
        appGivens.put(APP_Given_Key_Session_Count, getSessionCount());
        appGivens.put(APP_Given_Key_Decision_Count, getDecisionCount());

        // increment decision count value by 1
        SharedPreferences sp = appContext.getSharedPreferences(SP_File_Name, Context.MODE_PRIVATE);
        int curDecisionCount = sp.getInt(SP_Key_Decision_Count, 0);
        sp.edit().putInt(SP_Key_Decision_Count, curDecisionCount+1).apply();

        IMPLog.d(Tag, "appGivens: " + appGivens.toString());

        return appGivens;
    }

    private String getCountry() {
        String country = getCountryFromNetwork();
        if (!TextUtils.isEmpty(country)) {
            return country;
        }
        return getCountryFromLocale();
    }

    private String getCountryFromNetwork() {
        try {
            TelephonyManager manager = (TelephonyManager) appContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                String country = manager.getNetworkCountryIso();
                if (country != null) {
                    return country.toUpperCase(Locale.US);
                }
            }
        } catch (Exception e) {
            // Failed to get country from network
        }
        return "";
    }

    private String getCountryFromLocale() {
        return Locale.getDefault().getCountry().toUpperCase(Locale.US);
    }

    private String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    private int getGMTTimezoneOffset() {
        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        return mTimeZone.getRawOffset() / 3600000;
    }

    private String getCarrier() {
        try {
            TelephonyManager manager = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
            return manager.getNetworkOperatorName();
        } catch (Exception e) {
            // Failed to get network operator name from network
        }
        return "";
    }

    private String getOS() {
        return "android";
    }

    private double getOsVersion() {
        return AppGivensProviderUtils.versionToInt(Build.VERSION.RELEASE);
    }

    private String getDevice() {
        if(!TextUtils.isEmpty(Build.MODEL) && Build.MODEL.contains(Build.MANUFACTURER)) {
            // There are devices that have manufacture included in the model field.
            return Build.MODEL;
        } else {
            return Build.MANUFACTURER + " " + Build.MODEL;
        }
    }

    private double getDeviceVersion() {
        return AppGivensProviderUtils.parseDeviceVersion(Build.MODEL);
    }

    public String getApp() {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(appContext.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return appContext.getResources().getString(labelRes);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return "";
    }

    /**
     * @return A string used as the version number shown to users.
     * The versionName has no purpose other than to be displayed to users.
     * It could be anything like: 1.0, 1.0.0, 1.0.0 alpha, alpha 1.0.0.
     * */
    private double getAppVersion(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return AppGivensProviderUtils.versionToInt(packageInfo.versionName);
        } catch (Exception e) {
        }
        return 0;
    }

    private double getBuildVersion(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode * 1000.0;
        } catch (Exception e) {
        }
        return 0;
    }

    private double getImproveVersion() {
        String version = BuildConfig.IMPROVE_AI_VERSION;
        return AppGivensProviderUtils.versionToInt(version);
    }

    private Point getScreenDimension(Context context) {
        Point point = new Point();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(displayMetrics);
        } else {
            display.getMetrics(displayMetrics);
        }
        point.x = displayMetrics.widthPixels;
        point.y = displayMetrics.heightPixels;
        return point;
    }

    private double getDayOfWeek() {
        // I have not found any framework method that can do the conversion more elegantly.
        Map weekDayMap = new HashMap() {
            {
                put(Calendar.MONDAY, 1);
                put(Calendar.TUESDAY, 2);
                put(Calendar.WEDNESDAY, 3);
                put(Calendar.THURSDAY, 4);
                put(Calendar.FRIDAY, 5);
                put(Calendar.SATURDAY, 6);
                put(Calendar.SUNDAY, 7);
            }
        };
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        int seconds = date.getHours() * 3600 + date.getMinutes() * 60 + date.getSeconds();
        return (int)weekDayMap.get(calendar.get(Calendar.DAY_OF_WEEK)) + seconds / 86400.0;
    }

    /**
     * @return Seconds since midnight with fractional millis
     * */
    private double getSinceMidnight() {
        Date date = new Date();
        double seconds = date.getHours() * 3600 + date.getMinutes() * 60 +
                date.getSeconds() + (date.getTime() % 1000)/1000.0;
        return seconds;
    }

    /**
     * Session start time is the moment when the first AppGivensProvider instance is created.
     * */
    private double getSinceSessionStart() {
        return AppGivensProviderUtils.getSinceSessionStart(appContext);
    }

    /**
     * @return 0, if there's no last session
     * */
    private double getSinceLastSessionStart() {
        return AppGivensProviderUtils.getSinceLastSessionStart();
    }

    private double getSinceBorn() {
        return AppGivensProviderUtils.getSinceBorn(appContext);
    }

    private int getSessionCount() {
        return AppGivensProviderUtils.getSessionCount(appContext);
    }

    // 0 is returned for the first session
    private int getDecisionCount() {
        return AppGivensProviderUtils.getDecisionCount(appContext);
    }
}
