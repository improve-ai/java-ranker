package ai.improve;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class IMPTrackerHandler {
    public static final String Tag = "IMPTrackerHandler";

    private static final String TYPE_KEY = "type";

    private static final String DECISION_TYPE = "decision";
    private static final String MODEL_KEY = "model";
    private static final String DECISION_BEST_KEY = "variant";
    private static final String COUNT_KEY = "count";
    private static final String GIVEN_KEY = "given";
    private static final String RUNNERS_UP_KEY = "runners_up";
    private static final String SAMPLE_VARIANT_KEY = "sample";
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
        IMPTrackerHandler.historyId = historyId;
    }

    public static <T> void track(BaseDecisionTracker tracker, Object bestVariant, List<T> variants, Map<String, Object> givens,
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
        body.put(COUNT_KEY, variants == null ? 0 : variants.size());

        setBestVariant(bestVariant, body);

        if(givens != null) {
            body.put(GIVEN_KEY, givens);
        }

        List<T> runnersUp = null;
        if(variantsRankedAndTrackRunnersUp) {
            runnersUp = topRunnersUp(variants, tracker.getMaxRunnersUp());
            body.put(RUNNERS_UP_KEY, runnersUp);
        }

        Object sampleVariant = sampleVariant(variants, runnersUp == null ? 0 : runnersUp.size());
        if(sampleVariant != null) {
            body.put(SAMPLE_VARIANT_KEY, sampleVariant);
        }

        postTrackingRequest(tracker, body);
    }

    // If there are no runners up, then sample is a random sample from
    // variants with just best excluded.
    //
    // If there are runners up, then sample is a random sample from
    // variants with best and runners up excluded.
    //
    // If there is only one variant, which is the best, then there is no sample.
    //
    // If there are no remaining variants after best and runners up, then
    // there is no sample.
    public static <T> T sampleVariant(List<T> variants, int runnersUpCount) {
        if(variants == null) {
            return null;
        }

        // Sample variant is selected from variants excluding runners-up and the
        // best variant
        T variant = null;
        int samplesCount = variants.size() - runnersUpCount - 1;
        if (samplesCount > 0) {
            int randomIndex = new Random().nextInt(samplesCount) + runnersUpCount + 1;
            variant = variants.get(randomIndex);
        }
        return variant;
    }

    public static <T> List<T> topRunnersUp(List<T> ranked, int maxRunnersUp) {
        return ranked.subList(1, 1+Math.min(maxRunnersUp, ranked.size()-1));
    }

    // Notice:
    // Putting null into hashmap won't throw an NullPointerException; It appears as
    // {"variant":null} when json encoded.
    public static void setBestVariant(Object variant, Map<String, Object> body) {
        if(variant != null) {
            body.put(DECISION_BEST_KEY, variant);
            // case 1: variants is empty
        } else {
            // This happens only in two cases
            // case 1: variants is empty
            // case 2: variants is nil
            body.put(COUNT_KEY, 1);
            body.put(DECISION_BEST_KEY, null);
        }
    }

    public static void postTrackingRequest(BaseDecisionTracker tracker, Map<String, Object> body) {
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
        body.put(MESSAGE_ID_KEY, UUID.randomUUID());

        try {
            HttpUtil.withUrl(tracker.getTrackURL()).withHeaders(headers).withBody(body).post();
        } catch (MalformedURLException e) {
            IMPLog.e(Tag, e.getLocalizedMessage());
        }
    }

    public static void trackEvent(BaseDecisionTracker tracker, String eventName) {
        trackEvent(tracker, eventName, null);
    }

    public static void trackEvent(BaseDecisionTracker tracker, String eventName, Map<String, Object> properties) {
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
