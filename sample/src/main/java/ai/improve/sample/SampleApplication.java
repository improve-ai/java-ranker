package ai.improve.sample;

import android.app.Application;

import ai.improve.IMPLog;

public class SampleApplication extends Application {
    public static final String Tag = "SampleApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        IMPLog.d(Tag, "onCreate");
    }
}
