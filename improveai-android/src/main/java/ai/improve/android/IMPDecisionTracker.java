package ai.improve.android;

import android.content.Context;

public class IMPDecisionTracker extends BaseIMPDecisionTracker {
    public IMPDecisionTracker(Context context, String trackURL) {
        this(context, trackURL, null);
    }

    public IMPDecisionTracker(Context context, String trackURL, String apiKey) {
        super(trackURL, apiKey, new HistoryIdProviderImp(context));
    }
}
