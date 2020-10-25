package ai.improve.android.spi;

import ai.improve.android.HttpUtil;
import ai.improve.android.DecisionTracker;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.net.MalformedURLException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class DefaultDecisionTracker implements DecisionTracker {

    private static final Logger logger = Logger.getLogger(DefaultDecisionTracker.class.getName());
    private static final Random random = new SecureRandom();
    private static final SimpleDateFormat ISO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);

    /**
     * Android Context (usually Application instance)
     * Used to store historyId across multiple instances
     */
    private Context context;

    /**
     * Tracking URL
     */
    private String trackUrl;

    /**
     * API Key to post to tracker
     */
    private String apiKey;

    /**
     * Unique identifier
     */
    private String historyId;


    public DefaultDecisionTracker(Context context) {
        this(context, null, null);
    }

    public DefaultDecisionTracker(Context context, String trackUrl) {
        this(context, trackUrl, null);
    }

    public DefaultDecisionTracker(Context context, String trackUrl, String apiKey) {
        this.context = context;
        this.trackUrl = trackUrl;
        this.apiKey = apiKey;

        SharedPreferences preferences = context.getSharedPreferences("ai.improve", Context.MODE_PRIVATE);

        if(preferences.contains(HISTORY_ID_KEY)) {
            this.historyId = preferences.getString(HISTORY_ID_KEY, "");
        } else {
            this.historyId = generateHistoryId();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(HISTORY_ID_KEY, this.historyId);
            editor.commit();
        }

    }


    private String generateHistoryId() {
        byte[] data = UUID.randomUUID().toString().getBytes();
        return Base64.encodeToString(data, Base64.DEFAULT);
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
    public void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey, CompetionHandler completionHandler) {
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

            if (random.nextDouble() > 1.0 / (double) variants.size()) {
                Object randomSample = variants.get(random.nextInt(variants.size()));
                body.put(VARIANTS_COUNT_KEY, variants.size());
                body.put(SAMPLE_VARIANT_KEY, randomSample);
            } else {
                body.put(VARIANTS_KEY, variants);
            }
        }

        postTrackingRequest(trackUrl, body);
    }


    @Override
    public void addReward(String rewardKey, Double reward) {
        addRewards(Collections.singletonMap(rewardKey, reward));
    }

    @Override
    public void addRewards(Map<String, Double> rewards) {
        addRewards(rewards, null);
    }

    @Override
    public void addRewards(Map<String, Double> rewards, CompetionHandler completionHandler) {
        if (rewards != null && !rewards.isEmpty()) {
            logger.info("Tracking rewards: " + rewards);
            track(Collections.singletonMap(REWARDS_TYPE, rewards), completionHandler);
        } else {
            logger.severe("Skipping trackRewards for null or empty rewards");
            if (completionHandler != null) completionHandler.onError("Null rewards");
        }
    }

    @Override
    public void trackAnalyticsEvent(String event, Map<String, Object> properties) {
        trackAnalyticsEvent(event, properties, null);
    }

    @Override
    public void trackAnalyticsEvent(String event, Map<String, Object> properties, Map<String, Object> context) {
        Map<String, Object> body = new HashMap<>();
        if (event != null) {
            body.put(EVENT_KEY, event);
        }
        if (properties != null) {
            body.put(PROPERTIES_KEY, properties);
        }
        if (context != null) {
            body.put(CONTEXT_KEY, context);
        }
        track(body);
    }

    private void track(Map<String, Object> body) {
        track(body, null);
    }

    private void track(Map<String, Object> body, CompetionHandler handler) {
        if (this.trackUrl == null) {
            return;
        }
        postTrackingRequest(this.trackUrl, body);
    }


    private void postTrackingRequest(String trackUrl, Map<String, Object> body) {
        if (this.historyId == null) {
            logger.severe("historyId cannot be null");
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
            logger.severe(e.getLocalizedMessage());
        }
    }

}
