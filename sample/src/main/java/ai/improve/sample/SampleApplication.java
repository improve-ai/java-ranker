package ai.improve.sample;

import android.app.Application;

import ai.improve.DecisionModel;
import ai.improve.log.IMPLog;

public class SampleApplication extends Application {
    public static final String Tag = "SampleApplication";

    public static final String Track_URL = "https://d97zv0mo3g.execute-api.us-east-2.amazonaws.com/track";

    @Override
    public void onCreate() {
        super.onCreate();
        IMPLog.d(Tag, "onCreate");
        DecisionModel.defaultTrackURL = Track_URL;
    }
}
