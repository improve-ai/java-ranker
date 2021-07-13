package ai.improve.android;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;

public class Utils {

    private static Context sAppContext;

    private static Object getActivityThread() {
        Object activityThread = null;
        try {
            Method method = Class.forName("android.app.ActivityThread").getMethod("currentActivityThread");
            method.setAccessible(true);
            activityThread = method.invoke(null);
        } catch (final Exception e) {
        }
        return activityThread;
    }

    public static Context getAppContext() {
        if(sAppContext == null) {
            try {
                Object activityThread = getActivityThread();
                Object app = activityThread.getClass().getMethod("getApplication").invoke(activityThread);
                sAppContext = ((Application) app).getApplicationContext();
            } catch (Throwable e) {
//                throw new IllegalStateException("Can not access Application context by magic code, boom!", e);
            }
        }
        return sAppContext;
    }
}
