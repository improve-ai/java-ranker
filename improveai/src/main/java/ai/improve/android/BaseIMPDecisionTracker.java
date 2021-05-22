package ai.improve.android;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public abstract class BaseIMPDecisionTracker {
    public static final String Tag = "IMPDecisionTracker";

    static final String TYPE_KEY = "type";

    String DECISION_TYPE = "decision";
    String MODEL_KEY = "model";
    String DECISION_BEST_KEY = "variant";
    String COUNT_KEY = "count";
    String GIVEN_KEY = "given";
    String RUNNERS_UP_KEY = "runners_up";
    String SAMPLE_VARIANT_KEY = "sample";
    String TIMESTAMP_KEY = "timestamp";
    String MESSAGE_ID_KEY = "message_id";
    String PROPERTIES_KEY = "properties";
    String EVENT_KEY = "event";


    String CONTENT_TYPE_HEADER = "Content-Type";
    String APPLICATION_JSON = "application/json";
    String API_KEY_HEADER = "x-api-key";

    private static final SimpleDateFormat ISO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);


    static final String HISTORY_ID_KEY = "history_id";

    private String trackURL;

    private String apiKey;

    private String historyId;

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp;

    public BaseIMPDecisionTracker(String trackURL, HistoryIdProvider historyIdProvider) {
        this(trackURL, null, historyIdProvider);
    }

    public int getMaxRunnersUp() {
        return maxRunnersUp;
    }

    public void setMaxRunnersUp(int maxRunnersUp) {
        this.maxRunnersUp = maxRunnersUp;
    }

    public BaseIMPDecisionTracker(String trackURL, String apiKey, HistoryIdProvider historyIdProvider) {
        this.maxRunnersUp = 50;
        this.trackURL = trackURL;
        this.apiKey = apiKey;

        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.e(Tag, "trackURL is empty or null, tracking disabled");
        }

        this.historyId = historyIdProvider.getHistoryId();
    }

    public boolean shouldtrackRunnersUp(int variantsCount) {
        if(variantsCount <= 1 || this.maxRunnersUp == 0) {
            return false;
        }
        return Math.random() < 1.0 / Math.min(variantsCount - 1, this.maxRunnersUp);
    }

    public <T> List<T> topRunnersUp(List<T> ranked) {
        return ranked.subList(1, 1+Math.min(this.maxRunnersUp, ranked.size()-1));
    }

    public void track(Object bestVariant, List<Object> variants, Map<String, Object> givens,
                      String modelName, boolean variantsRankedAndTrackRunnersUp) {
        if(modelName == null || modelName.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: modelName is empty or nil");
            return ;
        }

        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: trackURL is empty or nil");
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

        List<Object> runnersUp = null;
        if(variantsRankedAndTrackRunnersUp) {
            runnersUp = topRunnersUp(variants);
            body.put(RUNNERS_UP_KEY, runnersUp);
        }

        Object sampleVariant = sampleVariant(variants, runnersUp == null ? 0 : runnersUp.size());
        if(sampleVariant != null) {
            body.put(SAMPLE_VARIANT_KEY, sampleVariant);
        }

        postTrackingRequest(trackURL, body);
    }

    // Notice:
    // Putting null into hashmap won't throw an NullPointerException; It appears as
    // {"variant":null} when json encoded.
    private void setBestVariant(Object variant, Map<String, Object> body) {
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
    private Object sampleVariant(List<Object> variants, int runnersUpCount) {
        if(variants == null) {
            return null;
        }

        // Sample variant is selected from variants excluding runners-up and the
        // best variant
        Object variant = null;
        int samplesCount = variants.size() - runnersUpCount - 1;
        if (samplesCount > 0) {
            int randomIndex = new Random().nextInt(samplesCount) + runnersUpCount + 1;
            variant = variants.get(randomIndex);
        }
        return variant;
    }

    private void postTrackingRequest(String trackUrl, Map<String, Object> body) {
        if (this.historyId == null || this.historyId.isEmpty()) {
            IMPLog.e(Tag, "historyId cannot be null");
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        if (this.apiKey != null) {
            headers.put(API_KEY_HEADER, this.apiKey);
        }
        String timestamp = ISO_TIMESTAMP_FORMAT.format(new Date());

        body = new HashMap<>(body);
        body.put(TIMESTAMP_KEY, timestamp);
        body.put(HISTORY_ID_KEY, historyId);
        body.put(MESSAGE_ID_KEY, UUID.randomUUID());

        try {
            HttpUtil.withUrl(trackUrl).withHeaders(headers).withBody(body).post();
        } catch (MalformedURLException e) {
            IMPLog.e(Tag, e.getLocalizedMessage());
        }
    }

    public void trackEvent(String eventName) {
        trackEvent(eventName, null);
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        if(trackURL == null || trackURL.isEmpty()) {
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

        postTrackingRequest(this.trackURL, body);
    }
}
