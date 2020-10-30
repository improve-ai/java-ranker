package ai.improve.android.spi;

import ai.improve.android.Decision;
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

    private static final float DEFAULT_EVENT_VALUE = 0.01f;

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

        if (preferences.contains(HISTORY_ID_KEY)) {
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
    public Object trackUsingBestFrom(Decision decision) {
        if (trackUrl == null) {
            return null;
        }
        List topRunnersUp;
        if (decision.isTrackRunnersUp()) {
            topRunnersUp = decision.topRunnersUp();
        }
        Object best = decision.best();

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, decision.getModelName());
        body.put(DECISION_BEST_KEY, best);
        if(decision.topRunnersUp() != null) {
            body.put(RUNNERS_UP_KEY, decision.topRunnersUp().size());
        }
        body.put(COUNT_KEY, decision.getVariants().size());

        if (context != null) {
            body.put(CONTEXT_KEY, context);
        }

        // Producing random sample from variants other than best and runners up. Exclude field if none

        List variants = new ArrayList();
        variants.addAll(decision.getVariants());
        variants.removeAll(decision.topRunnersUp());
        variants.remove(decision.best());
        if (variants != null && variants.size() > 0) {
            if (random.nextDouble() > 1.0 / (double) variants.size()) {
                Object randomSample = variants.get(random.nextInt(variants.size()));
                body.put(SAMPLE_VARIANT_KEY, randomSample);
            }
        }

        postTrackingRequest(trackUrl, body);
        return decision.best();
    }


    @Override
    public void trackEvent(String event) {
        trackEvent(event, null, null);
    }

    @Override
    public void trackEvent(String event, Map<String, Object> properties) {
        trackEvent(event, properties, null);
    }

    @Override
    public void trackEvent(String event, Map<String, Object> properties, Map<String, Object> context) {
        Map<String, Object> body = new HashMap<>();
        if (event != null) {
            body.put(EVENT_KEY, event);
        }
        if (properties != null) {
            body.put(PROPERTIES_KEY, properties);
        } else {
            body.put(PROPERTIES_KEY, Collections.singletonMap(VALUE_KEY, DEFAULT_EVENT_VALUE));
        }
        if (context != null) {
            body.put(CONTEXT_KEY, context);
        }
        track(body);
    }

    private void track(Map<String, Object> body) {
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
