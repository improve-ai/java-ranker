package ai.improve.sample;

import android.app.Application;

import ai.improve.DecisionModel;
import ai.improve.log.IMPLog;

public class SampleApplication extends Application {
    public static final String Tag = "SampleApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        IMPLog.d(Tag, "onCreate, defaultTrackURL=" + DecisionModel.getDefaultTrackURL());
        // trackUrl is obtained from your Improve AI Gym configuration
    }
}
