package ai.improve.android;

import android.content.Context;

import ai.improve.BaseDecisionTracker;

public class DecisionTracker extends BaseDecisionTracker {
    public DecisionTracker(Context context, String trackURL) {
        this(context, trackURL, null);
    }

    public DecisionTracker(Context context, String trackURL, String apiKey) {
        super(trackURL, apiKey, new HistoryIdProviderImp(context));
    }
}
