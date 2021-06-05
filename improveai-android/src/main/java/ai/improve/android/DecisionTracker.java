package ai.improve.android;

import android.content.Context;

import ai.improve.BaseDecisionTracker;
import ai.improve.IMPLog;

public class DecisionTracker extends BaseDecisionTracker {
    public DecisionTracker(Context context, String trackURL) {
        this(context, trackURL, null);
    }

    public DecisionTracker(Context context, String trackURL, String apiKey) {
        super(trackURL, apiKey, new HistoryIdProviderImp(context));
    }

    /**
     * The sdk does not have any init method, yet I have to setup the logger somewhere...
     * So here it goes.
     * */
    static {
        IMPLog.setLogger(new LoggerImp());
    }
}
