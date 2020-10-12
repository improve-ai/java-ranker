package ai.improve.android.spi;

import ai.improve.android.HttpUtil;
import ai.improve.android.ImproveTrackCompletion;
import ai.improve.android.ImproveTracker;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class DefaultImproveTracker implements ImproveTracker {

    private static final Logger logger = Logger.getLogger(DefaultImproveTracker.class.getName());
    private static final Random random = new SecureRandom();


    private String trackUrl;

    private String apiKey;

    private String historyId;

    public DefaultImproveTracker(String trackUrl) {
        this.trackUrl = trackUrl;
    }

    public DefaultImproveTracker(String trackUrl, String apiKey) {
        this.trackUrl = trackUrl;
        this.apiKey = apiKey;

        //TODO: Cache historyId in Android Context instance
        this.historyId = generateHistoryId();
    }


    private String generateHistoryId() {
        int historyIdSize = 32; // 256 bits
        byte[] data = new byte[historyIdSize];
        random.nextBytes(data);
        return Base64.getEncoder().encodeToString(data);
    }


    @Override
    public void trackDecision(Object variant, List variants, String modelName) {
        trackDecision(variant, variants, modelName, null, null, null);
    }

    @Override
    public void trackDecision(Object variant, List variants, String modelName, Map context) {
        trackDecision(variant, variants, modelName, context, null, null);

    }

    @Override
    public void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey) {
        trackDecision(variant, variants, modelName, context, rewardKey, null);

    }

    @Override
    public void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey, ImproveTrackCompletion completionHandler) {
        //do nothing yet
        if (trackUrl == null) {
            return; // no tracking url set - nothing to track.
        }
        if (variant == null) {
            logger.severe("Skipping trackDecision for nil variant. To track null values use [NSNull null]");
            if (completionHandler != null) {
                completionHandler.onError(null);
            }
            return;
        }

        // the rewardKey is never nil
        if (rewardKey == null) {
            logger.info("Using model name as rewardKey: " + modelName);
            if (completionHandler != null) {
                completionHandler.onError(null);
            }
            rewardKey = modelName;
        }

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(VARIANT_KEY, variant);
        body.put(MODEL_KEY, modelName);
        body.put(REWARD_KEY_KEY, rewardKey);

        if (context != null) {
            body.put(CONTEXT_KEY, context);
        }

        if (variants != null && variants.size() > 0) {
            body.put(VARIANTS_COUNT_KEY, variants.size());

            if (random.nextDouble() > 1.0 / (double) variants.size()) {
                Object randomSample = variants.get(random.nextInt(variants.size()));
                body.put(SAMPLE_VARIANT_KEY, randomSample);
            } else {
                body.put(VARIANTS_KEY, variants);
            }
        }

        postTrackingRequest(trackUrl, body);
    }


    @Override
    public void addReward(Double reward, String rewardKey) {

    }

    @Override
    public void addRewards(Map<String, Double> rewards) {

    }

    @Override
    public void addRewards(Map<String, Double> rewards, ImproveTrackCompletion completionHandler) {

    }

    @Override
    public void trackAnalyticsEvent(String event, Map<String, Object> properties) {

    }

    @Override
    public void trackAnalyticsEvent(String event, Map<String, Object> properties, Map<String, Object> context) {

    }

    private void postTrackingRequest(String trackUrl, Map<String, Object> body) {
        String historyId = "";
        if(this.historyId == null) {
            logger.severe("historyId cannot be null");
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        if(this.apiKey != null) {
            headers.put(API_KEY_HEADER, this.apiKey);
        }

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        body = new HashMap<>(body);
        body.put(TIMESTAMP_KEY, timestamp);
        body.put(HISTORY_ID_KEY, historyId);
        body.put(MESSAGE_ID_KEY, UUID.randomUUID());

        String requestBody = new JSONObject(body).toString();

        try {
            HttpUtil.withUrl(trackUrl).withHeaders(headers).withBody(body).post();
        } catch (MalformedURLException e) {
            logger.severe(e.getLocalizedMessage());
        }
    }

}
