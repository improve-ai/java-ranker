package ai.improve.android;

import java.util.Map;

public interface DecisionTracker {

    String MODEL_KEY = "model";
    String HISTORY_ID_KEY = "history_id";
    String TIMESTAMP_KEY = "timestamp";
    String MESSAGE_ID_KEY = "message_id";
    String TYPE_KEY = "type";
    String RUNNERS_UP_KEY = "runners_up";
    String COUNT_KEY = "count";
    String CONTEXT_KEY = "context";
    String DECISION_BEST_KEY = "variant";
    String SAMPLE_VARIANT_KEY = "sample";
    String EVENT_KEY = "event";
    String VALUE_KEY = "value";
    String PROPERTIES_KEY = "properties";

    String DECISION_TYPE = "decision";

    String API_KEY_HEADER = "x-api-key";
    String CONTENT_TYPE_HEADER = "Content-Type";
    String APPLICATION_JSON = "application/json";

    Object trackUsingBestFrom(Decision decision);

    /**
     * Tracks a general analytics event that may be further processed by backend scripts.  You may use this for example
     * to keep reward assignment logic on the backend.  In the case where all reward logic is handled on the backend
     * you may wish to disable autoTrack on choose calls and not call trackRewards.
     *
     * @param event      the name of the event to track
     */
    void trackEvent(String event);

    /**
     * Tracks a general analytics event that may be further processed by backend scripts.  You may use this for example
     * to keep reward assignment logic on the backend.  In the case where all reward logic is handled on the backend
     * you may wish to disable autoTrack on choose calls and not call trackRewards.
     *
     * @param event      the name of the event to track
     * @param properties JSON encodable event properties
     */
    void trackEvent(String event, Map<String, Object> properties);

    /**
     * Tracks a general analytics event that may be further processed by backend scripts.  You may use this for example
     * to keep reward assignment logic on the backend.  In the case where all reward logic is handled on the backend
     * you may wish to disable autoTrack on choose calls and not call trackRewards.
     *
     * @param event      the name of the event to track
     * @param properties JSON encodable event properties
     * @param context    JSON encodeable context
     */
    void trackEvent(String event, Map<String, Object> properties, Map<String, Object> context);

}
