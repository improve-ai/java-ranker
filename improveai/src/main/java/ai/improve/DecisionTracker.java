package ai.improve;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import ai.improve.log.IMPLog;
import ai.improve.util.TrackerHandler;
import ai.improve.util.Utils;

public class DecisionTracker {
    public static final String Tag = "BaseDecisionTracker";

    private String trackURL;

    private String apiKey;

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp;

    /**
     * Android only
     * */
    public DecisionTracker(String trackURL) {
        this(trackURL, null);
    }

    /**
     * Android only
     * */
    public DecisionTracker(String trackURL, String apiKey) {
        this(trackURL, apiKey, null);
    }

    /**
     * History id must be set for non-Android platforms, so this is the only
     * valid constructor method for them.
     * */
    public DecisionTracker(String trackURL, String apiKey, String historyId) {
        this.maxRunnersUp = 50;
        this.trackURL = trackURL;
        this.apiKey = apiKey;

        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.e(Tag, "trackURL is empty or null, tracking disabled");
        }

        if(Utils.isAndroid()) {
            String id = getHistoryId();
            if(Utils.isEmpty(id)) {
                throw new RuntimeException("Fatal error, history id must not be null or empty");
            }
            TrackerHandler.setHistoryId(id);
        } else {
            // history id must be set for non-Android platform
            if(Utils.isEmpty(historyId)) {
                throw new RuntimeException("Fatal error, history id must not be null or empty");
            }
            TrackerHandler.setHistoryId(historyId);
        }
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
        this.maxRunnersUp = maxRunnersUp >= 0 ? maxRunnersUp : 0;
    }


    public void trackEvent(String eventName) {
        trackEvent(eventName, null);
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        TrackerHandler.trackEvent(this, eventName, properties);
    }

    private String getHistoryId() {
        try {
            Class clz = Class.forName("ai.improve.android.HistoryIdProviderImp");
            Object o = clz.newInstance();

            Method method = clz.getDeclaredMethod("getHistoryId");
            String historyId = (String)method.invoke(o);
            return historyId;
        } catch (InstantiationException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (ClassNotFoundException e) {
        }
        return "";
    }
}
