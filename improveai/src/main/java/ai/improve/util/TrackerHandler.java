package ai.improve.util;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import ai.improve.DecisionTracker;
import ai.improve.log.IMPLog;

public class TrackerHandler {
    public static final String Tag = "IMPTrackerHandler";

    private static final String TYPE_KEY = "type";

    private static final String DECISION_TYPE = "decision";
    private static final String MODEL_KEY = "model";
    public static final String DECISION_BEST_KEY = "variant";
    public static final String COUNT_KEY = "count";
    private static final String GIVENS_KEY = "givens";
    private static final String RUNNERS_UP_KEY = "runners_up";
    public static final String SAMPLE_VARIANT_KEY = "sample";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String MESSAGE_ID_KEY = "message_id";
    private static final String PROPERTIES_KEY = "properties";
    private static final String EVENT_KEY = "event";


    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String API_KEY_HEADER = "x-api-key";

    private static final SimpleDateFormat ISO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);


    private static final String HISTORY_ID_KEY = "history_id";

    private static String historyId = "";

    public static String getHistoryId() {
        return historyId;
    }

    public static void setHistoryId(String historyId) {
        TrackerHandler.historyId = historyId;
    }

    public static <T> void track(DecisionTracker tracker, Object bestVariant, List<T> variants, Map<String, Object> givens,
                                 String modelName, boolean variantsRankedAndTrackRunnersUp) {
        if(modelName == null || modelName.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: modelName is empty or null");
            return ;
        }

        if(tracker.getTrackURL() == null || tracker.getTrackURL().isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: trackURL is empty or null");
            return ;
        }

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, modelName);

        setCount(variants, body);

        setBestVariant(bestVariant, body);

        if(givens != null) {
            body.put(GIVENS_KEY, givens);
        }

        List<T> runnersUp = null;
        if(variantsRankedAndTrackRunnersUp) {
            runnersUp = topRunnersUp(variants, tracker.getMaxRunnersUp());
            body.put(RUNNERS_UP_KEY, runnersUp);
        }

        int runnersUpCount = runnersUp == null ? 0 : runnersUp.size();
        setSampleVariant(variants, runnersUpCount, body);

        postTrackingRequest(tracker, body);
    }

    public static <T> List<T> topRunnersUp(List<T> ranked, int maxRunnersUp) {
        return ranked.subList(1, 1+Math.min(maxRunnersUp, ranked.size()-1));
    }

    /**
     * A null variant should appear as {"variant":null} when json encoded
     * */
    public static void setBestVariant(Object variant, Map<String, Object> body) {
        body.put(DECISION_BEST_KEY, variant);
    }

    // Move to separate method for unit test
    // "count" field should be 1 in case of null or empty variants
    public static <T> void setCount(List<T> variants, Map<String, Object> body) {
        if(variants == null || variants.size() <= 0) {
            body.put(COUNT_KEY, 1);
        } else {
            body.put(COUNT_KEY, variants.size());
        }
    }

    /**
     * Sample variant is selected from variants excluding runners-up and
     * the best variant
     *
     * If there are no runners up, then sample is a random sample from
     * variants with just best excluded.
     *
     * If there are runners up, then sample is a random sample from
     * variants with best and runners up excluded.
     *
     * If there is only one variant, which is the best, then there is no sample.
     *
     * If there are no remaining variants after best and runners up, then
     * there is no sample.
     *
     * If the sample variant itself is null, it should also be included in the body map.
     **/
    public static <T> void setSampleVariant(List<T> variants, int runnersUpCount, Map<String, Object> body) {
        if(variants == null || variants.size() <= 0) {
            return ;
        }

        T variant = null;
        int samplesCount = variants.size() - runnersUpCount - 1;
        if (samplesCount <= 0) {
            return ;
        }

        int randomIndex = new Random().nextInt(samplesCount) + runnersUpCount + 1;
        variant = variants.get(randomIndex);
        body.put(SAMPLE_VARIANT_KEY, variant);
    }

    public static void postTrackingRequest(DecisionTracker tracker, Map<String, Object> body) {
        if (historyId == null || historyId.isEmpty()) {
            IMPLog.e(Tag, "historyId cannot be null");
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        if (tracker.getApiKey() != null) {
            headers.put(API_KEY_HEADER, tracker.getApiKey());
        }
        String timestamp = ISO_TIMESTAMP_FORMAT.format(new Date());

        body = new HashMap<>(body);
        body.put(TIMESTAMP_KEY, timestamp);
        body.put(HISTORY_ID_KEY, historyId);
        body.put(MESSAGE_ID_KEY, UUID.randomUUID().toString());

        Map<String, Object> finalBody = body;
        new Thread() {
            @Override
            public void run() {
                try {
                    // android.os.NetworkOnMainThreadException will be thrown if post() is called
                    // in main thread
                    HttpUtil.withUrl(tracker.getTrackURL()).withHeaders(headers).withBody(finalBody).post();
                } catch (MalformedURLException e) {
                    IMPLog.e(Tag, e.getLocalizedMessage());
                }
            }
        }.start();
    }

    public static void trackEvent(DecisionTracker tracker, String eventName) {
        trackEvent(tracker, eventName, null);
    }

    public static void trackEvent(DecisionTracker tracker, String eventName, Map<String, Object> properties) {
        if(tracker.getTrackURL() == null || tracker.getTrackURL().isEmpty()) {
            IMPLog.w(Tag, "trackURL is empty or nil, event won't be tracked.");
            return ;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("type", "event");
        if (eventName != null) {
            body.put(EVENT_KEY, eventName);
        }
        if (properties != null) {
            body.put(PROPERTIES_KEY, properties);
        }

        postTrackingRequest(tracker, body);
    }
}
