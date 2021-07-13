package ai.improve;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class DecisionTracker {
    public static final String Tag = "BaseDecisionTracker";

    private String trackURL;

    private String apiKey;

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp;

    public DecisionTracker(String trackURL) {
        this(trackURL, null);
    }

    public DecisionTracker(String trackURL, String apiKey) {
        this.maxRunnersUp = 50;
        this.trackURL = trackURL;
        this.apiKey = apiKey;

        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.e(Tag, "trackURL is empty or null, tracking disabled");
        }

        String historyId = getHistoryId();
        IMPLog.d(Tag, "historyId = " + historyId);

        TrackerHandler.setHistoryId(historyId);
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
        if(Utils.isAndroid()) {
            try {
                Class clz = Class.forName("ai.improve.android.HistoryIdProviderImp");
                Object o = clz.newInstance();

                Method method = clz.getDeclaredMethod("getHistoryId");
                String historyId = (String)method.invoke(o);
                return historyId;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
