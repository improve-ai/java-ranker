package ai.improve;

import java.util.Map;

public abstract class BaseDecisionTracker {
    public static final String Tag = "BaseDecisionTracker";

    private String trackURL;

    private String apiKey;

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp;

    public BaseDecisionTracker(String trackURL, HistoryIdProvider historyIdProvider) {
        this(trackURL, null, historyIdProvider);
    }

    public BaseDecisionTracker(String trackURL, String apiKey, HistoryIdProvider historyIdProvider) {
        this.maxRunnersUp = 50;
        this.trackURL = trackURL;
        this.apiKey = apiKey;

        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.e(Tag, "trackURL is empty or null, tracking disabled");
        }

        String historyId = historyIdProvider.getHistoryId();
        IMPLog.d(Tag, "historyId=" + historyId);

        IMPTrackerHandler.setHistoryId(historyId);
    }

    public String getTrackURL() {
        return trackURL;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getMaxRunnersUp() {
        return maxRunnersUp;
    }

    /**
     * @param maxRunnersUp  Hyperparameter that affects training speed and model performance.
     *                      Values from 10-100 are probably reasonable.
     *                      0 disables runners up tracking
     * */
    public void setMaxRunnersUp(int maxRunnersUp) {
        this.maxRunnersUp = maxRunnersUp;
    }


    public void trackEvent(String eventName) {
        trackEvent(eventName, null);
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        IMPTrackerHandler.trackEvent(this, eventName, properties);
    }
}
